package ca.footeware.javagi.journal;

import java.io.IOException;

import org.gnome.gio.Resource;
import org.javagi.base.GErrorException;

public class Main {

	public static void main(String[] args) throws GErrorException, IOException {
		byte[] bytes = Main.class.getResourceAsStream("/journal.gresource").readAllBytes();
		var resources = Resource.fromData(bytes);
		resources.resourcesRegister();
		JournalApplication.create().run(args);
	}
}
