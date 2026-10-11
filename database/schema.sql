-- Tạo CSDL cho Clothing Shop E-Commerce theo ERD Clothing E-Commerce (sửa lần 4).
-- File được sinh từ ERD; khi ERD thay đổi thì sinh lại, không sửa tay.
-- Ứng dụng không tự tạo hay sửa bảng (schema-generation = none), nên phải chạy file này một lần trước khi chạy ứng dụng.
--   Máy local : mysql -u root -p < database/schema.sql
--   Aiven     : mysql --host=<host> --port=<port> --user=avnadmin --password --ssl-mode=REQUIRED < database/schema.sql
-- Collation utf8mb4_0900_as_ci: không phân biệt chữ hoa/thường nhưng phân biệt dấu tiếng Việt (đặc tả mục 3.1).

CREATE DATABASE IF NOT EXISTS `clothing_shop` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci;
USE `clothing_shop`;

CREATE TABLE `User` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `userName` VARCHAR(50) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `fullName` VARCHAR(100) NOT NULL,
  `gender` ENUM('FEMALE', 'MALE', 'OTHER') NOT NULL,
  `dob` DATE NOT NULL,
  `email` VARCHAR(100) NOT NULL,
  `phone` VARCHAR(15) NOT NULL,
  `version` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_User_userName` (`userName`),
  UNIQUE KEY `uk_User_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

