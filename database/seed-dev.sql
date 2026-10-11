-- Dữ liệu mẫu để chạy thử trên máy local: mặt hàng, biến thể, giảm giá, đánh giá.
-- CHỈ DÙNG CHO CSDL LOCAL ĐỂ PHÁT TRIỂN, không chạy trên Aiven hay CSDL thật.
-- Chạy một lần, sau database/schema.sql, trên CSDL vừa tạo (chạy lần hai sẽ báo trùng khóa UNIQUE):
--   mysql -u root -p --default-character-set=utf8mb4 < database/seed-dev.sql
--
-- Nội dung:
--   - 7 mặt hàng đang bán, mỗi biến thể (màu + kích thước) là một dòng Product (Q01, Q44):
--       có một đợt giảm giá đang hiệu lực (15%: 150.000 → 127.500, làm tròn thành 128.000 theo BR-21),
--       một đợt chưa đến ngày bắt đầu, một đợt đã kết thúc, một mặt hàng hết hàng mọi dòng, vài dòng hết hàng.
--   - 1 mặt hàng không bán (active = FALSE), không hiện ở cửa hàng (BR-16).
--   - 6 voucher tạo thủ công: đang dùng được (phần trăm có mức tối đa, số tiền cố định, riêng hạng Trung thành),
--     chưa đến ngày bắt đầu, đã hết hạn, hết lượt.
--   - 2 khách hàng mẫu chỉ để làm người viết đánh giá. Cột password không phải chuỗi băm hợp lệ nên
--     KHÔNG đăng nhập được bằng các tài khoản này; muốn thử giỏ hàng thì tự đăng ký tài khoản.
--     Đánh giá mẫu được thêm thẳng vào bảng, bỏ qua điều kiện đã mua hàng (BR-14) cho tiện xem giao diện.
-- Ảnh lấy từ template Shoppers (Colorlib, CC BY 3.0), nằm ở src/main/webapp/assets/images.

SET NAMES utf8mb4;
USE `clothing_shop`;

INSERT INTO `Product` (`name`, `category`, `description`, `imgUrl`, `basePrice`, `color`, `size`, `stockQuantity`, `active`, `createdAt`) VALUES
('Áo ba lỗ Tank-Top', 'SHIRT', 'Áo ba lỗ cotton 100%, thoáng mát, in hình nổi bật.\nGiặt máy ở nhiệt độ thường.', 'assets/images/cloth_1.jpg', 150000, 'Trắng', 'S', 12, TRUE, NOW() - INTERVAL 10 DAY),
('Áo ba lỗ Tank-Top', 'SHIRT', 'Áo ba lỗ cotton 100%, thoáng mát, in hình nổi bật.\nGiặt máy ở nhiệt độ thường.', 'assets/images/cloth_1.jpg', 150000, 'Trắng', 'M', 20, TRUE, NOW() - INTERVAL 10 DAY),
('Áo ba lỗ Tank-Top', 'SHIRT', 'Áo ba lỗ cotton 100%, thoáng mát, in hình nổi bật.\nGiặt máy ở nhiệt độ thường.', 'assets/images/cloth_1.jpg', 150000, 'Trắng', 'L', 0, TRUE, NOW() - INTERVAL 10 DAY),

('Áo polo họa tiết', 'SHIRT', 'Áo polo vải cá sấu, họa tiết in toàn thân, cổ bẻ.', 'assets/images/cloth_2.jpg', 280000, 'Be', 'M', 8, TRUE, NOW() - INTERVAL 8 DAY),
('Áo polo họa tiết', 'SHIRT', 'Áo polo vải cá sấu, họa tiết in toàn thân, cổ bẻ.', 'assets/images/cloth_2.jpg', 280000, 'Be', 'L', 15, TRUE, NOW() - INTERVAL 8 DAY),
('Áo polo họa tiết', 'SHIRT', 'Áo polo vải cá sấu, họa tiết in toàn thân, cổ bẻ.', 'assets/images/cloth_2.jpg', 280000, 'Be', 'XL', 5, TRUE, NOW() - INTERVAL 8 DAY),

