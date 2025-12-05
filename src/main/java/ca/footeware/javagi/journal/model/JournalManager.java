/**
 *
 */
package ca.footeware.javagi.journal.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Provides a read/write interface to the {@link Journal} using
 * {@link LocalDate} keys and {@link String} values.
 */
public class JournalManager {

	private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static Journal journal;

	/**
	 * Add an entry to the journal.
	 *
	 * @param key   {@link LocalDate}
	 * @param value {@link String} the text of the entry, to be encrypted
	 * @throws JournalException
	 */
	public static void addEntry(LocalDate key, String value) throws JournalException {
		try {
			journal.addEntry(key.format(dateFormatter), value);
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
				| BadPaddingException | InvalidAlgorithmParameterException | InvalidKeySpecException e) {
			throw new JournalException("Error adding entry to journal.", e);
		}
	}

	/**
	 * Creates a new journal in the provided file using the provided password.
	 *
	 * @param file     {@link org.gnome.gio.File}
	 * @param password {@link String}
	 * @throws IOException
	 */
	public static void createJournal(org.gnome.gio.File file, String password) throws IOException {
		createJournal(file.getPath(), password);
	}

	/**
	 * Creates a new journal at the specified path with the specified name and
	 * password.
	 *
	 * @param pathName {@link String}
	 * @param password {@link String}
	 * @throws IOException
	 */
	public static void createJournal(String pathName, String password) throws IOException {
		File file = new File(pathName);
		if (file.exists()) {
			// nasty but the Gtk FileDialog would have prompted us to overwrite
			if (!file.canWrite()) {
				throw new IOException("Unknown error, file cannot be written to.");
			}
			Files.delete(file.toPath());
		}
		boolean newFile = file.createNewFile();
		if (!newFile) {
			throw new IOException("Unknown error, file was not created.");
		}
		journal = new Journal(file, password);
	}

	/**
	 * Gets the journal entry for provided date.
	 *
	 * @param date {@link LocalDate}
	 * @return {@link String}
	 * @throws JournalException
	 */
	public static String getEntry(LocalDate date) throws JournalException {
		try {
			if (date == null) {
				throw new IllegalArgumentException("Provided date must not be null.");
			}
			String formatted = date.format(dateFormatter);
			return journal.getEntry(formatted);
		} catch (IllegalArgumentException e) {
			throw new JournalException("Error: " + e.getMessage(), e);
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
				| BadPaddingException | InvalidAlgorithmParameterException | InvalidKeySpecException e) {
			throw new JournalException("Error fetching journal entry.", e);
		}
	}

	/**
	 * Gets the list of entry date values; the set of keys.
	 *
	 * @return {@link List} of {@link LocalDate}
	 */
	public static List<LocalDate> getEntryDates() {
		List<LocalDate> keys = new ArrayList<>();
		Map<String, String> entries = journal.getEntries();
		for (String key : entries.keySet()) {
			keys.add(LocalDate.parse(key, dateFormatter));
		}
		return keys;
	}

	/**
	 * Get the first journal entry date (key).
	 *
	 * @return {@link LocalDate}
	 */
	public static LocalDate getFirstEntryDate() {
		List<LocalDate> entryDates = getEntryDates();
		if (!entryDates.isEmpty()) {
			return entryDates.get(0);
		}
		return null;
	}

	/**
	 * Get the last journal entry date (key).
	 *
	 * @return {@link LocalDate}
	 */
	public static LocalDate getLastEntryDate() {
		List<LocalDate> entryDates = getEntryDates();
		if (!entryDates.isEmpty()) {
			return entryDates.get(entryDates.size() - 1);
		}
		return null;
	}

	/**
	 * Get the next journal entry after the provided date.
	 *
	 * @param selectedDate {@link LocalDate}
	 * @return {@link LocalDate} may be the same as the provided date if there is no
	 *         subsequent entry.
	 */
	public static LocalDate getNextEntryDate(LocalDate selectedDate) {
		List<LocalDate> entryDates = getEntryDates();
		switch (entryDates.size()) {
		case 0 -> {
			return selectedDate;
		}
		case 1 -> {
			return entryDates.get(0).isAfter(selectedDate) ? entryDates.get(0) : selectedDate;
		}
		default -> {
			LocalDate nextDate = parseForNext(selectedDate, entryDates);
			if (nextDate != null) {
				return nextDate;
			}
		}
		}
		// fallback is same date
		return selectedDate;
	}

