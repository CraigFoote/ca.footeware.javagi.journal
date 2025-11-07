/**
 *
 */
package ca.footeware.javagi.journal.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * A wrapper around a {@link Properties} file persisting a {@link TreeMap} whose
 * keys are date {@link String}s in format yyyy-MM-dd and the values are
 * encrypted {@link String}s.
 */
public class Journal {

	private File file;
	private Map<String, String> map;
	private String password;
	private Properties properties;

	/**
	 * Constructor.
	 *
	 * @param file     {@link File}
	 * @param password {@link String}
	 * @throws IOException if the journal cannot be loaded
	 */
	public Journal(File file, String password) throws IOException {
		this.file = file;
		this.password = password;
		this.map = new TreeMap<>();
		this.properties = new Properties();
		try (var in = new FileInputStream(file)) {
			this.properties.load(in);
		}
		/*
		 * The TreeMap, this.map, is natively sorted by key (so date strings are
		 * ascending), is the data model object. The Properties file, this.properties,
		 * is the persistence vector. Copy its entries to the map.
		 */
		this.properties.entrySet().forEach(entry -> map.put((String) entry.getKey(), (String) entry.getValue()));
	}

	/**
	 * Adds an entry to the journal.
	 *
	 * @param key   {@link String} a date in the format yyyy-mm-dd
	 * @param value {@link String} to be encrypted
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeySpecException
	 */
	public void addEntry(String key, String value)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException,
			BadPaddingException, InvalidAlgorithmParameterException, InvalidKeySpecException {
		if (value == null || value.isBlank()) {
			map.remove(key);
		} else {
			String encrypted = Superstar.encrypt(value, password);
			map.put(key, encrypted);
		}
	}

	/**
	 * Gets all entries from the journal.
	 *
	 * @return Map<String, String>
	 */
	public Map<String, String> getEntries() {
		return map;
	}

	/**
	 * Gets an entry from the journal.
	 *
	 * @param key {@link String}
	 * @return {@link String} may be null if there's no entry for the provided date
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeySpecException
	 */
	public String getEntry(String key)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException,
			BadPaddingException, InvalidAlgorithmParameterException, InvalidKeySpecException {
		String encrypted = map.get(key);
		if (encrypted != null) {
			return Superstar.decrypt(encrypted, password);
		}
		return null;
	}

	/**
	 * Saves the journal to disk.
	 *
	 * @throws IOException
	 */
	public void save() throws IOException {
		properties.clear();
		map.forEach((k, v) -> properties.put(k, v));
		try (var out = new FileOutputStream(file)) {
			properties.store(out, null);
		}
	}

	/**
	 * Checks the password can decrypt an entry.
	 *
	 * @return boolean true if password worked
	 */
	public boolean testPassword() {
		if (!map.isEmpty()) {
			Entry<String, String> entry = map.entrySet().iterator().next();
			try {
				Superstar.decrypt(entry.getValue(), password);
			} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
					| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
					| InvalidKeySpecException _) {
				return false;
			}
		}
		return true;
	}
}
