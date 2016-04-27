/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.springframework.boot.loader.tools.JarWriter.EntryTransformer;
import org.springframework.lang.UsesJava8;

/**
 * Utility class that can be used to repackage an archive so that it can be executed using
 * '{@literal java -jar}'.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
public class Repackager {

	private static final String MAIN_CLASS_ATTRIBUTE = "Main-Class";

	private static final String START_CLASS_ATTRIBUTE = "Start-Class";

	private static final String BOOT_VERSION_ATTRIBUTE = "Spring-Boot-Version";

	private static final String BOOT_LIB_ATTRIBUTE = "Spring-Boot-Lib";

	private static final String BOOT_CLASSES_ATTRIBUTE = "Spring-Boot-Classes";

	private static final byte[] ZIP_FILE_HEADER = new byte[] { 'P', 'K', 3, 4 };

	private String mainClass;

	private boolean backupSource = true;

	private final File source;

	private Layout layout;

	public Repackager(File source) {
		if (source == null || !source.exists() || !source.isFile()) {
			throw new IllegalArgumentException("Source must refer to an existing file");
		}
		this.source = source.getAbsoluteFile();
		this.layout = Layouts.forFile(source);
	}

	/**
	 * Sets the main class that should be run. If not specified the value from the
	 * MANIFEST will be used, or if no manifest entry is found the archive will be
	 * searched for a suitable class.
	 * @param mainClass the main class name
	 */
	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	/**
	 * Sets if source files should be backed up when they would be overwritten.
	 * @param backupSource if source files should be backed up
	 */
	public void setBackupSource(boolean backupSource) {
		this.backupSource = backupSource;
	}

	/**
	 * Sets the layout to use for the jar. Defaults to {@link Layouts#forFile(File)}.
	 * @param layout the layout
	 */
	public void setLayout(Layout layout) {
		if (layout == null) {
			throw new IllegalArgumentException("Layout must not be null");
		}
		this.layout = layout;
	}

	/**
	 * Repackage the source file so that it can be run using '{@literal java -jar}'.
	 * @param libraries the libraries required to run the archive
	 * @throws IOException if the file cannot be repackaged
	 */
	public void repackage(Libraries libraries) throws IOException {
		repackage(this.source, libraries);
	}

	/**
	 * Repackage to the given destination so that it can be launched using '
	 * {@literal java -jar}'.
	 * @param destination the destination file (may be the same as the source)
	 * @param libraries the libraries required to run the archive
	 * @throws IOException if the file cannot be repackaged
	 */
	public void repackage(File destination, Libraries libraries) throws IOException {
		repackage(destination, libraries, null);
	}

	/**
	 * Repackage to the given destination so that it can be launched using '
	 * {@literal java -jar}'.
	 * @param destination the destination file (may be the same as the source)
	 * @param libraries the libraries required to run the archive
	 * @param launchScript an optional launch script prepended to the front of the jar
	 * @throws IOException if the file cannot be repackaged
	 * @since 1.3.0
	 */
	public void repackage(File destination, Libraries libraries,
			LaunchScript launchScript) throws IOException {
		if (destination == null || destination.isDirectory()) {
			throw new IllegalArgumentException("Invalid destination");
		}
		if (libraries == null) {
			throw new IllegalArgumentException("Libraries must not be null");
		}
		if (alreadyRepackaged()) {
			return;
		}
		destination = destination.getAbsoluteFile();
		File workingSource = this.source;
		if (this.source.equals(destination)) {
			workingSource = getBackupFile();
			workingSource.delete();
			renameFile(this.source, workingSource);
		}
		destination.delete();
		try {
			JarFile jarFileSource = new JarFile(workingSource);
			try {
				repackage(jarFileSource, destination, libraries, launchScript);
			}
			finally {
				jarFileSource.close();
			}
		}
		finally {
			if (!this.backupSource && !this.source.equals(workingSource)) {
				deleteFile(workingSource);
			}
		}
	}

	/**
	 * Return the {@link File} to use to backup the original source.
	 * @return the file to use to backup the original source
	 */
	public final File getBackupFile() {
		return new File(this.source.getParentFile(), this.source.getName() + ".original");
	}

	private boolean alreadyRepackaged() throws IOException {
		JarFile jarFile = new JarFile(this.source);
		try {
			Manifest manifest = jarFile.getManifest();
			return (manifest != null && manifest.getMainAttributes()
					.getValue(BOOT_VERSION_ATTRIBUTE) != null);
		}
		finally {
			jarFile.close();
		}
	}

	private void repackage(JarFile sourceJar, File destination, Libraries libraries,
			LaunchScript launchScript) throws IOException {
		JarWriter writer = new JarWriter(destination, launchScript);
		try {
			final List<Library> unpackLibraries = new ArrayList<Library>();
			final List<Library> standardLibraries = new ArrayList<Library>();
			libraries.doWithLibraries(new LibraryCallback() {

				@Override
				public void library(Library library) throws IOException {
					File file = library.getFile();
					if (isZip(file)) {
						if (library.isUnpackRequired()) {
							unpackLibraries.add(library);
						}
						else {
							standardLibraries.add(library);
						}
					}
				}

			});
			writer.writeManifest(buildManifest(sourceJar));
			Set<String> seen = new HashSet<String>();
			writeNestedLibraries(unpackLibraries, seen, writer);
			if (this.layout instanceof RepackagingLayout) {
				writer.writeEntries(sourceJar,
						new RenamingEntryTransformer(((RepackagingLayout) this.layout)
								.getRepackagedClassesLocation()));
			}
			else {
				writer.writeEntries(sourceJar);
			}
			writeNestedLibraries(standardLibraries, seen, writer);
			if (this.layout.isExecutable()) {
				writer.writeLoaderClasses();
			}
		}
		finally {
			try {
				writer.close();
			}
			catch (Exception ex) {
				// Ignore
			}
		}
	}

	private void writeNestedLibraries(List<Library> libraries, Set<String> alreadySeen,
			JarWriter writer) throws IOException {
		for (Library library : libraries) {
			String destination = Repackager.this.layout
					.getLibraryDestination(library.getName(), library.getScope());
			if (destination != null) {
				if (!alreadySeen.add(destination + library.getName())) {
					throw new IllegalStateException(
							"Duplicate library " + library.getName());
				}
				writer.writeNestedLibrary(destination, library);
			}
		}
	}

	private boolean isZip(File file) {
		try {
			FileInputStream fileInputStream = new FileInputStream(file);
			try {
				return isZip(fileInputStream);
			}
			finally {
				fileInputStream.close();
			}
		}
		catch (IOException ex) {
			return false;
		}
	}

	private boolean isZip(InputStream inputStream) throws IOException {
		for (int i = 0; i < ZIP_FILE_HEADER.length; i++) {
			if (inputStream.read() != ZIP_FILE_HEADER[i]) {
				return false;
			}
		}
		return true;
	}

	private Manifest buildManifest(JarFile source) throws IOException {
		Manifest manifest = source.getManifest();
		if (manifest == null) {
			manifest = new Manifest();
			manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
		}
		manifest = new Manifest(manifest);
		String startClass = this.mainClass;
		if (startClass == null) {
			startClass = manifest.getMainAttributes().getValue(MAIN_CLASS_ATTRIBUTE);
		}
		if (startClass == null) {
			startClass = findMainMethod(source);
		}
		String launcherClassName = this.layout.getLauncherClassName();
		if (launcherClassName != null) {
			manifest.getMainAttributes().putValue(MAIN_CLASS_ATTRIBUTE,
					launcherClassName);
			if (startClass == null) {
				throw new IllegalStateException("Unable to find main class");
			}
			manifest.getMainAttributes().putValue(START_CLASS_ATTRIBUTE, startClass);
		}
		else if (startClass != null) {
			manifest.getMainAttributes().putValue(MAIN_CLASS_ATTRIBUTE, startClass);
		}
		String bootVersion = getClass().getPackage().getImplementationVersion();
		manifest.getMainAttributes().putValue(BOOT_VERSION_ATTRIBUTE, bootVersion);
		manifest.getMainAttributes().putValue(BOOT_CLASSES_ATTRIBUTE,
				(this.layout instanceof RepackagingLayout)
						? ((RepackagingLayout) this.layout).getRepackagedClassesLocation()
						: this.layout.getClassesLocation());
		manifest.getMainAttributes().putValue(BOOT_LIB_ATTRIBUTE,
				this.layout.getLibraryDestination("", LibraryScope.COMPILE));
		return manifest;
	}

	protected String findMainMethod(JarFile source) throws IOException {
		return MainClassFinder.findSingleMainClass(source,
				this.layout.getClassesLocation());
	}

	private void renameFile(File file, File dest) {
		if (!file.renameTo(dest)) {
			throw new IllegalStateException(
					"Unable to rename '" + file + "' to '" + dest + "'");
		}
	}

	private void deleteFile(File file) {
		if (!file.delete()) {
			throw new IllegalStateException("Unable to delete '" + file + "'");
		}
	}

	/**
	 * An {@code EntryTransformer} that renames entries by applying a prefix.
	 */
	private static final class RenamingEntryTransformer implements EntryTransformer {

		private final String namePrefix;

		private RenamingEntryTransformer(String namePrefix) {
			this.namePrefix = namePrefix;
		}

		@Override
		public JarEntry transform(JarEntry entry) {
			if (entry.getName().startsWith("META-INF/")
					|| entry.getName().startsWith("BOOT-INF/")) {
				return entry;
			}
			JarEntry renamedEntry = new JarEntry(this.namePrefix + entry.getName());
			renamedEntry.setTime(entry.getTime());
			renamedEntry.setSize(entry.getSize());
			renamedEntry.setMethod(entry.getMethod());
			if (entry.getComment() != null) {
				renamedEntry.setComment(entry.getComment());
			}
			renamedEntry.setCompressedSize(entry.getCompressedSize());
			renamedEntry.setCrc(entry.getCrc());
			setCreationTimeIfPossible(entry, renamedEntry);
			if (entry.getExtra() != null) {
				renamedEntry.setExtra(entry.getExtra());
			}
			setLastAccessTimeIfPossible(entry, renamedEntry);
			setLastModifiedTimeIfPossible(entry, renamedEntry);
			return renamedEntry;
		}

		@UsesJava8
		private void setCreationTimeIfPossible(JarEntry source, JarEntry target) {
			try {
				if (source.getCreationTime() != null) {
					target.setCreationTime(source.getCreationTime());
				}
			}
			catch (NoSuchMethodError ex) {
				// Not running on Java 8. Continue.
			}
		}

		@UsesJava8
		private void setLastAccessTimeIfPossible(JarEntry source, JarEntry target) {
			try {
				if (source.getLastAccessTime() != null) {
					target.setLastAccessTime(source.getLastAccessTime());
				}
			}
			catch (NoSuchMethodError ex) {
				// Not running on Java 8. Continue.
			}
		}

		@UsesJava8
		private void setLastModifiedTimeIfPossible(JarEntry source, JarEntry target) {
			try {
				if (source.getLastModifiedTime() != null) {
					target.setLastModifiedTime(source.getLastModifiedTime());
				}
			}
			catch (NoSuchMethodError ex) {
				// Not running on Java 8. Continue.
			}
		}

	}

}
