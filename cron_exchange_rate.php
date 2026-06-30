<?php
/**
 * Cron Job: Sync USD to LKR Exchange Rate
 * Description: Fetches the latest USD/LKR exchange rate from a public API
 *              and updates the database 'settings' table.
 * Usage: php cron_exchange_rate.php
 */

// --- Database Configuration ---
// Adjust these parameters to match your database server configuration.
define('DB_HOST', '127.0.0.1');
define('DB_PORT', '3306');
define('DB_NAME', 'your_database_name');
define('DB_USER', 'your_database_user');
define('DB_PASS', 'your_database_password');

// --- Error Logging ---
// Log errors rather than printing them to stdout in production/silent environments.
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', __DIR__ . '/cron_error.log');

/**
 * Log message helper.
 *
 * @param string $message
 */
function logMessage($message) {
    $timestamp = date('Y-m-d H:i:s');
    echo "[{$timestamp}] {$message}\n";
}

try {
    logMessage("Starting exchange rate sync...");

    // 1. Fetch exchange rate from a reliable public API (ExchangeRate-API open endpoint)
    $apiUrl = "https://open.er-api.com/v6/latest/USD";
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $apiUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 15);
    curl_setopt($ch, CURLOPT_USERAGENT, 'CeyvanaExchangeRateUpdater/1.0');
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    
    if (curl_errno($ch)) {
        throw new Exception("CURL Error: " . curl_error($ch));
    }
    curl_close($ch);

    if ($httpCode !== 200) {
        throw new Exception("Exchange rate API returned non-200 HTTP status code: {$httpCode}");
    }

    $data = json_decode($response, true);
    if (!$data || !isset($data['result']) || $data['result'] !== 'success') {
        throw new Exception("Invalid or failed response structure from the exchange rate API.");
    }

    if (!isset($data['rates']['LKR'])) {
        throw new Exception("LKR (Sri Lankan Rupee) rate not found in the API response.");
    }

    $usdToLkrRate = (double) $data['rates']['LKR'];
    if ($usdToLkrRate <= 0) {
        throw new Exception("Invalid exchange rate value received: {$usdToLkrRate}");
    }

    logMessage("Successfully fetched rate: 1 USD = {$usdToLkrRate} LKR");

    // 2. Connect to the database via PDO
    $dsn = "mysql:host=" . DB_HOST . ";port=" . DB_PORT . ";dbname=" . DB_NAME . ";charset=utf8mb4";
    $options = [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES   => false,
    ];
    
    $pdo = new PDO($dsn, DB_USER, DB_PASS, $options);
    logMessage("Connected to the database successfully.");

    // 3. Update the 'settings' table
    // Choose the update style that matches your database schema:
    //
    // --- Style A: Key-Value settings table (Most Common) ---
    // Schema structure: table 'settings' with columns (key_name, value, updated_at)
    $stmt = $pdo->prepare("
        INSERT INTO settings (key_name, value, updated_at) 
        VALUES ('usd_to_lkr', :rate, NOW()) 
        ON DUPLICATE KEY UPDATE value = :rate_update, updated_at = NOW()
    ");
    
    $stmt->execute([
        'rate' => $usdToLkrRate,
        'rate_update' => $usdToLkrRate
    ]);

    /*
    // --- Style B: Flat column/single-row config table ---
    // Schema structure: table 'settings' with a direct column 'usd_lkr_rate'
    $stmt = $pdo->prepare("
        UPDATE settings 
        SET usd_lkr_rate = :rate, updated_at = NOW() 
        LIMIT 1
    ");
    $stmt->execute(['rate' => $usdToLkrRate]);
    */

    logMessage("Database updated. 'usd_to_lkr' set to {$usdToLkrRate}.");
    logMessage("Exchange rate sync completed successfully.");

} catch (Exception $e) {
    $errorMsg = "Sync failed: " . $e->getMessage();
    logMessage($errorMsg);
    error_log($errorMsg);
    exit(1);
}
