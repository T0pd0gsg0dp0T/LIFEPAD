package com.lifepad.app.security

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
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

    /**
     * Plain (unencrypted) prefs — stores only non-secret key-derivation parameters and a
     * PIN-encrypted passphrase backup. Survives EncryptedSharedPreferences data-clear events.
     */
    private val plainPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("lifepad_plain_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val TAG = "SecurityManager"

        // Encrypted prefs keys
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_HASH_LEGACY = "pin_hash_legacy"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_DB_PASSPHRASE = "db_passphrase"
        private const val KEY_LOCK_TIMEOUT_MS = "lock_timeout_ms"
        private const val KEY_LAST_BACKGROUND_ELAPSED = "last_background_elapsed"
        private const val KEY_SKIPPED_PIN_SETUP = "skipped_pin_setup"
        private const val KEY_DB_ENCRYPTED = "db_encrypted"
        private const val KEY_ATTEMPTS_REMAINING = "attempts_remaining"
        private const val KEY_NEEDS_LOCK = "needs_lock"

        // Lockout keys (elapsed-realtime based, with wall-clock fallback for post-reboot)
        private const val KEY_LOCKOUT_ELAPSED_START = "lockout_elapsed_start"
        private const val KEY_LOCKOUT_WALLCLOCK_START = "lockout_wallclock_start"
        private const val KEY_LOCKOUT_DURATION_MS = "lockout_duration_ms"

        // Plain prefs keys (passphrase backup / key derivation)
        private const val PLAIN_KEY_KDF_SALT = "kdf_salt"
        private const val PLAIN_KEY_PASSPHRASE_BACKUP = "passphrase_backup"
        private const val PLAIN_KEY_RECOVERY_ATTEMPTED = "pref_recovery_attempted"

        private const val DEFAULT_TIMEOUT_MS = 60_000L // 1 minute
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 5 * 60_000L // 5 minutes
        private const val PBKDF2_ITERATIONS = 100_000
        private const val PBKDF2_KEY_LENGTH = 256
    }

    // ─── PIN management ────────────────────────────────────────────────────────

    fun isPinSet(): Boolean = prefs.contains(KEY_PIN_HASH)

    fun hasSkippedPinSetup(): Boolean = prefs.getBoolean(KEY_SKIPPED_PIN_SETUP, false)

    fun setSkippedPinSetup() {
        prefs.edit().putBoolean(KEY_SKIPPED_PIN_SETUP, true).apply()
    }

    fun setPin(pin: String) {
        val pinSalt = generateSalt()
        val hash = hashPinPbkdf2(pin, pinSalt)
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, pinSalt)
            .remove(KEY_PIN_HASH_LEGACY) // Remove legacy SHA-256 hash if present
            .putBoolean(KEY_SKIPPED_PIN_SETUP, false)
            .apply()

        // Generate DB passphrase on first PIN setup if not already present
        if (!prefs.contains(KEY_DB_PASSPHRASE)) {
            val passphrase = generatePassphrase()
            prefs.edit().putString(KEY_DB_PASSPHRASE, passphrase).apply()
        }

        storePassphraseBackup(pin, overwrite = true)
    }

    fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false

        // Check for legacy SHA-256 hash — upgrade transparently if matched (Fix #4 migration)
        val legacyHash = prefs.getString(KEY_PIN_HASH_LEGACY, null)
        if (legacyHash != null && hashPinSha256Legacy(pin, salt) == legacyHash) {
            upgradePinHashToPbkdf2(pin, salt)
            storePassphraseBackup(pin)
            return true
        }

        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val pbkdf2Hash = hashPinPbkdf2OrNull(pin, salt)
        if (pbkdf2Hash == storedHash) {
            storePassphraseBackup(pin)
            return true
        }

        // Older builds stored the legacy SHA-256 value under KEY_PIN_HASH itself.
        // Accept it once, then replace it with the PBKDF2 hash expected by current builds.
        if (hashPinSha256Legacy(pin, salt) == storedHash) {
            upgradePinHashToPbkdf2(pin, salt)
            storePassphraseBackup(pin)
            return true
        }

        return false
    }

    private fun upgradePinHashToPbkdf2(pin: String, existingSalt: String) {
        val salt = if (existingSalt.isHexString()) existingSalt else generateSalt()
        val newHash = hashPinPbkdf2(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_HASH, newHash)
            .putString(KEY_PIN_SALT, salt)
            .remove(KEY_PIN_HASH_LEGACY)
            .apply()
        Log.i(TAG, "PIN hash upgraded from SHA-256 to PBKDF2")
    }

    private fun storePassphraseBackup(pin: String, overwrite: Boolean = false) {
        if (!overwrite && plainPrefs.contains(PLAIN_KEY_PASSPHRASE_BACKUP)) return

        val currentPassphrase = prefs.getString(KEY_DB_PASSPHRASE, null) ?: return
        val kdfSalt = getOrCreateKdfSalt()
        val backup = encryptPassphraseWithPin(currentPassphrase, pin, kdfSalt)
        plainPrefs.edit().putString(PLAIN_KEY_PASSPHRASE_BACKUP, backup).apply()
    }

    private fun getOrCreateKdfSalt(): String {
        val existingSalt = plainPrefs.getString(PLAIN_KEY_KDF_SALT, null)
        if (existingSalt != null) return existingSalt

        val salt = generateSalt()
        plainPrefs.edit().putString(PLAIN_KEY_KDF_SALT, salt).apply()
        return salt
    }

    // ─── Passphrase backup / recovery (Fix #1) ─────────────────────────────────

    /**
     * True if encrypted passphrase is missing from secure prefs but a PIN-encrypted backup
     * exists in plain prefs — indicates EncryptedSharedPreferences was wiped.
     */
    fun needsPassphraseRecovery(): Boolean {
        val hasPassphraseInSecurePrefs = prefs.contains(KEY_DB_PASSPHRASE)
        val hasBackupInPlainPrefs = plainPrefs.contains(PLAIN_KEY_KDF_SALT) &&
                plainPrefs.contains(PLAIN_KEY_PASSPHRASE_BACKUP)
        return !hasPassphraseInSecurePrefs && hasBackupInPlainPrefs
    }

    /**
     * Decrypts the passphrase backup using the PIN and restores it to encrypted prefs.
     * Returns true on success. Call this before opening the DB when needsPassphraseRecovery() is true.
     */
    fun recoverPassphraseFromPin(pin: String): Boolean {
        val kdfSalt = plainPrefs.getString(PLAIN_KEY_KDF_SALT, null) ?: return false
        val backup = plainPrefs.getString(PLAIN_KEY_PASSPHRASE_BACKUP, null) ?: return false
        return try {
            val passphrase = decryptPassphraseWithPin(backup, pin, kdfSalt)
            prefs.edit().putString(KEY_DB_PASSPHRASE, passphrase).apply()
            Log.i(TAG, "Passphrase recovered from PIN-encrypted backup")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Passphrase recovery failed — wrong PIN or corrupted backup", e)
            false
        }
    }

    // ─── DB passphrase ─────────────────────────────────────────────────────────

    @Synchronized // Fix #3: prevent race condition during first-run passphrase generation
    fun getDbPassphrase(): ByteArray {
        val passphrase = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (passphrase != null) {
            return passphrase.toByteArray()
        }
        // First run — generate and store passphrase
        val newPassphrase = generatePassphrase()
        prefs.edit().putString(KEY_DB_PASSPHRASE, newPassphrase).apply()
        return newPassphrase.toByteArray()
    }

    fun isDbEncrypted(): Boolean = prefs.getBoolean(KEY_DB_ENCRYPTED, false)

    // ─── Database migration (Fix #2, #7) ──────────────────────────────────────

    /**
     * Migrates an existing unencrypted database to SQLCipher encryption.
     * Backs up WAL/SHM before deleting; restores all three files on failure.
     * Returns true if the DB is encrypted (or no DB exists yet), false if migration failed.
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
        val walFile = File("${dbFile.absolutePath}-wal")
        val shmFile = File("${dbFile.absolutePath}-shm")
        val walBackupFile = File("${dbFile.absolutePath}-wal.backup")
        val shmBackupFile = File("${dbFile.absolutePath}-shm.backup")
        val tempFile = File(dbFile.parent, "${dbFile.name}.tmp")

        try {
            // Backup original DB and WAL/SHM before touching anything
            dbFile.copyTo(backupFile, overwrite = true)
            if (walFile.exists()) walFile.copyTo(walBackupFile, overwrite = true)
            if (shmFile.exists()) shmFile.copyTo(shmBackupFile, overwrite = true)
            tempFile.delete()

            net.sqlcipher.database.SQLiteDatabase.loadLibs(context)

            // Create a new encrypted database in temp file
            val encryptedDb = net.sqlcipher.database.SQLiteDatabase.openOrCreateDatabase(
                tempFile, passphraseStr, null
            )

            // Attach unencrypted DB as 'plaintext' (KEY '' = no encryption)
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
            walFile.delete()
            shmFile.delete()
            tempFile.renameTo(dbFile)

            // Clean up all backups on success
            backupFile.delete()
            walBackupFile.delete()
            shmBackupFile.delete()

            prefs.edit().putBoolean(KEY_DB_ENCRYPTED, true).apply()
            Log.i(TAG, "Database encrypted successfully")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Database encryption failed, restoring backup", e)
            tempFile.delete()

            // Restore main DB
            if (backupFile.exists()) {
                dbFile.delete()
                backupFile.renameTo(dbFile)
            }
            // Restore WAL
            if (walBackupFile.exists()) {
                walFile.delete()
                walBackupFile.renameTo(walFile)
            } else {
                walFile.delete()
            }
            // Restore SHM
            if (shmBackupFile.exists()) {
                shmFile.delete()
                shmBackupFile.renameTo(shmFile)
            } else {
                shmFile.delete()
            }
            return false
        }
    }

    // ─── Lock / timeout ────────────────────────────────────────────────────────

    fun getLockTimeoutMs(): Long = prefs.getLong(KEY_LOCK_TIMEOUT_MS, DEFAULT_TIMEOUT_MS)

    fun setLockTimeoutMs(timeoutMs: Long) {
        prefs.edit().putLong(KEY_LOCK_TIMEOUT_MS, timeoutMs).apply()
    }

    /** Records elapsed-realtime at background transition (manipulation-resistant). Fix #6. */
    fun setLastBackgroundTime(elapsedMs: Long) {
        prefs.edit().putLong(KEY_LAST_BACKGROUND_ELAPSED, elapsedMs).apply()
    }

    fun getLastBackgroundTime(): Long = prefs.getLong(KEY_LAST_BACKGROUND_ELAPSED, 0L)

    fun setNeedsLock() {
        prefs.edit().putBoolean(KEY_NEEDS_LOCK, true).apply()
    }

    fun clearNeedsLock() {
        prefs.edit().putBoolean(KEY_NEEDS_LOCK, false).apply()
    }

    fun needsLock(): Boolean = prefs.getBoolean(KEY_NEEDS_LOCK, true)

    fun shouldLockAfterBackground(): Boolean {
        if (!isPinSet()) return false
        return needsLock()
    }

    // ─── Lockout (Fix #6) ─────────────────────────────────────────────────────

    fun isLockedOut(): Boolean {
        val duration = prefs.getLong(KEY_LOCKOUT_DURATION_MS, 0L)
        if (duration <= 0L) return false

        val elapsedStart = prefs.getLong(KEY_LOCKOUT_ELAPSED_START, 0L)
        if (elapsedStart > 0L) {
            val elapsed = SystemClock.elapsedRealtime() - elapsedStart
            if (elapsed >= 0L) {
                // Elapsed-realtime path: resistant to wall-clock manipulation
                return elapsed < duration
            }
            // elapsed < 0 means device rebooted since lockout was recorded — fall through
        }

        // Post-reboot fallback: use wall-clock start time
        val wallClockStart = prefs.getLong(KEY_LOCKOUT_WALLCLOCK_START, 0L)
        if (wallClockStart <= 0L) return false
        return (System.currentTimeMillis() - wallClockStart) < duration
    }

    fun getLockoutRemainingMs(): Long {
        val duration = prefs.getLong(KEY_LOCKOUT_DURATION_MS, 0L)
        if (duration <= 0L) return 0L

        val elapsedStart = prefs.getLong(KEY_LOCKOUT_ELAPSED_START, 0L)
        if (elapsedStart > 0L) {
            val elapsed = SystemClock.elapsedRealtime() - elapsedStart
            if (elapsed >= 0L) {
                return maxOf(0L, duration - elapsed)
            }
        }

        val wallClockStart = prefs.getLong(KEY_LOCKOUT_WALLCLOCK_START, 0L)
        if (wallClockStart <= 0L) return 0L
        return maxOf(0L, duration - (System.currentTimeMillis() - wallClockStart))
    }

    fun getAttemptsRemaining(): Int = prefs.getInt(KEY_ATTEMPTS_REMAINING, MAX_ATTEMPTS)

    /** Records a failed unlock attempt. Returns remaining attempts (0 = locked out). */
    fun recordFailedAttempt(): Int {
        val remaining = getAttemptsRemaining() - 1
        if (remaining <= 0) {
            prefs.edit()
                .putInt(KEY_ATTEMPTS_REMAINING, 0)
                .putLong(KEY_LOCKOUT_ELAPSED_START, SystemClock.elapsedRealtime())
                .putLong(KEY_LOCKOUT_WALLCLOCK_START, System.currentTimeMillis())
                .putLong(KEY_LOCKOUT_DURATION_MS, LOCKOUT_DURATION_MS)
                .apply()
            return 0
        }
        prefs.edit().putInt(KEY_ATTEMPTS_REMAINING, remaining).apply()
        return remaining
    }

    fun resetFailedAttempts() {
        prefs.edit()
            .putInt(KEY_ATTEMPTS_REMAINING, MAX_ATTEMPTS)
            .putLong(KEY_LOCKOUT_ELAPSED_START, 0L)
            .putLong(KEY_LOCKOUT_WALLCLOCK_START, 0L)
            .putLong(KEY_LOCKOUT_DURATION_MS, 0L)
            .apply()
    }

    // ─── Crypto helpers ────────────────────────────────────────────────────────

    /** Fix #4: PBKDF2WithHmacSHA256 with 100,000 iterations. */
    private fun hashPinPbkdf2(pin: String, salt: String): String {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(pin.toCharArray(), salt.decodeHex(), PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
        return factory.generateSecret(spec).encoded.toHex()
    }

    private fun hashPinPbkdf2OrNull(pin: String, salt: String): String? {
        return try {
            hashPinPbkdf2(pin, salt)
        } catch (e: Exception) {
            Log.w(TAG, "Stored PIN salt is not compatible with PBKDF2; trying legacy verification", e)
            null
        }
    }

    /** Legacy SHA-256 hash — used only for transparent migration detection. */
    private fun hashPinSha256Legacy(pin: String, salt: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest("$salt:$pin".toByteArray()).toHex()
    }

    /** AES-GCM-256 encrypt passphrase using PBKDF2-derived key from PIN + kdfSalt. */
    private fun encryptPassphraseWithPin(passphrase: String, pin: String, kdfSalt: String): String {
        val key = deriveKeyFromPin(pin, kdfSalt)
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(passphrase.toByteArray(Charsets.UTF_8))
        // Store as Base64(iv || ciphertext)
        return Base64.encodeToString(iv + ciphertext, Base64.NO_WRAP)
    }

    /** AES-GCM-256 decrypt passphrase using PBKDF2-derived key from PIN + kdfSalt. */
    private fun decryptPassphraseWithPin(backup: String, pin: String, kdfSalt: String): String {
        val combined = Base64.decode(backup, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, 12)
        val ciphertext = combined.copyOfRange(12, combined.size)
        val key = deriveKeyFromPin(pin, kdfSalt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun deriveKeyFromPin(pin: String, kdfSalt: String): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(pin.toCharArray(), kdfSalt.decodeHex(), PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
        return factory.generateSecret(spec).encoded
    }

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    private fun generatePassphrase(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Hex string must have even length" }
        return ByteArray(length / 2) { i ->
            ((this[i * 2].digitToInt(16) shl 4) + this[i * 2 + 1].digitToInt(16)).toByte()
        }
    }

    private fun String.isHexString(): Boolean {
        return length % 2 == 0 && all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }

    // ─── Encrypted prefs creation (Fix #5) ────────────────────────────────────

    private fun createEncryptedPrefsWithRecovery(): SharedPreferences {
        // Use a local reference to avoid circular lazy init (plainPrefs also lazily initialized)
        val plain = context.getSharedPreferences("lifepad_plain_prefs", Context.MODE_PRIVATE)
        return try {
            createEncryptedPrefs()
        } catch (e: Exception) {
            Log.e(TAG, "Encrypted prefs init failed", e)
            val alreadyAttempted = plain.getBoolean(PLAIN_KEY_RECOVERY_ATTEMPTED, false)
            if (!alreadyAttempted) {
                Log.w(TAG, "First failure — resetting encrypted prefs and retrying")
                plain.edit().putBoolean(PLAIN_KEY_RECOVERY_ATTEMPTED, true).apply()
                context.deleteSharedPreferences("lifepad_secure_prefs")
                try {
                    val result = createEncryptedPrefs()
                    plain.edit().remove(PLAIN_KEY_RECOVERY_ATTEMPTED).apply()
                    result
                } catch (e2: Exception) {
                    Log.e(TAG, "Second failure — falling back to unencrypted prefs", e2)
                    context.getSharedPreferences("lifepad_fallback_prefs", Context.MODE_PRIVATE)
                }
            } else {
                Log.w(TAG, "Recovery already attempted — using fallback unencrypted prefs")
                plain.edit().remove(PLAIN_KEY_RECOVERY_ATTEMPTED).apply()
                context.getSharedPreferences("lifepad_fallback_prefs", Context.MODE_PRIVATE)
            }
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
