package com.clothingshop.service;

import com.clothingshop.entity.Customer;
import com.clothingshop.entity.Employee;
import com.clothingshop.entity.EmployeeRole;
import com.clothingshop.entity.Gender;
import com.clothingshop.entity.Order;
import com.clothingshop.entity.OrderStatus;
import com.clothingshop.entity.Payment;
import com.clothingshop.entity.PaymentMethod;
import com.clothingshop.entity.PaymentStatus;
import com.clothingshop.entity.Product;
import com.clothingshop.entity.ProductCategory;
import com.clothingshop.util.JpaUtil;
import com.clothingshop.util.PasswordHasher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Creates rows for tests in the shared in-memory database; every name is unique so tests do not see each other's data. */
final class TestData {

    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 10, 8, 0);

    private TestData() {
    }

    static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    /** A customer whose password is {@code password} (hashed). */
    static Customer customer(String password) {
        String name = unique("kh");
        return JpaUtil.inTransaction(em -> {
            Customer c = new Customer(name, PasswordHasher.hash(password), "Khách " + name, Gender.FEMALE,
                    LocalDate.of(2000, 1, 1), name + "@example.com", "0900000000", NOW);
            em.persist(c);
            return c;
        });
    }

    static Employee employee(EmployeeRole role, String password) {
        String name = unique("nv");
        return JpaUtil.inTransaction(em -> {
            Employee e = new Employee(name, PasswordHasher.hash(password), "Nhân viên " + name, Gender.MALE,
                    LocalDate.of(1995, 1, 1), name + "@example.com", "0911111111", NOW.toLocalDate(), role);
            em.persist(e);
            return e;
        });
    }

    static Product product(String name, String color, String size, int stock, String basePrice) {
        return JpaUtil.inTransaction(em -> {
            Product p = new Product(name, ProductCategory.SHIRT, null, "assets/images/cloth_1.jpg", new BigDecimal(basePrice),
                    color, size, stock, true, NOW);
            em.persist(p);
            return p;
        });
    }

    /** An order of {@code total} VND (shipping fee included) with one payment, without lines (enough for totals and levels). */
    static int order(int customerId, String total, OrderStatus status, PaymentMethod method, PaymentStatus paymentStatus) {
        return JpaUtil.inTransaction(em -> {
            Order o = new Order(em.getReference(Customer.class, customerId), null, NOW, "1 Võ Văn Ngân, Thủ Đức, TP. HCM", null,
                    new BigDecimal("20000"), new BigDecimal(total));
            o.setStatus(status);
            em.persist(o);
            Payment p = new Payment(o, method);
            p.setStatus(paymentStatus);
            if (paymentStatus == PaymentStatus.PAID || paymentStatus == PaymentStatus.REFUND) {
                p.setPaidAt(NOW);
            }
            em.persist(p);
            return o.getId();
        });
    }

    static void setPaymentStatus(int orderId, PaymentStatus status) {
        JpaUtil.inTransaction(em -> {
            em.createQuery("select p from Payment p where p.order.id = :o", Payment.class).setParameter("o", orderId)
                    .getSingleResult().setStatus(status);
            return null;
        });
    }
}
