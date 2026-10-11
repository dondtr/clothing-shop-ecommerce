package com.clothingshop.service;

import com.clothingshop.dto.AddressForm;
import com.clothingshop.dto.AddressView;
import com.clothingshop.entity.Address;
import com.clothingshop.entity.Customer;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ConflictException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Delivery addresses of a customer (FR-06). Once a customer has addresses, exactly one of them is the default; the
 * default can only be deleted together with choosing or creating a new default (Q62, SD13). The recipient of an order is
 * always the account holder (Q06), so an address has no name or phone of its own.
 */
public class AddressService {

    /** Order.address is VARCHAR(255) and stores "street, ward, province" (Q06, Q66). */
    static final int ORDER_ADDRESS_MAX = 255;
    static final String MSG_NOT_FOUND = "Địa chỉ không tồn tại";
    public static final String MSG_DEFAULT_NEEDS_REPLACEMENT =
            "Đây là địa chỉ mặc định. Hãy chọn một địa chỉ khác hoặc tạo địa chỉ mới làm mặc định rồi mới xóa.";

    public List<AddressView> list(int customerId) {
        return JpaUtil.read(em -> addresses(em, customerId).stream().map(AddressService::view).toList());
    }

    /** FR-06 step 1: the first address becomes the default. */
    public AddressView add(int customerId, AddressForm form) {
        AddressForm clean = validate(form);
        return JpaUtil.inTransaction(em -> view(persistNew(em, customerId, clean, false)));
    }

    /** FR-06 step 2 (Address.updateAddress()); refused when the address changed since the form was opened. */
    public void update(int customerId, int addressId, AddressForm form, Long version) {
        AddressForm clean = validate(form);
        JpaUtil.inTransaction(em -> {
            Address address = find(em, customerId, addressId);
            ConflictException.check(address.getVersion(), version);
            address.updateAddress(clean.province(), clean.ward(), clean.street());
            return null;
        });
    }

    /** FR-06 step 3: this address becomes the default, every other one is no longer default. */
    public void setDefault(int customerId, int addressId) {
        JpaUtil.inTransaction(em -> {
            lockCustomer(em, customerId);
            find(em, customerId, addressId);
            for (Address a : addresses(em, customerId)) {
                a.setDefaultAddress(a.getId() == addressId);
            }
            return null;
        });
    }

    /** FR-06 step 4: deletes an address that is not the default; the default needs {@link #removeDefault}. */
    public void remove(int customerId, int addressId) {
        JpaUtil.inTransaction(em -> {
            Address address = find(em, customerId, addressId);
            if (address.isDefaultAddress()) {
                throw new BusinessException(MSG_DEFAULT_NEEDS_REPLACEMENT);
            }
            em.remove(address);
            return null;
        });
    }

    /** FR-06 step 5, first choice: another saved address becomes the default and the old default is deleted, together. */
    public void removeDefault(int customerId, int addressId, int newDefaultId) {
        if (addressId == newDefaultId) {
            throw new BusinessException("Vui lòng chọn một địa chỉ khác làm mặc định");
        }
        JpaUtil.inTransaction(em -> {
            lockCustomer(em, customerId);
            Address old = find(em, customerId, addressId);
            Address replacement = find(em, customerId, newDefaultId);
            for (Address a : addresses(em, customerId)) {
                a.setDefaultAddress(a == replacement);
            }
            em.remove(old);
            return null;
        });
    }

    /** FR-06 step 5, second choice: a new address becomes the default and the old default is deleted, together. */
    public void removeDefault(int customerId, int addressId, AddressForm newAddress) {
        AddressForm clean = validate(newAddress);
        JpaUtil.inTransaction(em -> {
            lockCustomer(em, customerId);
            Address old = find(em, customerId, addressId);
            for (Address a : addresses(em, customerId)) {
                a.setDefaultAddress(false);
            }
            em.remove(old);
            em.flush();
            persistNew(em, customerId, clean, true);
            return null;
        });
    }

    /**
     * Checks an address as FR-06 requires: every part is required and fits its column, and the order text
     * "street, ward, province" fits Order.address. Returns the trimmed values.
     */
    public static AddressForm validate(AddressForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        String province = required(form.province(), 100, "province", "tỉnh/thành phố", errors);
        String ward = required(form.ward(), 100, "ward", "phường/xã", errors);
        String street = required(form.street(), 255, "street", "số nhà, tên đường", errors);
        if (errors.isEmpty() && Address.join(street, ward, province).length() > ORDER_ADDRESS_MAX) {
            errors.put("street", "Địa chỉ đầy đủ (số nhà, phường/xã, tỉnh/thành phố) tối đa " + ORDER_ADDRESS_MAX + " ký tự");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return new AddressForm(province, ward, street);
    }

    /**
     * Saves an already validated address in the caller's transaction (also used by checkout, FR-12 step 1). It becomes the
     * default when {@code makeDefault} is set or when it is the customer's first address.
     */
    static Address persistNew(EntityManager em, int customerId, AddressForm clean, boolean makeDefault) {
        lockCustomer(em, customerId);
        boolean first = addresses(em, customerId).isEmpty();
        Address address = new Address(clean.province(), clean.ward(), clean.street(), makeDefault || first);
        address.setCustomer(em.getReference(Customer.class, customerId));
        em.persist(address);
        return address;
    }

    /**
     * Changes to which address is the default run one at a time per customer, so two browsers cannot leave a customer
     * with two default addresses or none.
     */
    private static void lockCustomer(EntityManager em, int customerId) {
        if (em.find(Customer.class, customerId, LockModeType.PESSIMISTIC_WRITE) == null) {
            throw new BusinessException("Tài khoản khách hàng không tồn tại");
        }
    }

    private static List<Address> addresses(EntityManager em, int customerId) {
        return em.createQuery("select a from Address a where a.customer.id = :c order by a.defaultAddress desc, a.id", Address.class)
                .setParameter("c", customerId).getResultList();
    }

    private static Address find(EntityManager em, int customerId, int addressId) {
        return em.createQuery("select a from Address a where a.id = :id and a.customer.id = :c", Address.class)
                .setParameter("id", addressId).setParameter("c", customerId)
                .getResultStream().findFirst().orElseThrow(() -> new BusinessException(MSG_NOT_FOUND));
    }

    private static String required(String raw, int max, String field, String label, Map<String, String> errors) {
        String value = UserRules.trim(raw);
        if (value.isEmpty()) {
            errors.put(field, "Vui lòng nhập " + label);
        } else if (value.length() > max) {
            errors.put(field, "Tối đa " + max + " ký tự");
        }
        return value;
    }

    private static AddressView view(Address a) {
        return new AddressView(a.getId(), a.getProvince(), a.getWard(), a.getStreet(), a.isDefaultAddress(),
                a.toAddressString(), a.getVersion() == null ? 0 : a.getVersion());
    }
}
