package com.petchat.messenger.security.securestorage;

import static com.petchat.messenger.security.securestorage.SecureStorageException.ExceptionType.CRYPTO_EXCEPTION;
import static com.petchat.messenger.security.securestorage.SecureStorageException.ExceptionType.INTERNAL_LIBRARY_EXCEPTION;
import static com.petchat.messenger.security.securestorage.SecureStorageException.ExceptionType.KEYSTORE_EXCEPTION;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.util.Locale;

import javax.crypto.Cipher;

final class KeystoreTool {
    private static final String
            KEY_ALIAS = "secureKeyPair",
            KEY_ENCRYPTION_ALGORITHM = "RSA",
            KEY_CHARSET = "UTF-8",
            KEY_KEYSTORE_NAME = "AndroidKeyStore",
            KEY_CIPHER_MARSHMALLOW_PROVIDER = "AndroidKeyStoreBCWorkaround",
            KEY_TRANSFORMATION_ALGORITHM = "RSA/ECB/PKCS1Padding";

    // hidden constructor to disable initialization
    private KeystoreTool() { }

    @Nullable
    static String encryptMessage(@NonNull String plainMessage) throws SecureStorageException {
        try {
            Cipher input;
            input = Cipher.getInstance(KEY_TRANSFORMATION_ALGORITHM, KEY_CIPHER_MARSHMALLOW_PROVIDER);

            input.init(Cipher.ENCRYPT_MODE, getPublicKey());

            byte[] inputBytes = plainMessage.getBytes(KEY_CHARSET);
            int blockSize =  2048 / 8 - 11; // Для PKCS1Padding
            int offset = 0;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            while (offset < inputBytes.length) {
                int chunkSize = Math.min(blockSize, inputBytes.length - offset);
                byte[] chunk = input.doFinal(inputBytes, offset, chunkSize);
                outputStream.write(chunk);
                offset += chunkSize;
            }

            return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
        } catch (Exception e) {
            throw new SecureStorageException(e.getMessage(), e, KEYSTORE_EXCEPTION);
        }
    }

    @NonNull
    static String decryptMessage(@NonNull String encryptedMessage) throws SecureStorageException {
        try {
            Cipher output;
            output = Cipher.getInstance(KEY_TRANSFORMATION_ALGORITHM, KEY_CIPHER_MARSHMALLOW_PROVIDER);
            output.init(Cipher.DECRYPT_MODE, getPrivateKey());

            byte[] encryptedBytes = Base64.decode(encryptedMessage, Base64.DEFAULT);
            int blockSize =  2048 / 8; // Для PKCS1Padding
            int offset = 0;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            while (offset < encryptedBytes.length) {
                int chunkSize = Math.min(blockSize, encryptedBytes.length - offset);
                byte[] chunk = output.doFinal(encryptedBytes, offset, chunkSize);
                outputStream.write(chunk);
                offset += chunkSize;
            }

            return outputStream.toString(KEY_CHARSET);
        } catch (Exception e) {
            throw new SecureStorageException(e.getMessage(), e, CRYPTO_EXCEPTION);
        }
    }

    static boolean keyPairExists() throws SecureStorageException {
        try {
            return getKeyStoreInstance().getKey(KEY_ALIAS, null) != null;
        } catch (NoSuchAlgorithmException e) {
            throw new SecureStorageException(e.getMessage(), e, KEYSTORE_EXCEPTION);
        } catch (KeyStoreException | UnrecoverableKeyException e) {
            return false;
        }
    }

    static void generateKeyPair(@NonNull Context context) throws SecureStorageException {
        try {
            if (isRTL(context)) {
                Locale.setDefault(Locale.US);
            }

            KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ENCRYPTION_ALGORITHM, KEY_KEYSTORE_NAME);

            KeyGenParameterSpec keyGenParameterSpec =
                    new KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setKeySize(2048)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                            .build();

            generator.initialize(keyGenParameterSpec);
            generator.generateKeyPair();
        } catch (Exception e) {
            throw new SecureStorageException(e.getMessage(), e, KEYSTORE_EXCEPTION);
        }
    }

    static void deleteKeyPair() throws SecureStorageException {
        try {
            getKeyStoreInstance().deleteEntry(KEY_ALIAS);
        } catch (KeyStoreException e) {
            throw new SecureStorageException(e.getMessage(), e, KEYSTORE_EXCEPTION);
        }
    }

    @Nullable
    private static PublicKey getPublicKey() throws SecureStorageException {
        if (!keyPairExists())
            throw new SecureStorageException("keypair_does_not_exist", null, INTERNAL_LIBRARY_EXCEPTION);

        try {
            return getKeyStoreInstance().getCertificate(KEY_ALIAS).getPublicKey();
        } catch (Exception e) {
            throw new SecureStorageException(e.getMessage(), e, KEYSTORE_EXCEPTION);
        }
    }

    @Nullable
    private static PrivateKey getPrivateKey() throws SecureStorageException {
        if (!keyPairExists())
            throw new SecureStorageException("keypair_does_not_exist", null, INTERNAL_LIBRARY_EXCEPTION);

        try {
            return (PrivateKey) getKeyStoreInstance().getKey(KEY_ALIAS, null);
        } catch (Exception e) {
            throw new SecureStorageException(e.getMessage(), e, KEYSTORE_EXCEPTION);
        }
    }

    private static boolean isRTL(@NonNull Context context) {
        return context.getResources().getConfiguration()
                .getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }

    @NonNull
    private static KeyStore getKeyStoreInstance() throws SecureStorageException {
        try {
            // Get the AndroidKeyStore instance
            KeyStore keyStore = KeyStore.getInstance(KEY_KEYSTORE_NAME);

            // Relict of the JCA API - you have to call load even
            // if you do not have an input stream you want to load or it'll crash
            keyStore.load(null);

            return keyStore;
        } catch (Exception e) {
            throw new SecureStorageException(e.getMessage(), e, KEYSTORE_EXCEPTION);
        }
    }
}
