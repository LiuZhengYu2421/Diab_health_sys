package com.imut.diab_health_sys01.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM 加解密工具（用于数据库敏感字段加密，如用户 API Key）。
 * <p>
 * 用途：防止「拖库」后敏感明文直接泄露——即使数据库被整体拖走，
 * API Key 等字段仍是密文，攻击者无法直接使用。
 * <p>
 * 密钥由配置项 app.crypto.secret 经 SHA-256 派生（32 字节），
 * 生产环境务必通过环境变量（APP_CRYPTO_SECRET）注入随机强密钥，切勿使用默认值。
 */
public class AesUtil {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    /** GCM 推荐 IV 长度 12 字节 */
    private static final int IV_LENGTH = 12;
    /** GCM 认证标签 128 位 */
    private static final int TAG_BITS = 128;

    private AesUtil() {
    }

    /** 由任意长度密钥串派生 32 字节 AES-256 密钥 */
    private static byte[] deriveKey(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("密钥派生失败", e);
        }
    }

    /**
     * 加密：返回 base64(iv + ciphertext)，每条密文使用独立随机 IV。
     * 空串原样返回。
     */
    public static String encrypt(String plain, String secret) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        try {
            byte[] key = deriveKey(secret);
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));

            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("敏感字段加密失败", e);
        }
    }

    /**
     * 解密 base64(iv + ciphertext)；空串原样返回。
     * 解密失败（密钥变更/非密文/格式异常）时返回原值，兼容历史明文数据，
     * 避免因加密升级导致存量用户配置不可用。
     */
    public static String decrypt(String data, String secret) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        try {
            byte[] key = deriveKey(secret);
            byte[] in = Base64.getDecoder().decode(data);
            if (in.length <= IV_LENGTH) {
                return data;
            }
            byte[] iv = new byte[IV_LENGTH];
            byte[] body = new byte[in.length - IV_LENGTH];
            System.arraycopy(in, 0, iv, 0, IV_LENGTH);
            System.arraycopy(in, IV_LENGTH, body, 0, body.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(body), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 兼容历史明文数据：无法解密时按原值返回
            return data;
        }
    }
}
