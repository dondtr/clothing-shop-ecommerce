package com.clothingshop.service;

import com.clothingshop.util.ViewFunctions;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Text of the emails E1 – E9 (specification section 8). Every email goes to the customer's User.email, except E9 which
 * goes to any account. Plain text, in Vietnamese.
 */
final class Emails {

    /** Subject and body of one email. */
    record Message(String subject, String body) {
    }

    private static final String SIGNATURE = "\n\nShoppers – Thời trang mỗi ngày";

    private Emails() {
    }

    /** E1: the order is on its way, with the "Đã nhận hàng" button (FR-18). */
    static Message shipped(String fullName, int orderId, String receivedLink) {
        return new Message("Đơn hàng #" + orderId + " đang được giao",
                "Xin chào " + fullName + ",\n\n"
                        + "Đơn hàng #" + orderId + " đã được bàn giao cho đơn vị vận chuyển và đang trên đường đến bạn.\n\n"
                        + "Khi nhận được hàng, vui lòng bấm \"Đã nhận hàng\" để xác nhận:\n" + receivedLink + "\n\n"
                        + "Sau khi xác nhận, bạn có thể đánh giá sản phẩm và gửi yêu cầu trả hàng trong 15 ngày nếu cần."
                        + SIGNATURE);
    }

    /** E2: three days after shipping, asks whether the order arrived (FR-20). */
    static Message reminder(String fullName, int orderId, String receivedLink, String notReceivedLink) {
        return new Message("Bạn đã nhận được đơn hàng #" + orderId + " chưa?",
                "Xin chào " + fullName + ",\n\n"
                        + "Đơn hàng #" + orderId + " đã được giao cho đơn vị vận chuyển 3 ngày trước. Bạn đã nhận được hàng chưa?\n\n"
                        + "Đã nhận được hàng:\n" + receivedLink + "\n\n"
                        + "Chưa thấy hàng đâu:\n" + notReceivedLink + "\n\n"
                        + "Nếu bạn không bấm nút nào, đơn hàng sẽ tự hoàn tất sau 7 ngày kể từ ngày giao cho đơn vị vận chuyển."
                        + SIGNATURE);
    }

    /** E3 (reship) or E4 (resolved): how the shop handled "Chưa thấy hàng đâu"; the texts are typed by the employee (Q23). */
    static Message deliveryIssueHandled(String fullName, int orderId, boolean reship, String reason, String solution) {
        String what = reship
                ? "Cửa hàng đang chuẩn bị và giao lại hàng cho bạn, không thu thêm phí vận chuyển."
                : "Cửa hàng đã liên hệ đơn vị vận chuyển và xác định nguyên nhân giao hàng chậm.";
        return new Message("Cửa hàng đã xử lý thông báo về đơn hàng #" + orderId,
                "Xin chào " + fullName + ",\n\n"
                        + "Cảm ơn bạn đã báo chưa nhận được đơn hàng #" + orderId + ". " + what + "\n\n"
                        + "Lý do: " + reason + "\n"
                        + "Cách giải quyết: " + solution + "\n\n"
                        + "Khi nhận được hàng, bạn có thể xác nhận ở trang chi tiết đơn hàng."
                        + SIGNATURE);
    }

    /** E5: a product in the wishlist starts a discount (FR-11). */
    static Message wishlistDiscount(String fullName, String productName, BigDecimal percent, LocalDateTime start,
                                    LocalDateTime end, String productLink) {
        return new Message("\"" + productName + "\" trong danh sách yêu thích đang giảm giá",
                "Xin chào " + fullName + ",\n\n"
                        + "Mặt hàng \"" + productName + "\" trong danh sách yêu thích của bạn đang giảm "
                        + ViewFunctions.number(percent) + "%, từ " + ViewFunctions.dateTime(start) + " đến "
                        + ViewFunctions.dateTime(end) + ".\n\n"
                        + "Xem sản phẩm: " + productLink
                        + SIGNATURE);
    }

    /** E6: the order was cancelled (Q38). {@code refund} is empty when nothing was paid. */
    static Message cancelled(String fullName, int orderId, String reason, String refund) {
        return new Message("Đơn hàng #" + orderId + " đã bị hủy",
                "Xin chào " + fullName + ",\n\n"
                        + "Đơn hàng #" + orderId + " đã bị hủy.\n"
                        + "Lý do: " + reason + "\n"
                        + (refund.isEmpty() ? "" : refund + "\n")
                        + "\nCảm ơn bạn đã quan tâm đến Shoppers."
                        + SIGNATURE);
    }

    /** E7: result of a return request (Q38). */
    static Message returnResult(String fullName, int orderId, boolean approved, String refund) {
        String result = approved
                ? "Yêu cầu trả hàng của đơn #" + orderId + " đã được duyệt. " + refund
                : "Rất tiếc, yêu cầu trả hàng của đơn #" + orderId + " không được chấp nhận. Đơn hàng giữ trạng thái giao thành công.";
        return new Message("Kết quả yêu cầu trả hàng đơn #" + orderId,
                "Xin chào " + fullName + ",\n\n" + result + SIGNATURE);
    }

    /** E8: email verification at registration (FR-01, link valid 24 hours). */
    static Message verification(String fullName, String link) {
        return new Message("Xác thực email đăng ký tài khoản Shoppers",
                "Xin chào " + fullName + ",\n\n"
                        + "Cảm ơn bạn đã đăng ký tài khoản tại Shoppers. Bấm vào link dưới đây để xác thực email và hoàn tất đăng ký:\n\n"
                        + link + "\n\n"
                        + "Link có hiệu lực trong 24 giờ và chỉ dùng được một lần. Nếu bạn không đăng ký, hãy bỏ qua email này."
                        + SIGNATURE);
    }

    /** E9: password reset (FR-03, link valid 15 minutes). */
    static Message passwordReset(String fullName, String link) {
        return new Message("Đặt lại mật khẩu tài khoản Shoppers",
                "Xin chào " + fullName + ",\n\n"
                        + "Bạn (hoặc ai đó) vừa yêu cầu đặt lại mật khẩu. Bấm vào link dưới đây để đặt mật khẩu mới:\n\n"
                        + link + "\n\n"
                        + "Link có hiệu lực trong 15 phút và hết hiệu lực khi mật khẩu đã được đổi. "
                        + "Nếu bạn không yêu cầu, hãy bỏ qua email này; mật khẩu hiện tại vẫn giữ nguyên."
                        + SIGNATURE);
    }

    /** Refund line of E6 / E7. */
    static String refundNote(boolean vnpay, BigDecimal amount) {
        return vnpay
                ? "Số tiền " + ViewFunctions.vnd(amount) + " đã được hoàn qua VNPAY; thời gian tiền về tài khoản tùy ngân hàng."
                : "Cửa hàng sẽ liên hệ để hoàn " + ViewFunctions.vnd(amount) + " cho bạn.";
    }
}
