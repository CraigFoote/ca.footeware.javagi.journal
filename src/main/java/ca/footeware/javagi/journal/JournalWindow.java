package ca.footeware.javagi.journal;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.time.LocalDate;
import java.util.List;

import org.gnome.adw.AlertDialog;
import org.gnome.adw.Application;
import org.gnome.adw.ApplicationWindow;
import org.gnome.adw.ButtonContent;
import org.gnome.adw.PasswordEntryRow;
import org.gnome.adw.Toast;
import org.gnome.adw.ToastOverlay;
import org.gnome.adw.ViewStack;
import org.gnome.adw.WindowTitle;
import org.gnome.gdk.Display;
import org.gnome.gio.File;
import org.gnome.gio.SimpleAction;
import org.gnome.glib.DateTime;
import org.gnome.glib.Type;
import org.gnome.gobject.GObject;
import org.gnome.gtk.Button;
import org.gnome.gtk.Calendar;
import org.gnome.gtk.CssProvider;
import org.gnome.gtk.FileDialog;
import org.gnome.gtk.Gtk;
import org.gnome.gtk.TextBuffer;
import org.gnome.gtk.TextIter;
import org.gnome.gtk.TextView;
import org.gnome.gtk.WrapMode;
import org.javagi.base.GErrorException;
import org.javagi.gobject.annotations.InstanceInit;
import org.javagi.gtk.annotations.GtkCallback;
import org.javagi.gtk.annotations.GtkChild;
import org.javagi.gtk.annotations.GtkTemplate;
import org.javagi.gtk.types.TemplateTypes;

import ca.footeware.javagi.journal.model.JournalException;
import ca.footeware.javagi.journal.model.JournalManager;

@GtkTemplate(name = "JournalWindow", ui = "/journal/window.ui")
public class JournalWindow extends ApplicationWindow {

	public static final Type gtype = TemplateTypes.register(JournalWindow.class);

	/**
	 * Create a new {@link JournalWindow} with provided {@link Application}.
	 *
	 * @param app {@link Application}
	 * @return {@link JournalWindow}
	 */
	public static JournalWindow create(Application app) {
		JournalWindow win = GObject.newInstance(gtype);
		win.setApplication(app);
		return win;
	}

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

	private CssProvider provider = new CssProvider();

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
	 * @param address {@link MemorySegment}
	 */
	public JournalWindow(MemorySegment address) {
		super(address);
	}

	private void createCalendarNavigationButtonActions() {
		// First action
		var firstAction = new SimpleAction("first", null);
		firstAction.onActivate(_ -> this.onFirstAction());
		super.addAction(firstAction);

		// Previous action
		var previousAction = new SimpleAction("previous", null);
		previousAction.onActivate(_ -> this.onPreviousAction());
		super.addAction(previousAction);

		// Today action
		var todayAction = new SimpleAction("today", null);
		todayAction.onActivate(_ -> this.onTodayAction());
		super.addAction(todayAction);

		// Next action
		var nextAction = new SimpleAction("next", null);
		nextAction.onActivate(_ -> this.onNextAction());
		super.addAction(nextAction);

		// Last action
		var lastAction = new SimpleAction("last", null);
		lastAction.onActivate(_ -> this.onLastAction());
		super.addAction(lastAction);
	}