CREATE TABLE `Customer` (
  `userId` INT NOT NULL,
  `createdAt` DATETIME NOT NULL,
  `customerLevel` ENUM('CASUAL', 'REGULAR', 'LOYAL') NOT NULL,
  `autoDowngrade` BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (`userId`),
  CONSTRAINT `fk_Customer_userId` FOREIGN KEY (`userId`) REFERENCES `User` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

CREATE TABLE `Employee` (
  `userId` INT NOT NULL,
  `hireDate` DATE NOT NULL,
  `active` BOOLEAN NOT NULL,
  `role` ENUM('STAFF', 'ADMIN') NOT NULL,
  PRIMARY KEY (`userId`),
  CONSTRAINT `fk_Employee_userId` FOREIGN KEY (`userId`) REFERENCES `User` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

CREATE TABLE `Address` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `customerId` INT NOT NULL,
  `province` VARCHAR(100) NOT NULL,
  `ward` VARCHAR(100) NOT NULL,
  `street` VARCHAR(255) NOT NULL,
  `isDefault` BOOLEAN NOT NULL,
  `version` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_Address_customerId` FOREIGN KEY (`customerId`) REFERENCES `Customer` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

CREATE TABLE `Wishlist` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `customerId` INT NOT NULL,
  `version` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_Wishlist_customerId` (`customerId`),
  CONSTRAINT `fk_Wishlist_customerId` FOREIGN KEY (`customerId`) REFERENCES `Customer` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

CREATE TABLE `Product` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(200) NOT NULL,
  `category` ENUM('ACCESSORY', 'HAT', 'SHIRT', 'PANTS', 'DRESS', 'SHOES') NOT NULL,
  `description` TEXT NULL,
  `imgUrl` VARCHAR(255) NOT NULL,
  `basePrice` DECIMAL(12,2) NOT NULL,
  `color` VARCHAR(50) NOT NULL,
  `size` VARCHAR(20) NOT NULL,
  `stockQuantity` INT NOT NULL,
  `active` BOOLEAN NOT NULL,
  `createdAt` DATETIME NOT NULL,
  `version` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_Product_name_color_size` (`name`, `color`, `size`),
  CONSTRAINT `chk_Product_stockQuantity` CHECK (stockQuantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

CREATE TABLE `WishlistItem` (
  `wishlistId` INT NOT NULL,
  `productId` INT NOT NULL,
  PRIMARY KEY (`wishlistId`, `productId`),
  CONSTRAINT `fk_WishlistItem_wishlistId` FOREIGN KEY (`wishlistId`) REFERENCES `Wishlist` (`id`),
  CONSTRAINT `fk_WishlistItem_productId` FOREIGN KEY (`productId`) REFERENCES `Product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

CREATE TABLE `Voucher` (
  `code` VARCHAR(50) NOT NULL,
  `discountType` ENUM('PERCENT', 'FIXED_AMOUNT') NOT NULL,
  `discountValue` DECIMAL(12,2) NOT NULL,
  `quantity` INT NOT NULL,
  `version` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`code`),
  CONSTRAINT `chk_Voucher_quantity` CHECK (quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

CREATE TABLE `VoucherPolicy` (
  `voucherCode` VARCHAR(50) NOT NULL,
  `eligibleCustomer` ENUM('CASUAL', 'REGULAR', 'LOYAL', 'ALL') NOT NULL,
  `minOrderValue` DECIMAL(12,2) NOT NULL,
  `maxDiscount` DECIMAL(12,2) NULL,
  `startDate` DATETIME NOT NULL,
  `endDate` DATETIME NOT NULL,
  PRIMARY KEY (`voucherCode`),
  CONSTRAINT `fk_VoucherPolicy_voucherCode` FOREIGN KEY (`voucherCode`) REFERENCES `Voucher` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

CREATE TABLE `Order` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `customerId` INT NOT NULL,
  `voucherCode` VARCHAR(50) NULL,
  `orderDate` DATETIME NOT NULL,
  `address` VARCHAR(255) NOT NULL,
  `note` VARCHAR(500) NULL,
  `shippingFee` DECIMAL(12,2) NOT NULL,
  `totalAmount` DECIMAL(12,2) NOT NULL,
  `status` ENUM('PENDING', 'CONFIRMED', 'SHIPPING', 'COMPLETED', 'CANCELLED') NOT NULL,
  `shippedAt` DATETIME NULL,
  `deliveredAt` DATETIME NULL,
  `version` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_Order_customerId` FOREIGN KEY (`customerId`) REFERENCES `Customer` (`userId`),
  CONSTRAINT `fk_Order_voucherCode` FOREIGN KEY (`voucherCode`) REFERENCES `Voucher` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

CREATE TABLE `OrderDetail` (
  `orderId` INT NOT NULL,
  `productId` INT NOT NULL,
  `quantity` INT NOT NULL,
  `subtotal` DECIMAL(12,2) NOT NULL,
  `version` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`orderId`, `productId`),
  CONSTRAINT `chk_OrderDetail_quantity` CHECK (quantity > 0),
  CONSTRAINT `fk_OrderDetail_orderId` FOREIGN KEY (`orderId`) REFERENCES `Order` (`id`),
  CONSTRAINT `fk_OrderDetail_productId` FOREIGN KEY (`productId`) REFERENCES `Product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

CREATE TABLE `Payment` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `orderId` INT NOT NULL,
  `transactionNo` VARCHAR(100) NULL,
  `method` ENUM('COD', 'VNPAY') NOT NULL,
  `status` ENUM('UNPAID', 'PAID', 'FAILED', 'REFUND') NOT NULL,
  `paidAt` DATETIME NULL,
  `version` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_Payment_orderId` (`orderId`),
  CONSTRAINT `fk_Payment_orderId` FOREIGN KEY (`orderId`) REFERENCES `Order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

CREATE TABLE `Review` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `customerId` INT NOT NULL,
  `productId` INT NOT NULL,
  `rating` INT NOT NULL,
  `comment` TEXT NULL,
  `createdAt` DATETIME NOT NULL,
  `version` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_Review_customerId_productId` (`customerId`, `productId`),
  CONSTRAINT `chk_Review_rating` CHECK (rating BETWEEN 1 AND 5),
  CONSTRAINT `fk_Review_customerId` FOREIGN KEY (`customerId`) REFERENCES `Customer` (`userId`),
  CONSTRAINT `fk_Review_productId` FOREIGN KEY (`productId`) REFERENCES `Product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

CREATE TABLE `Discount` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `productId` INT NOT NULL,
  `discountValue` DECIMAL(5,2) NOT NULL,
  `startDate` DATETIME NOT NULL,
  `endDate` DATETIME NOT NULL,
  `version` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_Discount_productId` FOREIGN KEY (`productId`) REFERENCES `Product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

CREATE TABLE `DeliveryIssue` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `orderId` INT NOT NULL,
  `reportedAt` DATETIME NOT NULL,
  `status` ENUM('OPEN', 'RESOLVED') NOT NULL DEFAULT 'OPEN',
  `resolution` ENUM('RESHIP', 'RESOLVED') NULL,
  `resolvedBy` INT NULL,
  `resolvedAt` DATETIME NULL,
  `version` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_DeliveryIssue_orderId` (`orderId`),
  CONSTRAINT `fk_DeliveryIssue_orderId` FOREIGN KEY (`orderId`) REFERENCES `Order` (`id`),
  CONSTRAINT `fk_DeliveryIssue_resolvedBy` FOREIGN KEY (`resolvedBy`) REFERENCES `Employee` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

CREATE TABLE `ReturnRequest` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `orderId` INT NOT NULL,
  `requestedAt` DATETIME NOT NULL,
  `reason` VARCHAR(500) NOT NULL,
  `status` ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
  `handledBy` INT NULL,
  `handledAt` DATETIME NULL,
  `version` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ReturnRequest_orderId` (`orderId`),
  CONSTRAINT `fk_ReturnRequest_orderId` FOREIGN KEY (`orderId`) REFERENCES `Order` (`id`),
  CONSTRAINT `fk_ReturnRequest_handledBy` FOREIGN KEY (`handledBy`) REFERENCES `Employee` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;
