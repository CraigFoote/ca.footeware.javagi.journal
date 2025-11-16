package ca.footeware.javagi.journal;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.gnome.adw.AlertDialog;
import org.gnome.adw.Application;
import org.gnome.adw.ApplicationWindow;
import org.gnome.adw.ButtonContent;
import org.gnome.adw.PasswordEntryRow;
import org.gnome.adw.ResponseAppearance;
import org.gnome.adw.Toast;
import org.gnome.adw.ToastOverlay;
import org.gnome.adw.ViewStack;
import org.gnome.adw.WindowTitle;
import org.gnome.gdk.Display;
import org.gnome.gio.File;
import org.gnome.gio.SimpleAction;
import org.gnome.glib.DateTime;
import org.gnome.glib.TimeZone;
import org.gnome.gtk.Button;
import org.gnome.gtk.Calendar;
import org.gnome.gtk.CssProvider;
import org.gnome.gtk.FileDialog;
import org.gnome.gtk.Gtk;
import org.gnome.gtk.TextBuffer;
import org.gnome.gtk.TextBufferCommitNotify;
import org.gnome.gtk.TextBufferNotifyFlags;
import org.gnome.gtk.TextIter;
import org.gnome.gtk.TextView;
import org.gnome.gtk.WrapMode;
import org.javagi.base.GErrorException;
import org.javagi.gobject.annotations.InstanceInit;
import org.javagi.gtk.annotations.GtkCallback;
import org.javagi.gtk.annotations.GtkChild;
import org.javagi.gtk.annotations.GtkTemplate;

import ca.footeware.javagi.journal.model.JournalException;
import ca.footeware.javagi.journal.model.JournalManager;

@GtkTemplate(name = "JournalWindow", ui = "/journal/window.ui")
public class JournalWindow extends ApplicationWindow {

	private static final String CANCEL = "cancel";
	private static final String DISCARD = "discard";
	private static final String SAVE = "save";

	@GtkChild(name = "back_button")
	public Button backButton;

	@GtkChild(name = "calendar")
	public Calendar calendar;

	@GtkChild(name = "existing_journal_location")
	public ButtonContent existingJournalLocation;

	@GtkChild(name = "existing_journal_password")
	public PasswordEntryRow existingJournalPassword;

	private File file = null;

	@GtkChild(name = "new_journal_location")
	public ButtonContent newJournalLocation;

	@GtkChild(name = "new_journal_password_1")
	public PasswordEntryRow newJournalPassword1;

	@GtkChild(name = "new_journal_password_2")
	public PasswordEntryRow newJournalPassword2;

	private LocalDate previousDate = null;

	private String previousText = null;

	@GtkChild(name = "stack")
	public ViewStack stack;

	@GtkChild(name = "textview")
	public TextView textView;

	@GtkChild(name = "toaster")
	public ToastOverlay toaster;

	@GtkChild(name = "window_title")
	public WindowTitle windowTitle;

	/**
	 * Constructor.
	 *
	 * @param application {@link Application}
	 */
	public JournalWindow(Application application) {
		setApplication(application);
		present();
	}

	private LocalDate convert(DateTime date) {
		return LocalDate.of(date.getYear(), date.getMonth(), date.getDayOfMonth());
	}

	private DateTime convert(LocalDate localDate) {
		return new DateTime(TimeZone.local(), localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth(),
				0, 0, 0);
	}

	private void createCalendarNavigationActions() {
		// First action
		var firstAction = new SimpleAction("first", null);
		firstAction.onActivate(_ -> onFirstAction());
		super.addAction(firstAction);

		// Previous action
		var previousAction = new SimpleAction("previous", null);
		previousAction.onActivate(_ -> onPreviousAction());
		super.addAction(previousAction);

		// Today action
		var todayAction = new SimpleAction("today", null);
		todayAction.onActivate(_ -> onTodayAction());
		super.addAction(todayAction);

		// Next action
		var nextAction = new SimpleAction("next", null);
		nextAction.onActivate(_ -> onNextAction());
		super.addAction(nextAction);

		// Last action
		var lastAction = new SimpleAction("last", null);
		lastAction.onActivate(_ -> onLastAction());
		super.addAction(lastAction);
	}

	private void createEditorPageActions() {
		createCalendarNavigationActions();

		// Save action
		var saveAction = new SimpleAction("save_journal", null);
		saveAction.onActivate(_ -> onSaveAction());
		super.addAction(saveAction);
	}

	private void createNewJournalPageActions() {
		// New -> Browse action
		var newBrowseAction = new SimpleAction("new_browse_for_folder", null);
		newBrowseAction.onActivate(_ -> onNewBrowseAction());
		super.addAction(newBrowseAction);

		// Create New Journal action
		var createNewJournalAction = new SimpleAction("create_journal", null);
		createNewJournalAction.onActivate(_ -> onCreateNewJournalAction());
		super.addAction(createNewJournalAction);
	}

