package com.lifepad.app.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor(
    private val context: Context
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs: SharedPreferences by lazy {
        createEncryptedPrefsWithRecovery()
    }

    companion object {
        private const val TAG = "SecurityManager"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_DB_PASSPHRASE = "db_passphrase"
        private const val KEY_LOCK_TIMEOUT_MS = "lock_timeout_ms"
        private const val KEY_LAST_BACKGROUND_TIME = "last_background_time"
        private const val KEY_SKIPPED_PIN_SETUP = "skipped_pin_setup"
        private const val KEY_DB_ENCRYPTED = "db_encrypted"
        private const val KEY_ATTEMPTS_REMAINING = "attempts_remaining"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
        private const val KEY_NEEDS_LOCK = "needs_lock"
        private const val DEFAULT_TIMEOUT_MS = 60_000L // 1 minute
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 5 * 60_000L // 5 minutes
    }

    fun isPinSet(): Boolean = prefs.contains(KEY_PIN_HASH)

    fun hasSkippedPinSetup(): Boolean = prefs.getBoolean(KEY_SKIPPED_PIN_SETUP, false)

    fun setSkippedPinSetup() {
        prefs.edit().putBoolean(KEY_SKIPPED_PIN_SETUP, true).apply()
    }

    fun setPin(pin: String) {
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .putBoolean(KEY_SKIPPED_PIN_SETUP, false)
            .apply()

        // Generate DB passphrase on first PIN setup if not exists
        if (!prefs.contains(KEY_DB_PASSPHRASE)) {
            val passphrase = generatePassphrase()
            prefs.edit().putString(KEY_DB_PASSPHRASE, passphrase).apply()
        }
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        return hashPin(pin, salt) == storedHash
    }

    fun isLockedOut(): Boolean {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        return System.currentTimeMillis() < lockoutUntil
    }

    fun getLockoutRemainingMs(): Long {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        return maxOf(0L, lockoutUntil - System.currentTimeMillis())
    }

    fun getAttemptsRemaining(): Int = prefs.getInt(KEY_ATTEMPTS_REMAINING, MAX_ATTEMPTS)

    /** Records a failed unlock attempt. Returns remaining attempts (0 = locked out). */
    fun recordFailedAttempt(): Int {
        val remaining = getAttemptsRemaining() - 1
        if (remaining <= 0) {
            prefs.edit()
                .putInt(KEY_ATTEMPTS_REMAINING, 0)
                .putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + LOCKOUT_DURATION_MS)
                .apply()
            return 0
        }
        prefs.edit().putInt(KEY_ATTEMPTS_REMAINING, remaining).apply()
        return remaining
    }

    fun resetFailedAttempts() {
        prefs.edit()
            .putInt(KEY_ATTEMPTS_REMAINING, MAX_ATTEMPTS)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    fun getDbPassphrase(): ByteArray {
        val passphrase = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (passphrase != null) {
            return passphrase.toByteArray()
        }
        // First run - generate and store passphrase
        val newPassphrase = generatePassphrase()
        prefs.edit().putString(KEY_DB_PASSPHRASE, newPassphrase).apply()
        return newPassphrase.toByteArray()
    }

    fun isDbEncrypted(): Boolean = prefs.getBoolean(KEY_DB_ENCRYPTED, false)

    /**
     * Migrates an existing unencrypted database to SQLCipher encryption.
     * Returns true if the DB is encrypted (or no DB exists yet), false if migration failed
     * and the DB remains unencrypted.
     */
    fun migrateDatabaseToEncrypted(dbName: String): Boolean {
        val dbFile = context.getDatabasePath(dbName)

        // No existing DB — Room will create a fresh encrypted one
        if (!dbFile.exists()) {
            prefs.edit().putBoolean(KEY_DB_ENCRYPTED, true).apply()
            return true
        }

        // Already encrypted from a previous run
        if (isDbEncrypted()) return true

        // Check if the file is plaintext SQLite (header: "SQLite format 3\0")
        val isPlaintext = try {
            dbFile.inputStream().use { stream ->
                val header = ByteArray(16)
                val bytesRead = stream.read(header)
                bytesRead == 16 && String(header, Charsets.US_ASCII).startsWith("SQLite format 3")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cannot read DB header", e)
            return false
        }

        if (!isPlaintext) {
            // File exists but isn't plaintext SQLite — assume already encrypted
            prefs.edit().putBoolean(KEY_DB_ENCRYPTED, true).apply()
            return true
        }

        // Database is unencrypted — encrypt it
        val passphrase = getDbPassphrase()
        val passphraseStr = String(passphrase, Charsets.UTF_8)
        val backupFile = File(dbFile.parent, "${dbFile.name}.backup")
        val tempFile = File(dbFile.parent, "${dbFile.name}.tmp")

        try {
            // Backup original
            dbFile.copyTo(backupFile, overwrite = true)
            tempFile.delete()

            net.sqlcipher.database.SQLiteDatabase.loadLibs(context)

            // Create a new encrypted database
            val encryptedDb = net.sqlcipher.database.SQLiteDatabase.openOrCreateDatabase(
                tempFile, passphraseStr, null
            )

            // Attach the unencrypted DB as 'plaintext' (KEY '' = no encryption)
            encryptedDb.rawExecSQL("ATTACH DATABASE '${dbFile.absolutePath}' AS plaintext KEY ''")

            // Copy all data from plaintext → main (encrypted)
            encryptedDb.rawExecSQL("SELECT sqlcipher_export('main', 'plaintext')")

            // Preserve Room's schema version
            val cursor = encryptedDb.rawQuery("PRAGMA plaintext.user_version", null)
            var version = 0
            if (cursor.moveToFirst()) {
                version = cursor.getInt(0)
            }
            cursor.close()
            if (version > 0) {
                encryptedDb.rawExecSQL("PRAGMA user_version = $version")
            }

            encryptedDb.rawExecSQL("DETACH DATABASE plaintext")
            encryptedDb.close()

            // Replace original with encrypted version
            dbFile.delete()
            File("${dbFile.absolutePath}-wal").delete()
            File("${dbFile.absolutePath}-shm").delete()
            tempFile.renameTo(dbFile)

            // Clean up backup
            backupFile.delete()

            prefs.edit().putBoolean(KEY_DB_ENCRYPTED, true).apply()
            Log.i(TAG, "Database encrypted successfully")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Database encryption failed, restoring backup", e)
            tempFile.delete()
            if (backupFile.exists()) {
                dbFile.delete()
                backupFile.renameTo(dbFile)
            }
            return false
        }
    }

    fun getLockTimeoutMs(): Long = prefs.getLong(KEY_LOCK_TIMEOUT_MS, DEFAULT_TIMEOUT_MS)

    fun setLockTimeoutMs(timeoutMs: Long) {
        prefs.edit().putLong(KEY_LOCK_TIMEOUT_MS, timeoutMs).apply()
    }

    fun setLastBackgroundTime(timeMs: Long) {
        prefs.edit().putLong(KEY_LAST_BACKGROUND_TIME, timeMs).apply()
    }

    fun getLastBackgroundTime(): Long = prefs.getLong(KEY_LAST_BACKGROUND_TIME, 0L)

    fun setNeedsLock() {
        prefs.edit().putBoolean(KEY_NEEDS_LOCK, true).apply()
    }

    fun clearNeedsLock() {
        prefs.edit().putBoolean(KEY_NEEDS_LOCK, false).apply()
    }

    fun needsLock(): Boolean = prefs.getBoolean(KEY_NEEDS_LOCK, true)

    fun shouldLockAfterBackground(): Boolean {
        if (!isPinSet()) return false
        // Most secure: lock whenever the app has left the foreground.
        // Default to true so cold-starts require PIN after one is set.
        return needsLock()
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$salt:$pin".toByteArray()
        val hash = digest.digest(input)
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun generatePassphrase(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun createEncryptedPrefsWithRecovery(): SharedPreferences {
        return try {
            createEncryptedPrefs()
        } catch (e: Exception) {
            Log.e(TAG, "Encrypted prefs init failed, resetting secure prefs", e)
            context.deleteSharedPreferences("lifepad_secure_prefs")
            createEncryptedPrefs()
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            "lifepad_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
