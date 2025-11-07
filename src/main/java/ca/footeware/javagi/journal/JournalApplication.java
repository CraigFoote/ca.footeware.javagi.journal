package ca.footeware.javagi.journal;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.util.Properties;

import org.gnome.adw.AboutDialog;
import org.gnome.adw.Application;
import org.gnome.gdk.Display;
import org.gnome.gio.SimpleAction;
import org.gnome.glib.Type;
import org.gnome.glib.Variant;
import org.gnome.gobject.GObject;
import org.gnome.gtk.GtkBuilder;
import org.gnome.gtk.IconTheme;
import org.gnome.gtk.License;
import org.gnome.gtk.Window;
import org.javagi.base.GErrorException;
import org.javagi.gobject.annotations.InstanceInit;
import org.javagi.gobject.annotations.RegisteredType;
import org.javagi.gtk.types.TemplateTypes;

@RegisteredType(name = "JournalApplication")
public class JournalApplication extends Application {

	public static final Type gtype = TemplateTypes.register(JournalApplication.class);
	public static JournalApplication create() {
		JournalApplication app = GObject.newInstance(gtype);
		app.setApplicationId("ca.footeware.javagi.journal");
		app.onActivate(app::activate);
		return app;
	}

	private GtkBuilder builder;

	public JournalApplication(MemorySegment address) {
		super(address);
	}

	@Override
	public void activate() {
		var display = Display.getDefault();
		var iconTheme = IconTheme.getForDisplay(display);
		iconTheme.addResourcePath("/journal");

		var win = this.getActiveWindow();
		if (win == null) {
			win = JournalWindow.create(this);
		}
		win.present();
	}

	@InstanceInit
	public void init() {
		var aboutAction = new SimpleAction("about", null);
		aboutAction.onActivate(this::onAboutAction);
		addAction(aboutAction);

		var shortcutsAction = new SimpleAction("show-help-overlay", null);
		shortcutsAction.onActivate(this::onShortcutsAction);
		setAccelsForAction("app.show-help-overlay", new String[] { "<ctrl>question" });
		addAction(shortcutsAction);

		builder = new GtkBuilder();
	}

	// @formatter:off
	private void onAboutAction(Variant parameter) {
		String version = "unknown";
		Properties properties = new Properties();
		try {
			InputStream stream = this.getClass().getResourceAsStream("/project.properties");
			properties.load(stream);
			version = (String) properties.get("version");
		} catch (IOException e) {
			e.printStackTrace();
			// ignore and use initial value
		}
        var about = AboutDialog.builder()
            .setApplicationName("Journal")
            .setApplicationIcon("journal")
            .setDeveloperName("Another fine mess by Footeware.ca")
            .setDevelopers(new String[]{"Craig Foote"})
            .setVersion(version)
            .setWebsite("https://github.com/CraigFoote/ca.footeware.javagi.journal")
            .setIssueUrl("https://github.com/CraigFoote/ca.footeware.javagi.journal/issues")
            .setCopyright("©2025 Craig Foote")
            .setLicenseType(License.GPL_3_0)
            .build();
        about.present(this.getActiveWindow());
 	}
 	// @formatter:on

	private void onShortcutsAction(Variant variant1) {
		try {
			builder.addFromResource("/journal/help_overlay.ui");
			((Window) builder.getObject("help_overlay")).setVisible(true);
		} catch (GErrorException e) {
			e.printStackTrace();
		}
	}
}