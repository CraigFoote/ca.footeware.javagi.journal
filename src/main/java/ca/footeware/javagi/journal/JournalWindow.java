package ca.footeware.javagi.journal;

import java.io.IOException;
import java.lang.foreign.MemorySegment;

import org.gnome.adw.Application;
import org.gnome.adw.ApplicationWindow;
import org.gnome.adw.ButtonContent;
import org.gnome.adw.PasswordEntryRow;
import org.gnome.adw.ViewStack;
import org.gnome.adw.WindowTitle;
import org.gnome.gio.File;
import org.gnome.gio.SimpleAction;
import org.gnome.glib.Type;
import org.gnome.gobject.GObject;
import org.gnome.gtk.Button;
import org.gnome.gtk.FileDialog;
import org.javagi.base.GErrorException;
import org.javagi.gobject.annotations.InstanceInit;
import org.javagi.gtk.annotations.GtkChild;
import org.javagi.gtk.annotations.GtkTemplate;
import org.javagi.gtk.types.TemplateTypes;

import ca.footeware.javagi.journal.model.JournalException;
import ca.footeware.javagi.journal.model.JournalManager;

@GtkTemplate(name = "JournalWindow", ui = "/journal/window.ui")
public class JournalWindow extends ApplicationWindow {

	private static Application app;
	private File file = null;
	public static final Type gtype = TemplateTypes.register(JournalWindow.class);

	public static JournalWindow create(Application app) {
		JournalWindow.app = app;
		JournalWindow win = GObject.newInstance(gtype);
		win.setApplication(app);
		return win;
	}

	@GtkChild(name = "back_button")
	public Button backButton;

	@GtkChild(name = "new_journal_location")
	public ButtonContent newJournalLocationButton;

	@GtkChild(name = "new_journal_password_1")
	public PasswordEntryRow newJournalPassword1;

	@GtkChild(name = "new_journal_password_2")
	public PasswordEntryRow newJournalPassword2;

	@GtkChild(name = "stack")
	public ViewStack stack;

	@GtkChild(name = "window_title")
	public WindowTitle windowTitle;

	public JournalWindow(MemorySegment address) {
		super(address);
	}

	@InstanceInit
	public void init() {
		// Back action
		var backAction = new SimpleAction("back", null);
		backAction.onActivate((SimpleAction.ActivateCallback) _ -> {
			backButton.setVisible(false);
			stack.setVisibleChildName("home-page");
		});
		addAction(backAction);

		// New action
		var newAction = new SimpleAction("new", null);
		newAction.onActivate((SimpleAction.ActivateCallback) _ -> {
			backButton.setVisible(true);
			stack.setVisibleChildName("new-page");
		});
		addAction(newAction);

		// Open action
		var openAction = new SimpleAction("open", null);
		openAction.onActivate((SimpleAction.ActivateCallback) _ -> {
			backButton.setVisible(true);
			stack.setVisibleChildName("open-page");
		});
		addAction(openAction);

		// New -> Browse action
		var newBrowseAction = new SimpleAction("new_browse_for_folder", null);
		newBrowseAction.onActivate((SimpleAction.ActivateCallback) _ -> {
			// prompt for folder
			FileDialog fileDialog = new FileDialog();
			fileDialog.save(this, null, (_, result, _) -> {
				try {
					this.file = fileDialog.saveFinish(result);
					newJournalLocationButton.setLabel(file.getPath());
				} catch (GErrorException _) {
					// ignore - user closed dialog
				}
			});
		});
		addAction(newBrowseAction);

		// Create New Journal action
		var createNewJournalAction = new SimpleAction("create_journal", null);
		createNewJournalAction.onActivate((SimpleAction.ActivateCallback) _ -> {
			String password1 = newJournalPassword1.getText();
			String password2 = newJournalPassword2.getText();
			if (this.file != null && !password1.isEmpty() && !password2.isEmpty() && password1.equals(password2)) {
				try {
					JournalManager.createNewJournal(this.file, password2);
					JournalManager.saveJournal();
					stack.setVisibleChildName("editor-page");
					backButton.actionSetEnabled("win.create_journal", false);
				} catch (IOException | JournalException e) {
					// TODO notify user
					e.printStackTrace();
				}
			}
		});
		addAction(createNewJournalAction);
	}

}
