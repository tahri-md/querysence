package com.example.querysence.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.querysence.exception.BadRequestException;

@Service
public class DbConnectionCryptoService {

    private static final String AES = "AES";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 65_536;

    private static final byte[] SALT =
            "querysence-db-connection-salt".getBytes(StandardCharsets.UTF_8);

    private final SecretKeySpec derivedKey;
    private final SecureRandom secureRandom;

    public DbConnectionCryptoService(
            @Value("${db-connection.encryption-key}") String masterKey) {

        validateMasterKey(masterKey);

        this.derivedKey = deriveKey(masterKey);
        this.secureRandom = new SecureRandom();
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);

            cipher.init(Cipher.ENCRYPT_MODE, derivedKey, parameterSpec);

            byte[] ciphertext =
                    cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];

            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(
                    ciphertext,
                    0,
                    combined,
                    iv.length,
                    ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);

        } catch (GeneralSecurityException e) {
            throw new BadRequestException(
                    "Failed to encrypt connection credentials",
                    e);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);

            if (combined.length <= IV_LENGTH_BYTES) {
                throw new BadRequestException(
                        "Invalid encrypted connection credentials");
            }

            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] ciphertext =
                    new byte[combined.length - IV_LENGTH_BYTES];

            System.arraycopy(
                    combined,
                    0,
                    iv,
                    0,
                    IV_LENGTH_BYTES);

            System.arraycopy(
                    combined,
                    IV_LENGTH_BYTES,
                    ciphertext,
                    0,
                    ciphertext.length);

            Cipher cipher = Cipher.getInstance(AES_GCM);

            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    derivedKey,
                    parameterSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new BadRequestException(
                    "Failed to decrypt connection credentials",
                    e);
        }
    }

    private SecretKeySpec deriveKey(String masterKey) {
        PBEKeySpec spec = new PBEKeySpec(
                masterKey.toCharArray(),
                SALT,
                PBKDF2_ITERATIONS,
                KEY_LENGTH_BITS);

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);

            byte[] keyBytes = factory
                    .generateSecret(spec)
                    .getEncoded();

            return new SecretKeySpec(keyBytes, AES);

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Failed to derive encryption key",
                    e);
        } finally {
            spec.clearPassword();
        }
    }

    private static void validateMasterKey(String masterKey) {
        if (masterKey == null || masterKey.length() < 16) {
            throw new IllegalStateException(
                    "DB_CONNECTION_ENCRYPTION_KEY must be set "
                            + "and at least 16 characters long");
        }
    }
}