('Áo thun ngôi sao', 'SHIRT', 'Áo thun cổ tròn in hình ngôi sao, vải cotton co giãn 4 chiều.', 'assets/images/cloth_3.jpg', 199000, 'Xanh ngọc', 'S', 10, TRUE, NOW() - INTERVAL 6 DAY),
('Áo thun ngôi sao', 'SHIRT', 'Áo thun cổ tròn in hình ngôi sao, vải cotton co giãn 4 chiều.', 'assets/images/cloth_3.jpg', 199000, 'Xanh ngọc', 'M', 0, TRUE, NOW() - INTERVAL 6 DAY),
('Áo thun ngôi sao', 'SHIRT', 'Áo thun cổ tròn in hình ngôi sao, vải cotton co giãn 4 chiều.', 'assets/images/cloth_3.jpg', 199000, 'Xanh ngọc', 'L', 7, TRUE, NOW() - INTERVAL 6 DAY),
('Áo thun ngôi sao', 'SHIRT', 'Áo thun cổ tròn in hình ngôi sao, vải cotton co giãn 4 chiều.', 'assets/images/cloth_3.jpg', 199000, 'Đen', 'S', 3, TRUE, NOW() - INTERVAL 6 DAY),
('Áo thun ngôi sao', 'SHIRT', 'Áo thun cổ tròn in hình ngôi sao, vải cotton co giãn 4 chiều.', 'assets/images/cloth_3.jpg', 199000, 'Đen', 'M', 4, TRUE, NOW() - INTERVAL 6 DAY),

('Giày vải canvas', 'SHOES', 'Giày vải canvas đế cao su, nhẹ, đi êm chân.', 'assets/images/shoe_1.jpg', 450000, 'Xanh navy', '39', 5, TRUE, NOW() - INTERVAL 4 DAY),
('Giày vải canvas', 'SHOES', 'Giày vải canvas đế cao su, nhẹ, đi êm chân.', 'assets/images/shoe_1.jpg', 450000, 'Xanh navy', '40', 9, TRUE, NOW() - INTERVAL 4 DAY),
('Giày vải canvas', 'SHOES', 'Giày vải canvas đế cao su, nhẹ, đi êm chân.', 'assets/images/shoe_1.jpg', 450000, 'Xanh navy', '41', 6, TRUE, NOW() - INTERVAL 4 DAY),
('Giày vải canvas', 'SHOES', 'Giày vải canvas đế cao su, nhẹ, đi êm chân.', 'assets/images/shoe_1.jpg', 450000, 'Xanh navy', '42', 2, TRUE, NOW() - INTERVAL 4 DAY),

('Áo blazer nữ', 'SHIRT', 'Áo blazer dáng suông, vải tuyết mưa đứng form, phù hợp đi làm.', 'assets/images/women.jpg', 650000, 'Trắng kem', 'S', 4, TRUE, NOW() - INTERVAL 3 DAY),
('Áo blazer nữ', 'SHIRT', 'Áo blazer dáng suông, vải tuyết mưa đứng form, phù hợp đi làm.', 'assets/images/women.jpg', 650000, 'Trắng kem', 'M', 6, TRUE, NOW() - INTERVAL 3 DAY),
('Áo blazer nữ', 'SHIRT', 'Áo blazer dáng suông, vải tuyết mưa đứng form, phù hợp đi làm.', 'assets/images/women.jpg', 650000, 'Trắng kem', 'L', 3, TRUE, NOW() - INTERVAL 3 DAY),

('Áo thun trơn nam', 'SHIRT', 'Áo thun trơn cổ tròn, dễ phối đồ.', 'assets/images/men.jpg', 120000, 'Trắng', 'M', 30, TRUE, NOW() - INTERVAL 2 DAY),
('Áo thun trơn nam', 'SHIRT', 'Áo thun trơn cổ tròn, dễ phối đồ.', 'assets/images/men.jpg', 120000, 'Trắng', 'L', 25, TRUE, NOW() - INTERVAL 2 DAY),
('Áo thun trơn nam', 'SHIRT', 'Áo thun trơn cổ tròn, dễ phối đồ.', 'assets/images/men.jpg', 120000, 'Trắng', 'XL', 10, TRUE, NOW() - INTERVAL 2 DAY),
('Áo thun trơn nam', 'SHIRT', 'Áo thun trơn cổ tròn, dễ phối đồ.', 'assets/images/men.jpg', 120000, 'Đen', 'M', 20, TRUE, NOW() - INTERVAL 2 DAY),
('Áo thun trơn nam', 'SHIRT', 'Áo thun trơn cổ tròn, dễ phối đồ.', 'assets/images/men.jpg', 120000, 'Đen', 'L', 0, TRUE, NOW() - INTERVAL 2 DAY),

('Áo khoác trẻ em', 'SHIRT', 'Áo khoác dạ cho bé, có đai thắt eo.', 'assets/images/children.jpg', 320000, 'Hồng san hô', '110', 0, TRUE, NOW() - INTERVAL 1 DAY),
('Áo khoác trẻ em', 'SHIRT', 'Áo khoác dạ cho bé, có đai thắt eo.', 'assets/images/children.jpg', 320000, 'Hồng san hô', '120', 0, TRUE, NOW() - INTERVAL 1 DAY),

