-- ============================================================================
-- Ceyvana POS Backend - Normalized Database Schema (MySQL / MariaDB)
-- Description: Robust, production-ready schema supporting multi-role users,
--              categorized products with advanced packet/weight logic,
--              customer/supplier profiles, transactional ledger tracking,
--              inventory flow, invoices, and key-value system settings.
-- ============================================================================

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `settings`;
DROP TABLE IF EXISTS `invoice_logs`;
DROP TABLE IF EXISTS `invoices`;
DROP TABLE IF EXISTS `payments`;
DROP TABLE IF EXISTS `purchases`;
DROP TABLE IF EXISTS `expenses`;
DROP TABLE IF EXISTS `transaction_items`;
DROP TABLE IF EXISTS `transactions`;
DROP TABLE IF EXISTS `inventory_logs`;
DROP TABLE IF EXISTS `products`;
DROP TABLE IF EXISTS `categories`;
DROP TABLE IF EXISTS `suppliers`;
DROP TABLE IF EXISTS `customers`;
DROP TABLE IF EXISTS `users`;
SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------------------------
-- 1. Users & Roles
-- ----------------------------------------------------------------------------
CREATE TABLE `users` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(150) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `role` ENUM('Owner', 'Manager', 'Cashier') NOT NULL DEFAULT 'Cashier',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1,
    `attendance_active` TINYINT(1) NOT NULL DEFAULT 0,
    `last_login_at` TIMESTAMP NULL DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_users_email` (`email`),
    INDEX `idx_users_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 2. Categories
-- ----------------------------------------------------------------------------
CREATE TABLE `categories` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL UNIQUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 3. Customers
-- ----------------------------------------------------------------------------
CREATE TABLE `customers` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `email` VARCHAR(150) DEFAULT NULL,
    `loyalty_points` INT INT UNSIGNED NOT NULL DEFAULT 0,
    `balance` DECIMAL(15, 4) NOT NULL DEFAULT 0.0000 COMMENT 'Negative represents store credit owed, positive represents advanced payment',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_customers_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 4. Suppliers
-- ----------------------------------------------------------------------------
CREATE TABLE `suppliers` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `email` VARCHAR(150) DEFAULT NULL,
    `balance` DECIMAL(15, 4) NOT NULL DEFAULT 0.0000 COMMENT 'Outstanding credit balance due to supplier',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 5. Products (With Sophisticated Packet & Weight-Based Logic)
