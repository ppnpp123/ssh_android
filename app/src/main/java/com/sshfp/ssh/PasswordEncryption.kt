package com.sshfp.ssh

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 使用Android Keystore进行密码加密
 */
class PasswordEncryption(private val context: Context) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "sshfp_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    private val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    /**
     * 获取或生成主密钥
     */
    private fun getOrCreateSecretKey(): SecretKey {
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        return existingKey ?: generateKey()
    }

    /**
     * 生成新的AES密钥
     */
    private fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        val keyGenSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .build()
        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }

    /**
     * 加密文本
     */
    fun encrypt(text: String): String {
        try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(text.toByteArray(StandardCharsets.UTF_8))

            // 将IV和加密数据合并: IV(12 bytes) + EncryptedData
            val combined = iv + encryptedBytes
            return Base64.getEncoder().encodeToString(combined)
        } catch (e: Exception) {
            throw PasswordEncryptionException("Encryption failed: ${e.message}", e)
        }
    }

    /**
     * 解密文本
     */
    fun decrypt(encryptedText: String): String {
        try {
            val combined = Base64.getDecoder().decode(encryptedText)

            if (combined.size < 12) {
                throw PasswordEncryptionException("Invalid encrypted data")
            }

            val iv = combined.sliceArray(0 until 12)
            val encryptedBytes = combined.sliceArray(12 until combined.size)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            throw PasswordEncryptionException("Decryption failed: ${e.message}", e)
        }
    }

    /**
     * 加密异常
     */
    class PasswordEncryptionException(message: String, cause: Throwable? = null) :
        Exception(message, cause)
}