	private void createOpenJournalPageActions() {
		// Browse for existing journal action
		var existingBrowseAction = new SimpleAction("open_browse_for_journal", null);
		existingBrowseAction.onActivate(_ -> onExistingBrowseAction());
		super.addAction(existingBrowseAction);

		// Open journal action
		var openJournalAction = new SimpleAction("open_journal", null);
		openJournalAction.onActivate(_ -> onOpenJournalAction());
		super.addAction(openJournalAction);
	}

	private void createPageNavigationActions() {
		// Back action
		var backAction = new SimpleAction("back", null);
		backAction.onActivate(_ -> {
			backButton.setVisible(false);
			stack.setVisibleChildName("home-page");
		});
		super.addAction(backAction);

		// New Page action
		var newPageAction = new SimpleAction("new", null);
		newPageAction.onActivate(_ -> {
			backButton.setVisible(true);
			stack.setVisibleChildName("new-page");
		});
		super.addAction(newPageAction);

		// Open Page action
		var openPageAction = new SimpleAction("open", null);
		openPageAction.onActivate(_ -> {
			backButton.setVisible(true);
			stack.setVisibleChildName("open-page");
		});
		super.addAction(openPageAction);
	}

	private void displayDateEntry(LocalDate date) {
		if (isDirty()) {
			promptToSavePrevious(date);
		} else {
			TextBuffer buffer = textView.getBuffer();
			if (JournalManager.hasDate(date)) {
				try {
					String entry = JournalManager.getEntry(date);
					buffer.setText(entry, entry.length());
				} catch (JournalException e) {
					notifyUser(e.getMessage());
				}
			} else {
				buffer.setText("", 0);
			}
			setDirtyTitle(false);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj) || (getClass() != obj.getClass())) {
			return false;
		}
		JournalWindow other = (JournalWindow) obj;
		return Objects.equals(stack, other.stack);
	}

	private String getText() {
		TextIter startIter = new TextIter();
		TextIter endIter = new TextIter();
		textView.getBuffer().getBounds(startIter, endIter);
		return textView.getBuffer().getText(startIter, endIter, true);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(stack);
		return result;
	}

	/**
	 * Called after injection of objects annotated @GtkChild.
	 */
	@InstanceInit
	public void init() {
		// css
		CssProvider cssProvider = new CssProvider();
		cssProvider.loadFromResource("/journal/styles.css");
		Gtk.styleContextAddProviderForDisplay(Display.getDefault(), cssProvider,
				Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION);

		// actions
		createPageNavigationActions();
		createNewJournalPageActions();
		createOpenJournalPageActions();
		createEditorPageActions();
		createCalendarNavigationActions();

		// configure TextView
		textView.setWrapMode(WrapMode.WORD);
		textView.getBuffer().onChanged(this::onBufferChanged);
		textView.getBuffer().addCommitNotify(TextBufferNotifyFlags.BEFORE_INSERT, new TextBufferCommitNotify() {
			@Override
			public void run(TextBuffer buffer, Set<TextBufferNotifyFlags> flags, int position, int length) {
				previousDate = convert(calendar.getDate());
			}
		});
	}

	/**
	 * Determines if the text buffer contents have changed since last save and also
	 * checks that the window title is flagged as changed.
	 *
	 * @return boolean true if editor has unsaved edits and the title indicates
	 *         dirty state
	 */
	private boolean isDirty() {
		return windowTitle.getTitle().startsWith("•") && textView.getBuffer().getModified();
	}

	private void markEntryDays() {
		List<LocalDate> entryDates = JournalManager.getEntryDates();
		LocalDate now = LocalDate.now();
		for (LocalDate localDate : entryDates) {
			if (localDate.getYear() == now.getYear() && localDate.getMonth() == now.getMonth()) {
				calendar.markDay(localDate.getDayOfMonth());
			}
		}
	}

	private void notifyUser(String message) {
		toaster.addToast(new Toast(message));
	}

	private void onBufferChanged() {
		setDirtyTitle(true);
		previousText = getText();
	}

	private void onCreateNewJournalAction() {
		String password1 = newJournalPassword1.getText();
		String password2 = newJournalPassword2.getText();
		if (file != null && !password1.isEmpty() && !password2.isEmpty() && password1.equals(password2)) {
			try {
				JournalManager.createJournal(file, password2);
				JournalManager.saveJournal();
				stack.setVisibleChildName("editor-page");
				backButton.setVisible(false);
				windowTitle.setSubtitle(file.getPath());
			} catch (IOException | JournalException e) {
				notifyUser(e.getMessage());
			}
		}
	}

	/**
	 * Called when a date is selected in the calendar.
	 */
	@GtkCallback(name = "onDateSelected")
	public void onDateSelected() {
		displayDateEntry(convert(calendar.getDate()));
		setDirtyTitle(false);
		textView.getBuffer().setModified(false);
	}

	/**
	 * Prompts the user to browse local file system to choose the name and location
	 * of an existing journal file.
	 */
	private void onExistingBrowseAction() {
		FileDialog fileDialog = new FileDialog();
		fileDialog.open(this, null, (_, result, _) -> {
			try {
				file = fileDialog.openFinish(result);
				existingJournalLocation.setLabel(file.getPath());
				existingJournalPassword.grabFocus();
			} catch (GErrorException _) {
				// ignore - user closed dialog
			}
		});
	}

	private void onFirstAction() {
		// TODO
	}

	private void onLastAction() {
		// TODO
	}

	/**
	 * Prompts the user to browse local file system to choose a name and location
	 * for a new journal file.
	 */
	private void onNewBrowseAction() {
		FileDialog fileDialog = new FileDialog();
		fileDialog.save(this, null, (_, result, _) -> {
			try {
				file = fileDialog.saveFinish(result);
				newJournalLocation.setLabel(file.getPath());
				newJournalPassword1.grabFocus();
			} catch (GErrorException _) {
				// ignore - user closed dialog
			}
		});
	}

	private void onNextAction() {
		// TODO
	}

	/**
	 * Called when a journal is first opened. Shows the Editor page, hides the Back
	 * button, draws calendar and selects today's date.
	 */
	private void onOpenJournalAction() {
		String password = existingJournalPassword.getText();
		if (file != null) {
			try {
				JournalManager.openJournal(file, password);
				stack.setVisibleChildName("editor-page");
				backButton.setVisible(false);
				markEntryDays();
				textView.grabFocus();
				calendar.selectDay(DateTime.nowLocal());
				displayDateEntry(LocalDate.now());
				windowTitle.setSubtitle(file.getPath());
				// clear above indication of change
				textView.getBuffer().setModified(false);
				setDirtyTitle(false);
			} catch (JournalException e) {
				notifyUser(e.getMessage());
			}
		}
	}

	private void onPreviousAction() {
		// TODO
	}

	/**
	 * Save button handler.
	 */
	private void onSaveAction() {
		LocalDate localDate = convert(calendar.getDate());
		save(localDate, getText());
	}

	/**
	 * Selects today's date in the calendar.
	 */
	private void onTodayAction() {
		calendar.selectDay(DateTime.nowLocal());
	}

	/**
	 * Prompts the user to save unsaved modifications to the text buffer.
	 *
	 * @param newDate {@link LocalDate}
	 */
	private void promptToSavePrevious(LocalDate newDate) {
		AlertDialog alert = new AlertDialog("Unsaved Changes",
				"Do you want to save your edits to " + previousDate + "?");
		alert.addResponse(DISCARD, "Discard");
		alert.addResponse(SAVE, "Save");
		alert.setCloseResponse(CANCEL);
		alert.setResponseAppearance(SAVE, ResponseAppearance.SUGGESTED);
		alert.setResponseAppearance(DISCARD, ResponseAppearance.DESTRUCTIVE);
		alert.setDefaultResponse(SAVE);
		alert.choose(this, null, (_, result, _) -> {
			String button = alert.chooseFinish(result);
			switch (button) {
			case "save": {
				// save previous date's text
				save(previousDate, previousText);
				// display previous date
				calendar.selectDay(convert(previousDate));
				setDirtyTitle(false);
				break;
			}
			case DISCARD: {
				setDirtyTitle(false);
				textView.getBuffer().setModified(false);
				// display new date
				displayDateEntry(newDate);
				break;
			}
			case CANCEL: {
				// store displayed text
				String text = getText();
				// go back to date selected before the alert
				calendar.selectDay(convert(previousDate));
				// put the text back in the editor
				textView.getBuffer().setText(text, text.length());
				// simulate the state before the click that caused the alert
				setDirtyTitle(true);
				break;
			}
			default:
				throw new IllegalArgumentException("Unexpected value: " + button);
			}
		});
	}

	/**
	 * Adds the current date and text as a new journal entry, writes it to disk and
	 * updates the buffer modified flag and the window title.
	 *
	 * @param date {@link LocalDate}
	 * @param text {@link String}
	 */
	private void save(LocalDate date, String text) {
		try {
			JournalManager.addEntry(date, text);
			JournalManager.saveJournal();
			setDirtyTitle(false);
			notifyUser("Journal was saved.");
		} catch (JournalException e) {
			notifyUser(e.getMessage());
		}
	}

	private void setDirtyTitle(boolean dirty) {
		windowTitle.setTitle(dirty ? "• Journal" : "Journal");
	}

	@Override
	public String toString() {
		return "JournalWindow [file=" + file + "]";
	}
}