-- ----------------------------------------------------------------------------
CREATE TABLE `products` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(150) NOT NULL,
    `sku` VARCHAR(50) NOT NULL UNIQUE,
    `price` DECIMAL(12, 2) NOT NULL COMMENT 'Base selling price in local currency (LKR)',
    `cost_price` DECIMAL(12, 2) NOT NULL COMMENT 'Standard unit cost price in local currency (LKR)',
    `stock` INT NOT NULL DEFAULT 0 COMMENT 'Available stock quantity (either units or raw grams depending on type)',
    `low_stock_threshold` INT NOT NULL DEFAULT 5,
    `category_id` INT UNSIGNED NOT NULL,
    `brand` VARCHAR(100) DEFAULT 'Generic',
    `is_weight_based` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'True if product is sold/stocked by weight or grams',
    `unit` VARCHAR(50) NOT NULL DEFAULT 'Piece (pcs)' COMMENT 'Primary display unit (e.g. pcs, packs, bottles, kg)',
    `unit_type` ENUM('Piece', 'Packet', 'Gram (g)', 'Kilogram (kg)') NOT NULL DEFAULT 'Piece' COMMENT 'The structural stocking format for inventory math',
    `packet_weight` DECIMAL(10, 3) NOT NULL DEFAULT 0.000 COMMENT 'The physical product net weight (e.g. 400.000 for a 400g packet)',
    `packet_weight_unit` ENUM('g', 'kg') NOT NULL DEFAULT 'g' COMMENT 'Weight measurement metric',
    `opening_stock` INT NOT NULL DEFAULT 0,
    `total_weight_in_grams` INT NOT NULL DEFAULT 0 COMMENT 'Helper pre-calculated weight of current stock',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_products_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX `idx_products_sku` (`sku`),
    INDEX `idx_products_is_weight` (`is_weight_based`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 6. Inventory Logs & Packet Conversion Audits
-- ----------------------------------------------------------------------------
CREATE TABLE `inventory_logs` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `product_id` INT UNSIGNED NOT NULL,
    `user_id` INT UNSIGNED DEFAULT NULL,
    `type` ENUM('Opening', 'Purchase', 'Sale', 'Adjustment', 'Bulk_Breakage', 'Returns', 'Sync_Update') NOT NULL,
    `stock_change` INT NOT NULL COMMENT 'Quantity change. Can be positive or negative. Handled in units/grams.',
    `previous_stock` INT NOT NULL,
    `new_stock` INT NOT NULL,
    `total_weight_change_g` INT NOT NULL DEFAULT 0 COMMENT 'Derived weight impact in grams',
    `notes` VARCHAR(255) DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_inv_logs_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_inv_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX `idx_inv_logs_type` (`type`),
    INDEX `idx_inv_logs_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 7. Sales & Transactions
-- ----------------------------------------------------------------------------
CREATE TABLE `transactions` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT UNSIGNED DEFAULT NULL COMMENT 'The cashier who logged the transaction',
    `customer_id` INT UNSIGNED DEFAULT NULL COMMENT 'Optional link to registered profile',
    `customer_name` VARCHAR(100) NOT NULL DEFAULT 'Walk-in Customer',
    `subtotal` DECIMAL(12, 2) NOT NULL,
    `discount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    `tax` DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    `total` DECIMAL(12, 2) NOT NULL COMMENT 'Net transaction total',
    `payment_method` ENUM('Cash', 'Card', 'QR', 'Bank Transfer') NOT NULL,
    `status` ENUM('Completed', 'Held', 'Returned') NOT NULL DEFAULT 'Completed',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_transactions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_transactions_customer` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX `idx_transactions_status` (`status`),
    INDEX `idx_transactions_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 8. Transaction Items
-- ----------------------------------------------------------------------------
CREATE TABLE `transaction_items` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `transaction_id` INT UNSIGNED NOT NULL,
    `product_id` INT UNSIGNED NOT NULL,
    `product_name` VARCHAR(150) NOT NULL COMMENT 'Snapshotted product name to preserve history',
    `quantity` INT NOT NULL COMMENT 'Sold quantity (either unit count or weight in grams)',
    `price` DECIMAL(12, 2) NOT NULL COMMENT 'Unit price (LKR) at point of sale',
    `total` DECIMAL(12, 2) GENERATED ALWAYS AS (`quantity` * `price`) STORED,
    CONSTRAINT `fk_items_transaction` FOREIGN KEY (`transaction_id`) REFERENCES `transactions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_items_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX `idx_items_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 9. Invoices
-- ----------------------------------------------------------------------------
CREATE TABLE `invoices` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `invoice_number` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Formatted like INV-000001',
    `transaction_id` INT UNSIGNED NOT NULL,
    `pdf_path` VARCHAR(255) DEFAULT NULL,
    `is_sent_whatsapp` TINYINT(1) NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_invoices_transaction` FOREIGN KEY (`transaction_id`) REFERENCES `transactions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX `idx_invoices_num` (`invoice_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 10. Purchases (Supplier Supply Entries)
-- ----------------------------------------------------------------------------
CREATE TABLE `purchases` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `product_id` INT UNSIGNED NOT NULL,
    `product_name` VARCHAR(150) NOT NULL,
    `quantity` INT NOT NULL,
    `cost_price` DECIMAL(12, 2) NOT NULL,
    `total` DECIMAL(12, 2) NOT NULL,
    `supplier_id` INT UNSIGNED DEFAULT NULL,
    `supplier_name` VARCHAR(100) NOT NULL DEFAULT 'Generic Supplier',
    `payment_method` ENUM('Cash', 'Bank', 'Credit') NOT NULL DEFAULT 'Cash',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_purchases_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_purchases_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `suppliers` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX `idx_purchases_supplier` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 11. Payments (Ledger Accounts - Suppliers & Customers Credit Receipts)
-- ----------------------------------------------------------------------------
CREATE TABLE `payments` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `reference_id` INT UNSIGNED NOT NULL COMMENT 'Identifier referencing a customer.id or supplier.id',
    `type` ENUM('SupplierPayout', 'CustomerReceipt') NOT NULL,
    `amount` DECIMAL(12, 2) NOT NULL,
    `payment_method` ENUM('Cash', 'Bank') NOT NULL DEFAULT 'Cash',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_payments_ref` (`reference_id`),
    INDEX `idx_payments_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 12. Expenses
-- ----------------------------------------------------------------------------
CREATE TABLE `expenses` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(150) NOT NULL,
    `amount` DECIMAL(12, 2) NOT NULL,
    `category` ENUM('Rent', 'Utilities', 'Salaries', 'Marketing', 'Other') NOT NULL DEFAULT 'Other',
    `payment_method` ENUM('Cash', 'Bank') NOT NULL DEFAULT 'Cash',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_expenses_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 13. Settings (Application-wide Key-Value Configurations)
-- ----------------------------------------------------------------------------
CREATE TABLE `settings` (
    `key_name` VARCHAR(100) NOT NULL PRIMARY KEY,
    `value` TEXT NOT NULL,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- Seed Data Injection
-- ----------------------------------------------------------------------------

-- Seed System Settings (including usd_to_lkr fallback currency)
INSERT INTO `settings` (`key_name`, `value`) VALUES
('usd_to_lkr', '300.50'),
('store_name', 'Ceyvana Modern POS'),
('whatsapp_api_token', 'default_sandbox_token_here'),
('enable_loyalty_program', '1');

-- Seed Users (Password is bcrypt of 'admin123' -> '$2y$10$gR0fU/ZzK6A6T9Ois7e6IuM6hJ2vj6s2k6gZ67D2B03y6.P6oMDeq')
INSERT INTO `users` (`name`, `email`, `password_hash`, `role`) VALUES
('Supreme Admin', 'admin@ceyvana.com', '$2y$10$gR0fU/ZzK6A6T9Ois7e6IuM6hJ2vj6s2k6gZ67D2B03y6.P6oMDeq', 'Owner'),
('Nisal Perera', 'manager@ceyvana.com', '$2y$10$gR0fU/ZzK6A6T9Ois7e6IuM6hJ2vj6s2k6gZ67D2B03y6.P6oMDeq', 'Manager');

-- Seed Categories
INSERT INTO `categories` (`name`) VALUES
('General'),
('Groceries'),
('Spices & Herbs'),
('Beverages'),
('Grains & Rice');

-- Seed Suppliers
INSERT INTO `suppliers` (`name`, `phone`, `email`, `balance`) VALUES
('Lanka Distributors Co.', '+94711234567', 'sales@lankadistributors.lk', 0.00),
('Cargills Ceylon PLC', '+94112427777', 'info@cargillsceylon.com', 45000.00);

-- Seed Customers
INSERT INTO `customers` (`name`, `phone`, `email`, `loyalty_points`, `balance`) VALUES
('Walk-in Customer', '0000000000', 'walkin@ceyvana.com', 0, 0.00),
('Anura Rajapaksa', '+94771234567', 'anura.r@gmail.com', 120, -1500.00);

-- Seed Products demonstrating different measurement & unit type configurations
-- Stock values:
-- Piece: stock is individual units (e.g., 50 pcs)
-- Packet: stock represents aggregate physical grams (e.g. 50 packs * 400g = 20000g)
-- Gram (g): stock directly in grams (e.g. 3500g)
-- Kilogram (kg): stock in grams (e.g. 25000g representing 25.0 kg)
INSERT INTO `products` (`name`, `sku`, `price`, `cost_price`, `stock`, `low_stock_threshold`, `category_id`, `brand`, `is_weight_based`, `unit`, `unit_type`, `packet_weight`, `packet_weight_unit`, `opening_stock`, `total_weight_in_grams`) VALUES
('Red Rice Samba', 'RIC-SAM-05', 240.00, 190.00, 100, 10, 5, 'Lanka Harvest', 0, 'Piece (pcs)', 'Piece', 0.000, 'g', 100, 0),
('Ceyvana Premium Tea Pack', 'TEA-PRE-400', 850.00, 680.00, 16000, 15, 4, 'Ceyvana', 1, 'Pack', 'Packet', 400.000, 'g', 40, 16000),
('Ceylon Cinnamon Sticks', 'SPI-CIN-RAW', 3.50, 2.10, 5000, 1000, 3, 'Spicelands', 1, 'Gram (g)', 'Gram (g)', 0.000, 'g', 5000, 5000),
('Cardamom Pods Bulk', 'SPI-CAR-BLK', 6200.00, 4800.00, 8500, 2000, 3, 'Spicelands', 1, 'Kilogram (kg)', 'Kilogram (kg)', 0.000, 'g', 8500, 8500);

-- Record Initial Opening Inventory Logs to trace audits cleanly
INSERT INTO `inventory_logs` (`product_id`, `type`, `stock_change`, `previous_stock`, `new_stock`, `total_weight_change_g`, `notes`) VALUES
(1, 'Opening', 100, 0, 100, 0, 'Initial product setup - Red Rice Samba'),
(2, 'Opening', 16000, 0, 16000, 16000, 'Initial product setup - Ceylon Premium Tea Pack (40 Packs * 400g)'),
(3, 'Opening', 5000, 0, 5000, 5000, 'Initial product setup - Ceylon Cinnamon Sticks (5.00kg)'),
(4, 'Opening', 8500, 0, 8500, 8500, 'Initial product setup - Cardamom Pods Bulk (8.50kg)');
