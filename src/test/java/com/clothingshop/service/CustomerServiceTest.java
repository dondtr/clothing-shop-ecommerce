package com.clothingshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clothingshop.TestDatabase;
import com.clothingshop.dto.CustomerDetail;
import com.clothingshop.entity.Customer;
import com.clothingshop.entity.CustomerLevel;
import com.clothingshop.entity.OrderStatus;
import com.clothingshop.entity.PaymentMethod;
import com.clothingshop.entity.PaymentStatus;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ConflictException;
import com.clothingshop.util.JpaUtil;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** FR-27 automatic level, FR-28 manual upgrade and automatic downgrade switch, FR-42 lookup. */
class CustomerServiceTest {

    private final CustomerService service = new CustomerService();
    private int customerId;

    @BeforeAll
    static void database() {
        TestDatabase.start();
    }

    @BeforeEach
    void setUp() {
        customerId = TestData.customer("matkhau-123").getId();
    }

    private Customer reload() {
        return JpaUtil.read(em -> em.find(Customer.class, customerId));
    }

    private void evaluate() {
        JpaUtil.inTransaction(em -> {
            CustomerService.evaluateLevel(em, customerId);
            return null;
        });
    }

    private int paid(String total) {
        return TestData.order(customerId, total, OrderStatus.COMPLETED, PaymentMethod.COD, PaymentStatus.PAID);
    }

    @Test
    void thresholdsAreFromTheAmountUpwards() {
        assertEquals(CustomerLevel.CASUAL, CustomerService.levelFor(new BigDecimal("1999999")));
        assertEquals(CustomerLevel.REGULAR, CustomerService.levelFor(new BigDecimal("2000000")));
        assertEquals(CustomerLevel.REGULAR, CustomerService.levelFor(new BigDecimal("9999999")));
        assertEquals(CustomerLevel.LOYAL, CustomerService.levelFor(new BigDecimal("10000000")));
    }

    @Test
    void onlyPaidOrdersCountAndARefundLowersTheLevelWhenAutoDowngradeIsOn() {
        int order = paid("1500000");
        TestData.order(customerId, "900000", OrderStatus.PENDING, PaymentMethod.COD, PaymentStatus.UNPAID);
        TestData.order(customerId, "5000000", OrderStatus.PENDING, PaymentMethod.VNPAY, PaymentStatus.UNPAID);
        paid("500000");
        evaluate();
        assertEquals(CustomerLevel.REGULAR, reload().getCustomerLevel(), "1.500.000 + 500.000 paid");

        TestData.setPaymentStatus(order, PaymentStatus.REFUND);
        evaluate();
        assertEquals(CustomerLevel.CASUAL, reload().getCustomerLevel());
    }

    @Test
    void aManualUpgradeTurnsAutoDowngradeOffAndASystemUpgradeToLoyalTurnsItBackOn() {
        long version = reload().getVersion();
        service.upgradeLevel(customerId, CustomerLevel.REGULAR, version);
        assertEquals(CustomerLevel.REGULAR, reload().getCustomerLevel());
        assertFalse(reload().isAutoDowngrade());

        evaluate();
        assertEquals(CustomerLevel.REGULAR, reload().getCustomerLevel(), "kept: nothing paid, but autoDowngrade is off");

        paid("10000000");
        evaluate();
        assertEquals(CustomerLevel.LOYAL, reload().getCustomerLevel());
        assertTrue(reload().isAutoDowngrade(), "the system raised the level to LOYAL (Q31)");
    }

    @Test
    void aManualUpgradeToLoyalKeepsAutoDowngradeOff() {
        service.upgradeLevel(customerId, CustomerLevel.LOYAL, reload().getVersion());
        paid("12000000");
        evaluate();
        assertEquals(CustomerLevel.LOYAL, reload().getCustomerLevel());
        assertFalse(reload().isAutoDowngrade());
    }

    @Test
    void theManagerCannotLowerTheLevelOrSaveFromAnOldPage() {
        long version = reload().getVersion();
        service.upgradeLevel(customerId, CustomerLevel.REGULAR, version);
        assertThrows(ConflictException.class, () -> service.upgradeLevel(customerId, CustomerLevel.LOYAL, version));
        long current = reload().getVersion();
        assertThrows(BusinessException.class, () -> service.upgradeLevel(customerId, CustomerLevel.CASUAL, current));
        assertThrows(BusinessException.class, () -> service.upgradeLevel(customerId, CustomerLevel.REGULAR, current));
    }

    @Test
    void turningAutoDowngradeBackOnReEvaluatesAtOnce() {
        service.upgradeLevel(customerId, CustomerLevel.LOYAL, reload().getVersion());
        paid("3000000");
        service.setAutoDowngrade(customerId, true, reload().getVersion());
        assertEquals(CustomerLevel.REGULAR, reload().getCustomerLevel(), "Q53: lowered to the level of the paid total");
        assertTrue(reload().isAutoDowngrade());
    }

    @Test
    void lookupFindsCustomersAndShowsTheirVisibleOrders() {
        Customer c = reload();
        paid("300000");
        TestData.order(customerId, "400000", OrderStatus.PENDING, PaymentMethod.VNPAY, PaymentStatus.UNPAID);
        assertTrue(service.search(c.getEmail().toUpperCase()).stream().anyMatch(r -> r.id() == customerId));
        assertTrue(service.search("%").stream().noneMatch(r -> r.id() == customerId), "% is not a wildcard");

        CustomerDetail detail = service.detail(customerId);
        assertEquals(1, detail.orders().size(), "the order waiting for VNPAY is hidden (BR-26)");
        assertEquals(new BigDecimal("300000.00"), detail.paidTotal());
    }
}
