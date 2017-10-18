/*
 *
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */
package org.kitodo.data.encryption;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DesEncrypter {
    private Cipher encryptionCipher;
    private Cipher decryptionCipher;

    private static final byte[] defaultSalt = {(byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32, (byte) 0x56,
            (byte) 0x35, (byte) 0xE3, (byte) 0x03 };

    private static final Logger logger = LogManager.getLogger(DesEncrypter.class);

    private void initialise(String passPhrase) {
        int iterationCount = 19;

        KeySpec keySpec = new PBEKeySpec(passPhrase.toCharArray(), defaultSalt, iterationCount);

        try {
            SecretKey secretKey = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
            encryptionCipher = Cipher.getInstance(secretKey.getAlgorithm());
            decryptionCipher = Cipher.getInstance(secretKey.getAlgorithm());
            AlgorithmParameterSpec algorithmParameterSpec = new PBEParameterSpec(defaultSalt, iterationCount);
            encryptionCipher.init(Cipher.ENCRYPT_MODE, secretKey, algorithmParameterSpec);
            decryptionCipher.init(Cipher.DECRYPT_MODE, secretKey, algorithmParameterSpec);
        } catch (InvalidKeySpecException e) {
            logger.info("Catched InvalidKeySpecException with message: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            logger.info("Catched NoSuchAlgorithmException with message: " + e.getMessage());
        } catch (NoSuchPaddingException e) {
            logger.info("Catched NoSuchPaddingException with message: " + e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            logger.info("Catched InvalidAlgorithmParameterException with message: " + e.getMessage());
        } catch (InvalidKeyException e) {
            logger.info("Catched InvalidKeyException with message: " + e.getMessage());
        }
    }

    /**
     * Using class constructor with default passphrase for en- and decryption.
     */
    public DesEncrypter() {
        String defaultPassPhrase = "rusDML_Passphrase_for_secure_encryption_2005";
        initialise(defaultPassPhrase);
    }

    /**
     * Encrypt a given string.
     *
     * @param messageToEncrypt
     *            String to encrypt
     * @return encrypted string or null on error
     */
    public String encrypt(String messageToEncrypt) {
        if (messageToEncrypt == null) {
            messageToEncrypt = "";
        }

        try {
            byte[] utfEight = messageToEncrypt.getBytes(StandardCharsets.UTF_8);
            byte[] enc = encryptionCipher.doFinal(utfEight);
            return new String(Base64.encodeBase64(enc), StandardCharsets.UTF_8);
        } catch (BadPaddingException e) {
            logger.warn("Catched BadPaddingException with message: " + e.getMessage());
        } catch (IllegalBlockSizeException e) {
            logger.warn("Catched IllegalBlockSizeException with message: " + e.getMessage());
        }

        return null;
    }

    /**
     * Decrypt a encrypted string.
     *
     * @param messageToDecrypt
     *            String to decrypt
     * @return decrypted string or null on error
     */
    public String decrypt(String messageToDecrypt) {

        try {
            byte[] dec = Base64.decodeBase64(messageToDecrypt.getBytes(StandardCharsets.UTF_8));
            byte[] utfEight = decryptionCipher.doFinal(dec);
            return new String(utfEight, StandardCharsets.UTF_8);
        } catch (IllegalBlockSizeException e) {
            logger.warn("Catched IllegalBlockSizeException with message: " + e.getMessage());
        } catch (BadPaddingException e) {
            logger.warn("Catched BadPaddingException with message: " + e.getMessage());
        }

        return null;
    }
}
