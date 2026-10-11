package com.clothingshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/** VNPAY signing (HMAC-SHA512 over the sorted, URL-encoded vnp_* fields). */
class VnpayClientTest {

    private final VnpayClient client = new VnpayClient("TESTTMN1", "khoa-bi-mat-thu-nghiem", "https://pay.example/vpcpay.html",
            "https://api.example/transaction", "127.0.0.1", Clock.system(ZoneId.of("Asia/Ho_Chi_Minh")));

    private static Map<String, String> parse(String url) {
        Map<String, String> params = new LinkedHashMap<>();
        for (String pair : url.substring(url.indexOf('?') + 1).split("&")) {
            int eq = pair.indexOf('=');
            params.put(pair.substring(0, eq), URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.US_ASCII));
        }
        return params;
    }

    @Test
    void thePaymentUrlCarriesTheOrderAmountTimesHundredAndAValidSignature() {
        LocalDateTime created = LocalDateTime.of(2026, 10, 10, 8, 0, 5);
        String url = client.paymentUrl(42, new BigDecimal("358000.00"), created, created.plusMinutes(15), "10.0.0.7",
                "http://localhost:8080/clothing-shop/payment/vnpay-return");
        assertTrue(url.startsWith("https://pay.example/vpcpay.html?vnp_Amount=35800000&"), url);
        Map<String, String> params = parse(url);
        assertEquals("42", params.get("vnp_TxnRef"));
        assertEquals("20261010080005", params.get("vnp_CreateDate"));
        assertEquals("20261010081505", params.get("vnp_ExpireDate"));
        assertEquals("Thanh toan don hang 42", params.get("vnp_OrderInfo"));
        assertEquals(128, params.get("vnp_SecureHash").length(), "HMAC-SHA512 in hex");
        assertTrue(client.isSignatureValid(params));
    }

    @Test
    void changedOrUnsignedParametersAreRejected() {
        LocalDateTime created = LocalDateTime.of(2026, 10, 10, 8, 0);
        Map<String, String> params = parse(client.paymentUrl(7, new BigDecimal("100000"), created, created.plusMinutes(15),
                "127.0.0.1", "http://localhost/return"));
        Map<String, String> tampered = new TreeMap<>(params);
        tampered.put("vnp_Amount", "100");
        assertFalse(client.isSignatureValid(tampered));
        Map<String, String> unsigned = new TreeMap<>(params);
        unsigned.remove("vnp_SecureHash");
        assertFalse(client.isSignatureValid(unsigned));
        Map<String, String> upper = new TreeMap<>(params);
        upper.put("vnp_SecureHash", params.get("vnp_SecureHash").toUpperCase());
        assertTrue(client.isSignatureValid(upper), "the hex signature is compared without regard to case");
        Map<String, String> otherKey = new TreeMap<>(params);
        assertFalse(new VnpayClient("TESTTMN1", "khoa-khac", "", "", "", Clock.systemUTC()).isSignatureValid(otherKey));
    }

    @Test
    void canonicalTextEncodesValuesLikeVnpayAndSkipsEmptyOnes() {
        Map<String, String> fields = new TreeMap<>();
        fields.put("vnp_OrderInfo", "Thanh toan don hang 1");
        fields.put("vnp_ReturnUrl", "http://x/a?b=c");
        fields.put("vnp_BankCode", "");
        assertEquals("vnp_OrderInfo=Thanh+toan+don+hang+1&vnp_ReturnUrl=http%3A%2F%2Fx%2Fa%3Fb%3Dc", VnpayClient.canonical(fields));
        assertEquals("35800000", VnpayClient.vnpAmount(new BigDecimal("358000.00")));
        assertEquals(0, new BigDecimal("358000").compareTo(VnpayClient.parseVnpAmount("35800000")));
    }
}
