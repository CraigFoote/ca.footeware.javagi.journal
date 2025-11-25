# Journal

### An encrypted daily journal for flatpak.

Written in Java using [Java-GI](https://https://java-gi.org/) Gtk/Adw bindings, version 0.13.0, and packaged as a flatpak. It uses the [flatpak-maven-plugin](https://github.com/CraigFoote/flatpak-maven-plugin) to create flatpak artifacts. Have a look at this project's `pom.xml` configuration for details.

The code is compiled with Java 25 (the minimum for Java-GI is 22) and is packaged in a flatpak container with runtimes *org.gnome.Platform* 49, *org.gnome.Sdk* 49, and *org.freedesktop.Sdk.Extension.openjdk25* that includes the openjdk-25 JRE that runs the Journal application.

The encryption-by-password algorithm should provide privacy. The files used can be named anything and is just a plain text Java Properties file, e.g.:

```properties
#Wed Nov 05 15:24:00 EST 2025
2025-09-20=G6b9lzbW159Iz4ECDIv6PuJpLQQ2PnJMgQh5y7MCAfzetPZfqKOfb4c/1WLhyNhwsw\=\=
2025-09-24=JblFzDbfBE5DyGKjyBQ4PPCFOePILMaWYevYH2TF9o4+bxAATFdYgUnhAEpBfsCdCA\=\=
```

As you can see, the keys are the entry dates and the entries are encrypted text. Journal decrypts the contents for display when the entry date is clicked in its calendar.

## Prerequisites

You'll need [flatpak](https://flathub.org/setup) installed to install and run the .flatpak file once there's a [release](https://github.com/CraigFoote/ca.footeware.javagi.journal/releases).

### flatpak-maven-plugin

The [flatpak-maven-plugin](https://github.com/CraigFoote/flatpak-maven-plugin) only exists as a snapshot yet because it's nowhere near production ready. For now the project will need to be checked out, built and installed to local `.m2` folder.

### Journal Releases

I haven't created a release of the Journal yet because I'm still working on core features. Once I do, you'll find a .flatpak file in the [Releases](https://github.com/CraigFoote/ca.footeware.javagi.journal/releases) page. Downloading and double-clicking it should open it in the GNOME Software application allowing installation.

Until then, or if you want to build it yourself, you'll need Java JDK 25 installed. And maven and git, either installed in the eclipse IDE or as separate installs. Then you can clone this repository and build it locally. 

Along the way you may get some errors about missing flatpak runtimes. These can be fixed by installing them via, e.g.:

`flatpak install flathub org.gnome.Platform`

## Building

If you're using eclipse, use the `javagi.journal-BUILD.launch` run configuration. Or run at the project root:

```bash
mvn clean package
```

This populates the *target* folder, including an *app* folder with artifacts needed for a flatpak-builder call. The *app* folder should look like this:

```bash
❯ tree ./app
app/
├── ca.footeware.javagi.journal.desktop
├── ca.footeware.javagi.journal.metainfo.xml
├── ca.footeware.javagi.journal.png
├── ca.footeware.javagi.journal.svg
├── ca.footeware.javagi.journal.yml
├── journal
├── journal-0.0.1-SNAPSHOT.jar
├── screenshot1.png
└── screenshot2.png
```

The jar is a regular Java jar without embedded dependencies but we're building it into a flatpak installer that does contain the dependencies. From the *app* folder, run the following to build the flatpak and install it locally.

```
flatpak-builder --force-clean --user --verbose --install build-dir ca.footeware.javagi.journal.yml
```

If you get errors, running this can provide better explanations. Some *pom.xml* tags are used but I think I've got the required ones.

```
flatpak run --command=flatpak-builder-lint org.flatpak.Builder appstream ca.footeware.javagi.journal.metainfo.xml
```

## Running

To run the installed flatpak, use:

```bash
flatpak run ca.footeware.javagi.journal
```

Or use the launcher that should now be in your apps view. Or use Warehouse.

## Export to .flatpak file

This can be double-clicked to install in GNOME Software.

From the *app* folder again:

1. `flatpak-builder --repo=repo --force-clean build-dir ca.footeware.javagi.journal.yml`
2. `flatpak build-bundle repo ca.footeware.javagi.journal.flatpak ca.footeware.javagi.journal`

The last command took over 2 minutes on my laptop (!) With no progress or debug information but it did eventually return. The build result is a 60MB flatpak file that installs to 256MB. I guess the bundled JRE and dependent jars add up 🧐.

This creates a subfolder of *target/app* called *build-dir* with build resources but you'll find the **ca.footeware.javagi.journal.flatpak** in the *target/app* folder. Double-click it to open and install in GNOME Software.

## Debugging

[Warehouse](https://flathub.org/apps/io.github.flattool.Warehouse) is a great program to manage flatpaks, including verifying installation, running and removal.

Note that the `flatpak-builder --force-clean --user --verbose --install build-dir ca.footeware.javagi.journal.yml` command as described above installs the app with *user* scope while installing the .flatpak build through GNONE Software will install in *system* scope. From what I can tell, the system scope is preferred meaning the user scope probably should just be used for testing as it does not require root privileges.

A couple commands I've found that might help:

- Source verification: `flatpak run --command=flatpak-builder-lint org.flatpak.Builder appstream ca.footeware.javagi.journal.metainfo.xml`
- Container info: `flatpak info ca.footeware.javagi.journal`
- Attach to running container: `flatpak run --command=sh ca.footeware.javagi.journal`. Once connected, `cd /app` to see your files. You can check the version of java installed in `/app/jre`.

## Removing

To remove this fine piece of work, use Warehouse or run:

```
flatpak uninstall --delete-data ca.footeware.javagi.journal
```

## TODO

- Code tweaks.
- Integrate `flatpak-builder` and `flatpak build-bundle` calls into maven build.
- Release *ca.footeware.javagi.journal.flatpak* to [Flathub](https://flathub.org) for easy distribution and installation on clients.
- Release *flatpak-maven-plugin* to maven central.
---