	/**
	 * Get the previous journal entry before the provided date.
	 *
	 * @param selectedDate {@link LocalDate}
	 * @return {@link LocalDate} may be the same as the provided date if there's no
	 *         previous entry
	 */
	public static LocalDate getPreviousEntryDate(LocalDate selectedDate) {
		List<LocalDate> entryDates = getEntryDates();
		switch (entryDates.size()) {
		case 0 -> {
			return selectedDate;
		}
		case 1 -> {
			return entryDates.get(0).isBefore(selectedDate) ? entryDates.get(0) : selectedDate;
		}
		default -> {
			LocalDate previousDate = parseForPrevious(selectedDate, entryDates);
			if (previousDate != null) {
				return previousDate;
			}
		}
		}
		// fallback is same date
		return selectedDate;
	}

	/**
	 * Determines if the provided date is a key in the journal.
	 *
	 * @param date {@link LocalDate}
	 * @return boolean true if the date is found
	 */
	public static boolean hasDate(LocalDate date) {
		return getEntryDates().contains(date);
	}

	/**
	 * OPens an existing journal in provided file with provided password.
	 *
	 * @param file     {@link org.gnome.gio.File}
	 * @param password {@link String}
	 * @throws JournalException
	 */
	public static void openJournal(org.gnome.gio.File file, String password) throws JournalException {
		try {
			openJournal(file.getPath(), password);
		} catch (IOException e) {
			throw new JournalException(e.getMessage(), e);
		}
	}

	/**
	 * Opens an existing journal at the specified file path and using the provided
	 * password.
	 *
	 * @param path     {@link String}
	 * @param password {@link String}
	 * @throws IOException      if the file is not found
	 * @throws JournalException if the password is incorrect
	 */
	public static void openJournal(String path, String password) throws IOException, JournalException {
		File file = new File(path);
		if (!file.exists()) {
			throw new IOException("File not found: " + file.getAbsolutePath());
		}
		if (!file.canWrite()) {
			throw new IOException("File is read-only: " + file.getAbsolutePath());
		}
		journal = new Journal(file, password);
		if (!journal.testPassword()) {
			throw new JournalException("Incorrect password.");
		}
	}

	/**
	 * Parse over entryDates. If selectedDate is before the first entryDate, return
	 * the entryDate. Else if there's a following entryDate, and the selectedDate is
	 * either equal to that second entryDate, or between the two entryDates, return
	 * the second entryDate.
	 *
	 * @param selectedDate {@link LocalDate}
	 * @param entryDates   {@link List} of {@link LocalDate}
	 * @return {@link LocalDate} may be null
	 */
	private static LocalDate parseForNext(LocalDate selectedDate, List<LocalDate> entryDates) {
		for (int i = 0; i < entryDates.size(); i++) {
			LocalDate entryDate1 = entryDates.get(i);
			if (selectedDate.isBefore(entryDate1)) {
				return entryDate1;
			}
			if ((i + 1) < entryDates.size()) {
				LocalDate entryDate2 = entryDates.get(i + 1);
				if (entryDate1.isBefore(selectedDate) && entryDate2.isAfter(selectedDate)) {
					return entryDate2; // next entry
				}
			}
		}
		return null;
	}

	/**
	 * Parse over entryDates backwards. If selectedDate is after the last entryDate,
	 * return the entryDate. Else, if there's a following (prior) entryDate, and the
	 * selectedDate is after that second entryDate, or between the two entryDates,
	 * return the first entryDate.
	 *
	 * @param selectedDate {@link LocalDate}
	 * @param entryDates   {@link List} of {@link LocalDate}
	 * @return {@link LocalDate} may be null
	 */
	private static LocalDate parseForPrevious(LocalDate selectedDate, List<LocalDate> entryDates) {
		for (int i = entryDates.size() - 1; i >= 0; i--) {
			LocalDate entryDate1 = entryDates.get(i);
			if (selectedDate.isAfter(entryDate1)) {
				return entryDate1;
			}
			if ((i - 1) >= 0) {
				LocalDate entryDate2 = entryDates.get(i - 1);
				if (selectedDate.isAfter(entryDate2)
						|| (entryDate2.isBefore(selectedDate) && entryDate1.isAfter(selectedDate))) {
					return entryDate2; // previous entry
				}
			}
		}
		return null;
	}

	/**
	 * Save the journal to file.
	 *
	 * @throws JournalException
	 */
	public static void saveJournal() throws JournalException {
		try {
			journal.save();
		} catch (IOException e) {
			throw new JournalException("Error saving journal.", e);
		}
	}

	/**
	 * Constructor, hidden because all methods are static.
	 */
	private JournalManager() {
	}
}
