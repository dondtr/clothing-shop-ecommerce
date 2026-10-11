package com.clothingshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clothingshop.TestDatabase;
import com.clothingshop.dto.AddressForm;
import com.clothingshop.dto.AddressView;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ConflictException;
import com.clothingshop.exception.ValidationException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** FR-06 delivery addresses, including deleting the default address (Q62, SD13). */
class AddressServiceTest {

    private final AddressService service = new AddressService();
    private int customerId;

    @BeforeAll
    static void database() {
        TestDatabase.start();
    }

    @BeforeEach
    void setUp() {
        customerId = TestData.customer("matkhau-123").getId();
    }

    private static AddressForm form(String street) {
        return new AddressForm("TP. Hồ Chí Minh", "Phường Thủ Đức", street);
    }

    private AddressView defaultAddress() {
        List<AddressView> defaults = service.list(customerId).stream().filter(AddressView::defaultAddress).toList();
        assertEquals(1, defaults.size(), "exactly one default address");
        return defaults.get(0);
    }

    @Test
    void theFirstAddressIsTheDefaultAndTheTextIsStreetWardProvince() {
        AddressView first = service.add(customerId, form("1 Võ Văn Ngân"));
        AddressView second = service.add(customerId, form("2 Lê Văn Việt"));
        assertTrue(first.defaultAddress());
        assertFalse(second.defaultAddress());
        assertEquals("1 Võ Văn Ngân, Phường Thủ Đức, TP. Hồ Chí Minh", first.text());

        service.setDefault(customerId, second.id());
        assertEquals(second.id(), defaultAddress().id());
    }

    @Test
    void addressesAreCheckedAgainstTheErdAndTheOrderAddressLength() {
        ValidationException missing = assertThrows(ValidationException.class,
                () -> service.add(customerId, new AddressForm(" ", "", "")));
        assertEquals(java.util.Set.of("province", "ward", "street"), missing.getErrors().keySet());
        ValidationException tooLong = assertThrows(ValidationException.class,
                () -> service.add(customerId, new AddressForm("p".repeat(100), "w".repeat(100), "s".repeat(60))));
        assertTrue(tooLong.getErrors().get("street").contains("255"), "street, ward, province must fit Order.address");
    }

    @Test
    void theDefaultCanOnlyBeDeletedWithANewDefault() {
        AddressView first = service.add(customerId, form("1 Võ Văn Ngân"));
        AddressView second = service.add(customerId, form("2 Lê Văn Việt"));
        assertEquals(AddressService.MSG_DEFAULT_NEEDS_REPLACEMENT,
                assertThrows(BusinessException.class, () -> service.remove(customerId, first.id())).getMessage());

        service.removeDefault(customerId, first.id(), second.id());
        assertEquals(1, service.list(customerId).size());
        assertEquals(second.id(), defaultAddress().id());

        service.removeDefault(customerId, second.id(), form("3 Đặng Văn Bi"));
        assertEquals("3 Đặng Văn Bi", defaultAddress().street());
        assertEquals(1, service.list(customerId).size());
    }

    @Test
    void anInvalidReplacementKeepsTheOldDefault() {
        AddressView only = service.add(customerId, form("1 Võ Văn Ngân"));
        assertThrows(ValidationException.class, () -> service.removeDefault(customerId, only.id(), form("")));
        assertEquals(only.id(), defaultAddress().id());
    }

    @Test
    void otherAddressesAreDeletedDirectly() {
        service.add(customerId, form("1 Võ Văn Ngân"));
        AddressView second = service.add(customerId, form("2 Lê Văn Việt"));
        service.remove(customerId, second.id());
        assertEquals(1, service.list(customerId).size());
    }

    @Test
    void editsBasedOnAnOldVersionAreRefused() {
        AddressView address = service.add(customerId, form("1 Võ Văn Ngân"));
        service.update(customerId, address.id(), form("1A Võ Văn Ngân"), address.version());
        assertThrows(ConflictException.class,
                () -> service.update(customerId, address.id(), form("1B Võ Văn Ngân"), address.version()));
        assertEquals("1A Võ Văn Ngân", service.list(customerId).get(0).street());
    }

    @Test
    void customersCannotTouchOtherCustomersAddresses() {
        AddressView mine = service.add(customerId, form("1 Võ Văn Ngân"));
        int other = TestData.customer("matkhau-123").getId();
        assertThrows(BusinessException.class, () -> service.setDefault(other, mine.id()));
        assertThrows(BusinessException.class, () -> service.update(other, mine.id(), form("x"), mine.version()));
    }
}
