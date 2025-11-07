package ca.footeware.javagi.journal.model;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Is it an encryption service or something more sinister?
 */
public class Superstar {

	/**
	 * Decrypt data using the given password.
	 *
	 * @param encryptedData {@link String}
	 * @param password      {@link String}
	 * @return {@link String}
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws InvalidAlgorithmParameterException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws InvalidKeySpecException
	 */
	public static String decrypt(String encryptedData, String password) throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException,
			BadPaddingException, InvalidKeySpecException {

		// Decode the Base64 encoded data
		byte[] combined = Base64.getDecoder().decode(encryptedData);

		// Extract salt (first 16 bytes)
		byte[] salt = new byte[16];
		System.arraycopy(combined, 0, salt, 0, 16);

		// Extract IV (next 12 bytes)
		byte[] ivBytes = new byte[12];
		System.arraycopy(combined, 16, ivBytes, 0, 12);

		// Extract encrypted data (remaining bytes)
		byte[] encryptedBytes = new byte[combined.length - 28]; // 16 (salt) + 12 (IV) = 28
		System.arraycopy(combined, 28, encryptedBytes, 0, encryptedBytes.length);

		// Generate the same key using password and extracted salt
		SecretKey secretKey = generateAESKey(password, salt);

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, ivBytes);
		cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
		byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

		return new String(decryptedBytes);
	}

	/**
	 * Encrypt data using the given password.
	 *
	 * @param data     {@link String}
	 * @param password {@link String}
	 * @return {@link String}
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeySpecException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public static String encrypt(String data, String password) throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, InvalidAlgorithmParameterException, InvalidKeySpecException, IllegalBlockSizeException,
			BadPaddingException {

		// Generate random salt for key derivation
		byte[] salt = SecureRandom.getInstanceStrong().generateSeed(16);

		// Generate AES key from the password and salt
		SecretKey secretKey = generateAESKey(password, salt);

		// Generate a random 96-bit IV
		byte[] ivBytes = new byte[12];
		SecureRandom.getInstanceStrong().nextBytes(ivBytes);
		GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, ivBytes);
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
		byte[] encryptedData = cipher.doFinal(data.getBytes());

		// Concatenate salt, IV and encrypted data, then Base64 encode the whole thing
		byte[] combined = new byte[salt.length + ivBytes.length + encryptedData.length];
		System.arraycopy(salt, 0, combined, 0, salt.length);
		System.arraycopy(ivBytes, 0, combined, salt.length, ivBytes.length);
		System.arraycopy(encryptedData, 0, combined, salt.length + ivBytes.length, encryptedData.length);

		return Base64.getEncoder().encodeToString(combined);
	}

	/**
	 * Generate a symmetric key for AES encryption using a password and salt.
	 *
	 * @param password {@link String}
	 * @param salt     byte array
	 * @return {@link SecretKey}
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	private static SecretKey generateAESKey(String password, byte[] salt)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, 256);
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		byte[] encodedKey = factory.generateSecret(spec).getEncoded();
		return new SecretKeySpec(encodedKey, "AES");
	}

	/**
	 * Constructor, hidden because all methods are static.
	 */
	private Superstar() {
	}
}
