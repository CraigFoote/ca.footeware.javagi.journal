package ca.footeware.javagi.journal;

import java.lang.foreign.MemorySegment;

import org.gnome.adw.Application;
import org.gnome.adw.ApplicationWindow;
import org.gnome.adw.ViewStack;
import org.gnome.adw.WindowTitle;
import org.gnome.gio.SimpleAction;
import org.gnome.glib.Type;
import org.gnome.gobject.GObject;
import org.gnome.gtk.Button;
import org.javagi.gobject.annotations.InstanceInit;
import org.javagi.gtk.annotations.GtkChild;
import org.javagi.gtk.annotations.GtkTemplate;
import org.javagi.gtk.types.TemplateTypes;

@GtkTemplate(name = "JournalWindow", ui = "/journal/window.ui")
public class JournalWindow extends ApplicationWindow {

	public static final Type gtype = TemplateTypes.register(JournalWindow.class);
	private static Application app;

	@GtkChild(name = "window_title")
	public WindowTitle windowTitle;

	@GtkChild(name = "stack")
	public ViewStack stack;

	@GtkChild(name = "back_button")
	public Button backButton;

	public static JournalWindow create(Application app) {
		JournalWindow.app = app;
		JournalWindow win = GObject.newInstance(gtype);
		win.setApplication(app);
		return win;
	}

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
	}

}
