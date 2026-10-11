package com.clothingshop.service;

import com.clothingshop.util.AppConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VNPAY API 2.1.0: payment URL, signature of IPN / Return URL parameters, transaction query (querydr) and refund.
 * Every message is signed with HMAC-SHA512 using the shop's secret (section 10).
 *
 * <p>Settings: {@code vnpay.tmnCode} and {@code vnpay.hashSecret} (both required to enable VNPAY), {@code vnpay.payUrl}
 * and {@code vnpay.apiUrl} (default: VNPAY sandbox), {@code vnpay.serverIp} (the shop server's IP sent with API calls).
 */
public class VnpayClient implements VnpayGateway {

    private static final Logger LOG = LoggerFactory.getLogger(VnpayClient.class);

    static final String VERSION = "2.1.0";
    static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    static final String SANDBOX_PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    static final String SANDBOX_API_URL = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String tmnCode;
    private final byte[] secret;
    private final String payUrl;
    private final String apiUrl;
    private final String serverIp;
    private final Clock clock;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json = new ObjectMapper();

    public VnpayClient(String tmnCode, String hashSecret, String payUrl, String apiUrl, String serverIp, Clock clock) {
        this.tmnCode = tmnCode;
        this.secret = hashSecret.getBytes(StandardCharsets.UTF_8);
        this.payUrl = payUrl;
        this.apiUrl = apiUrl;
        this.serverIp = serverIp;
        this.clock = clock;
    }

    /** The client configured by {@code vnpay.*}, or empty when VNPAY is not configured (checkout then offers COD only). */
    public static Optional<VnpayGateway> fromConfig(Clock clock) {
        String tmn = AppConfig.get("vnpay.tmnCode", "").trim();
        String secret = AppConfig.get("vnpay.hashSecret", "").trim();
        if (tmn.isEmpty() || secret.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new VnpayClient(tmn, secret, AppConfig.get("vnpay.payUrl", SANDBOX_PAY_URL),
                AppConfig.get("vnpay.apiUrl", SANDBOX_API_URL), AppConfig.get("vnpay.serverIp", "127.0.0.1"), clock));
    }

    @Override
    public String paymentUrl(int orderId, BigDecimal amount, LocalDateTime createdAt, LocalDateTime expireAt, String clientIp,
                             String returnUrl) {
        Map<String, String> fields = new TreeMap<>();
        fields.put("vnp_Version", VERSION);
        fields.put("vnp_Command", "pay");
        fields.put("vnp_TmnCode", tmnCode);
        fields.put("vnp_Amount", vnpAmount(amount));
        fields.put("vnp_CurrCode", "VND");
        fields.put("vnp_TxnRef", String.valueOf(orderId));
        fields.put("vnp_OrderInfo", "Thanh toan don hang " + orderId);
        fields.put("vnp_OrderType", "other");
        fields.put("vnp_Locale", "vn");
        fields.put("vnp_ReturnUrl", returnUrl);
        fields.put("vnp_IpAddr", clientIp);
        fields.put("vnp_CreateDate", TIME.format(createdAt));
        fields.put("vnp_ExpireDate", TIME.format(expireAt));
        String query = canonical(fields);
        return payUrl + "?" + query + "&vnp_SecureHash=" + hmac(query);
    }

    @Override
    public boolean isSignatureValid(Map<String, String> params) {
        String received = params.get("vnp_SecureHash");
        if (received == null || received.isBlank()) {
            return false;
        }
        Map<String, String> fields = new TreeMap<>();
        params.forEach((k, v) -> {
            if (k.startsWith("vnp_") && !k.equals("vnp_SecureHash") && !k.equals("vnp_SecureHashType")) {
                fields.put(k, v);
            }
        });
        return equalsIgnoreCase(hmac(canonical(fields)), received);
    }

    @Override
    public QueryResult query(int orderId, LocalDateTime createdAt) {
        String requestId = requestId();
        String now = TIME.format(LocalDateTime.now(clock));
        String orderInfo = "Kiem tra ket qua giao dich don hang " + orderId;
        Map<String, String> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", VERSION);
        body.put("vnp_Command", "querydr");
        body.put("vnp_TmnCode", tmnCode);
        body.put("vnp_TxnRef", String.valueOf(orderId));
        body.put("vnp_OrderInfo", orderInfo);
        body.put("vnp_TransactionDate", TIME.format(createdAt));
        body.put("vnp_CreateDate", now);
        body.put("vnp_IpAddr", serverIp);
        body.put("vnp_SecureHash", hmac(String.join("|", requestId, VERSION, "querydr", tmnCode, String.valueOf(orderId),
                TIME.format(createdAt), now, serverIp, orderInfo)));
        Map<String, String> r;
        try {
            r = post(body);
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.warn("VNPAY querydr failed for order {}: {}", orderId, e.toString());
            return new QueryResult(QueryStatus.UNKNOWN, null, null, e.toString());
        }
        String code = r.getOrDefault("vnp_ResponseCode", "");
        boolean signed = isResponseSignatureValid(r, false);
        if ("91".equals(code) && (signed || !r.containsKey("vnp_SecureHash"))) {   // no such transaction: never paid
            return new QueryResult(QueryStatus.NOT_PAID, null, null, r.get("vnp_Message"));
        }
        if (!"00".equals(code) || !signed) {
            return new QueryResult(QueryStatus.UNKNOWN, null, null, code + " " + r.get("vnp_Message"));
        }
        String status = r.getOrDefault("vnp_TransactionStatus", "");
        BigDecimal amount = parseVnpAmount(r.get("vnp_Amount"));
        return switch (status) {
            case "00" -> new QueryResult(QueryStatus.PAID, r.get("vnp_TransactionNo"), amount, r.get("vnp_Message"));
            // 01: not completed (the payment time is over when J7 asks), 02: failed
            case "01", "02" -> new QueryResult(QueryStatus.NOT_PAID, null, null, "transaction status " + status);
            default -> new QueryResult(QueryStatus.UNKNOWN, null, null, "transaction status " + status);
        };
    }

    @Override
    public boolean refund(int orderId, BigDecimal amount, String transactionNo, LocalDateTime createdAt, String createdBy) {
        String requestId = requestId();
        String now = TIME.format(LocalDateTime.now(clock));
        String orderInfo = "Hoan tien don hang " + orderId;
        String vnpAmount = vnpAmount(amount);
        String txnNo = transactionNo == null ? "" : transactionNo;
        Map<String, String> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", VERSION);
        body.put("vnp_Command", "refund");
        body.put("vnp_TmnCode", tmnCode);
        body.put("vnp_TransactionType", "02");   // full refund
        body.put("vnp_TxnRef", String.valueOf(orderId));
        body.put("vnp_Amount", vnpAmount);
        body.put("vnp_OrderInfo", orderInfo);
        body.put("vnp_TransactionNo", txnNo);
        body.put("vnp_TransactionDate", TIME.format(createdAt));
        body.put("vnp_CreateBy", createdBy);
        body.put("vnp_CreateDate", now);
        body.put("vnp_IpAddr", serverIp);
        body.put("vnp_SecureHash", hmac(String.join("|", requestId, VERSION, "refund", tmnCode, "02", String.valueOf(orderId),
                vnpAmount, txnNo, TIME.format(createdAt), createdBy, now, serverIp, orderInfo)));
        try {
            Map<String, String> r = post(body);
            boolean ok = "00".equals(r.get("vnp_ResponseCode")) && isResponseSignatureValid(r, true);
            if (!ok) {
                LOG.warn("VNPAY refund refused for order {}: {} {}", orderId, r.get("vnp_ResponseCode"), r.get("vnp_Message"));
            }
            return ok;
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.warn("VNPAY refund failed for order {}: {}", orderId, e.toString());
            return false;
        }
    }

    private Map<String, String> post(Map<String, String> body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode());
        }
        Map<String, Object> raw = json.readValue(response.body(), new TypeReference<>() { });
        Map<String, String> result = new LinkedHashMap<>();
        raw.forEach((k, v) -> result.put(k, v == null ? null : String.valueOf(v)));
        return result;
    }

    /** Checks the signature of a querydr / refund answer; an answer without a signature is not trusted. */
    private boolean isResponseSignatureValid(Map<String, String> r, boolean refund) {
        String received = r.get("vnp_SecureHash");
        if (received == null || received.isBlank()) {
            return false;
        }
        String[] keys = refund
                ? new String[] {"vnp_ResponseId", "vnp_Command", "vnp_ResponseCode", "vnp_Message", "vnp_TmnCode", "vnp_TxnRef",
                        "vnp_Amount", "vnp_BankCode", "vnp_PayDate", "vnp_TransactionNo", "vnp_TransactionType",
                        "vnp_TransactionStatus", "vnp_OrderInfo"}
                : new String[] {"vnp_ResponseId", "vnp_Command", "vnp_ResponseCode", "vnp_Message", "vnp_TmnCode", "vnp_TxnRef",
                        "vnp_Amount", "vnp_BankCode", "vnp_PayDate", "vnp_TransactionNo", "vnp_TransactionType",
                        "vnp_TransactionStatus", "vnp_OrderInfo", "vnp_PromotionCode", "vnp_PromotionAmount"};
        StringJoiner data = new StringJoiner("|");
        for (String k : keys) {
            String v = r.get(k);
            data.add(v == null ? "" : v);
        }
        boolean valid = equalsIgnoreCase(hmac(data.toString()), received);
        if (!valid) {
            LOG.warn("VNPAY answer with an invalid signature for order {}", r.get("vnp_TxnRef"));
        }
        return valid;
    }

    /** Sorted vnp_* fields as key=URL-encoded value joined by '&', the text VNPAY signs (empty values are left out). */
    static String canonical(Map<String, String> sortedFields) {
        StringJoiner joiner = new StringJoiner("&");
        sortedFields.forEach((k, v) -> {
            if (v != null && !v.isEmpty()) {
                joiner.add(k + "=" + URLEncoder.encode(v, StandardCharsets.US_ASCII));
            }
        });
        return joiner.toString();
    }

    String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret, "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA512 is not available", e);
        }
    }

    /** VNPAY amounts are in VND × 100, without decimals. */
    static String vnpAmount(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    static BigDecimal parseVnpAmount(String value) {
        try {
            return value == null ? null : new BigDecimal(value).divide(BigDecimal.valueOf(100));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String requestId() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static boolean equalsIgnoreCase(String expected, String received) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                received.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }
}
