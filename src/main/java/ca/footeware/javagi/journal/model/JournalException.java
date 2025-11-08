package ca.footeware.javagi.journal.model;

/**
 * Used to indicate a problem with the {@link Journal}.
 */
public class JournalException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message {@link String}
	 */
	public JournalException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 *
	 * @param message   {@link String}
	 * @param throwable {@link Throwable}
	 */
	public JournalException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