('Mũ lưỡi trai cổ điển', 'HAT', 'Mặt hàng đã ngừng bán (dữ liệu mẫu cho BR-16).', 'assets/images/cloth_2.jpg', 90000, 'Đen', 'Free size', 10, FALSE, NOW() - INTERVAL 30 DAY);

-- Đợt giảm giá: một bản ghi Discount cho mỗi dòng của mặt hàng (FR-34)
INSERT INTO `Discount` (`productId`, `discountValue`, `startDate`, `endDate`)
SELECT `id`, 15, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 30 DAY FROM `Product` WHERE `name` = 'Áo ba lỗ Tank-Top';
INSERT INTO `Discount` (`productId`, `discountValue`, `startDate`, `endDate`)
SELECT `id`, 20, NOW() + INTERVAL 3 DAY, NOW() + INTERVAL 10 DAY FROM `Product` WHERE `name` = 'Giày vải canvas';
INSERT INTO `Discount` (`productId`, `discountValue`, `startDate`, `endDate`)
SELECT `id`, 30, NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 10 DAY FROM `Product` WHERE `name` = 'Áo thun trơn nam';

-- Khách hàng mẫu (không đăng nhập được, xem ghi chú đầu file) và đánh giá
INSERT INTO `User` (`userName`, `password`, `fullName`, `gender`, `dob`, `email`, `phone`)
VALUES ('seed_lan', 'disabled-seed-account', 'Nguyễn Thị Lan', 'FEMALE', '1998-04-12', 'seed.lan@example.com', '0900000001');
SET @lan = LAST_INSERT_ID();
INSERT INTO `Customer` (`userId`, `createdAt`, `customerLevel`, `autoDowngrade`) VALUES (@lan, NOW(), 'CASUAL', TRUE);

INSERT INTO `User` (`userName`, `password`, `fullName`, `gender`, `dob`, `email`, `phone`)
VALUES ('seed_minh', 'disabled-seed-account', 'Trần Văn Minh', 'MALE', '1995-09-30', 'seed.minh@example.com', '0900000002');
SET @minh = LAST_INSERT_ID();
INSERT INTO `Customer` (`userId`, `createdAt`, `customerLevel`, `autoDowngrade`) VALUES (@minh, NOW(), 'CASUAL', TRUE);

INSERT INTO `Review` (`customerId`, `productId`, `rating`, `comment`, `createdAt`)
SELECT @lan, `id`, 5, 'Vải mềm, mặc mát, đúng size.', NOW() - INTERVAL 2 DAY
FROM `Product` WHERE `name` = 'Áo thun ngôi sao' AND `color` = 'Xanh ngọc' AND `size` = 'S';
INSERT INTO `Review` (`customerId`, `productId`, `rating`, `comment`, `createdAt`)
SELECT @minh, `id`, 4, 'Màu đen đẹp, hình in rõ. Giao hơi chậm.', NOW() - INTERVAL 1 DAY
FROM `Product` WHERE `name` = 'Áo thun ngôi sao' AND `color` = 'Đen' AND `size` = 'M';

-- Voucher tạo thủ công (mã không trùng dạng mã ngày lễ, mã hằng tháng: BR-22)
INSERT INTO `Voucher` (`code`, `discountType`, `discountValue`, `quantity`) VALUES
('CHAOBAN', 'PERCENT', 10, 100),
('GIAM30K', 'FIXED_AMOUNT', 30000, 50),
('TRUNGTHANH', 'PERCENT', 20, 30),
('SAPTOI', 'PERCENT', 15, 100),
('HETHAN', 'FIXED_AMOUNT', 50000, 100),
('HETLUOT', 'PERCENT', 50, 0);
INSERT INTO `VoucherPolicy` (`voucherCode`, `eligibleCustomer`, `minOrderValue`, `maxDiscount`, `startDate`, `endDate`) VALUES
('CHAOBAN', 'ALL', 200000, 50000, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 60 DAY),
('GIAM30K', 'ALL', 300000, NULL, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 60 DAY),
('TRUNGTHANH', 'LOYAL', 0, 200000, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 60 DAY),
('SAPTOI', 'ALL', 0, NULL, NOW() + INTERVAL 5 DAY, NOW() + INTERVAL 30 DAY),
('HETHAN', 'ALL', 0, NULL, NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 1 DAY),
('HETLUOT', 'ALL', 0, NULL, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 30 DAY);