	private void createEditorPageActions() {
		createCalendarNavigationButtonActions();

		// Save action
		var saveAction = new SimpleAction("save_journal", null);
		saveAction.onActivate(_ -> this.onSaveAction());
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
			this.backButton.setVisible(false);
			this.stack.setVisibleChildName("home-page");
		});
		super.addAction(backAction);

		// New Page action
		var newPageAction = new SimpleAction("new", null);
		newPageAction.onActivate(_ -> {
			this.backButton.setVisible(true);
			this.stack.setVisibleChildName("new-page");
		});
		super.addAction(newPageAction);

		// Open Page action
		var openPageAction = new SimpleAction("open", null);
		openPageAction.onActivate(_ -> {
			this.backButton.setVisible(true);
			this.stack.setVisibleChildName("open-page");
		});
		super.addAction(openPageAction);
	}

	private void displayDate(LocalDate localDate) {
		TextBuffer buffer = textView.getBuffer();
		if (JournalManager.hasDate(localDate)) {
			try {
				String entry = JournalManager.getEntry(localDate);
				buffer.setText(entry, entry.length());
			} catch (JournalException e) {
				this.notifyUser(e.getMessage());
			}
		} else {
			buffer.setText("", 0);
		}
	}

	/**
	 * Called after injection of objects annotated @GtkChild.
	 */
	@InstanceInit
	public void init() {
		// css
		this.provider.loadFromResource("/journal/styles.css");
		Gtk.styleContextAddProviderForDisplay(Display.getDefault(), provider, 500);

		// actions
		createPageNavigationActions();
		createNewJournalPageActions();
		createOpenJournalPageActions();
		createEditorPageActions();
		createCalendarNavigationButtonActions();

		// configure TextView
		textView.setWrapMode(WrapMode.WORD);
		textView.getBuffer().onModifiedChanged(() -> {
			this.textView.getBuffer().setModified(true);
			this.updateWindowTitle();
		});
	}

	private void notifyUser(String message) {
		Toast toast = new Toast(message);
		this.toaster.addToast(toast);
	}

	private void onCreateNewJournalAction() {
		String password1 = newJournalPassword1.getText();
		String password2 = newJournalPassword2.getText();
		if (this.file != null && !password1.isEmpty() && !password2.isEmpty() && password1.equals(password2)) {
			try {
				JournalManager.createJournal(this.file, password2);
				JournalManager.saveJournal();
				this.stack.setVisibleChildName("editor-page");
				this.backButton.setVisible(false);
			} catch (IOException | JournalException e) {
				this.notifyUser(e.getMessage());
			}
		}
	}

	@GtkCallback(name = "onDateSelected")
	public void onDateSelected() {
		DateTime dateTime = calendar.getDate();
		// convert
		LocalDate localDate = LocalDate.of(dateTime.getYear(), dateTime.getMonth(), dateTime.getDayOfMonth());
		if (textView.getBuffer().getModified()) {
			AlertDialog alert = new AlertDialog("Unsaved Changes", "@TODO add date to message");
			alert.addResponse("discard", "Discard");
			alert.addResponse("save", "Save");
			alert.setCloseResponse("cancel");
			alert.setDefaultResponse("save");
			alert.choose(this, null, (a, result, b) -> {
				System.err.println(a);
				System.err.println(b);
				String button = ((AlertDialog) result.getSourceObject()).chooseFinish(result);
				switch (button) {
				case "save": {
					save(localDate);
					break;
				}
				case "discard": {
					displayDate(localDate);
					break;
				}
				case "cancel": {
					// do nothing but close dialog
					break;
				}
				default:
					throw new IllegalArgumentException("Unexpected value: " + button);
				}
			});
		} else {
			displayDate(localDate);
		}
	}

	private void onExistingBrowseAction() {
		FileDialog fileDialog = new FileDialog();
		fileDialog.open(this, null, (_, result, _) -> {
			try {
				this.file = fileDialog.openFinish(result);
				this.existingJournalLocation.setLabel(file.getPath());
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

	private void onNewBrowseAction() {
		FileDialog fileDialog = new FileDialog();
		fileDialog.save(this, null, (_, result, _) -> {
			try {
				this.file = fileDialog.saveFinish(result);
				this.newJournalLocation.setLabel(file.getPath());
			} catch (GErrorException _) {
				// ignore - user closed dialog
			}
		});
	}

	private void onNextAction() {
		// TODO
	}

	private void onOpenJournalAction() {
		String password = existingJournalPassword.getText();
		if (this.file != null) {
			try {
				JournalManager.openJournal(file, password);
				this.stack.setVisibleChildName("editor-page");
				this.backButton.setVisible(false);
				List<LocalDate> entryDates = JournalManager.getEntryDates();
				LocalDate now = LocalDate.now();
				for (LocalDate localDate : entryDates) {
					if (localDate.getYear() == now.getYear() && localDate.getMonth() == now.getMonth()) {
						calendar.markDay(localDate.getDayOfMonth());
					}
				}
				this.textView.grabFocus();
				this.calendar.selectDay(DateTime.nowLocal());
				this.textView.getBuffer().setModified(false);
				this.displayDate(LocalDate.now());
				this.updateWindowTitle();
			} catch (JournalException e) {
				this.notifyUser(e.getMessage());
			}
		}
	}

	private void onPreviousAction() {
		// TODO
	}

	private void onSaveAction() {
		TextBuffer buffer = this.textView.getBuffer();
		TextIter startIter = new TextIter();
		TextIter endIter = new TextIter();
		buffer.getStartIter(startIter);
		buffer.getEndIter(endIter);
		String text = buffer.getText(startIter, endIter, true);
		DateTime dateTime = this.calendar.getDate();
		LocalDate localDate = LocalDate.of(dateTime.getYear(), dateTime.getMonth(), dateTime.getDayOfMonth());
		try {
			JournalManager.addEntry(localDate, text);
			JournalManager.saveJournal();
			this.textView.getBuffer().setModified(false);
			this.updateWindowTitle();
			this.notifyUser("Journal was saved.");
		} catch (JournalException e) {
			this.notifyUser(e.getMessage());
		}
	}

	private void onTodayAction() {
		this.calendar.selectDay(DateTime.nowLocal());
	}

	private void save(LocalDate date) {
		TextIter startIter = new TextIter();
		TextIter endIter = new TextIter();
		String text = textView.getBuffer().getText(startIter, endIter, true);
		try {
			JournalManager.addEntry(date, text);
			JournalManager.saveJournal();
		} catch (JournalException e) {
			notifyUser(e.getMessage());
		}
	}

	/**
	 * Updates the window subtitle to a modified-indicator and the current filename.
	 * When no file is open, the subtitle is "An encrypted daily journal".
	 */
	private void updateWindowTitle() {
		this.windowTitle.setTitle((this.textView.getBuffer().getModified() ? "• " : "") + "Journal");
		this.windowTitle.setSubtitle(this.file == null ? "An encrypted daily journal" : this.file.getPath());
	}

}
