/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.launcher.tools;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Utility class that can be used to repackage an archive so that it can be executed using
 * '{@literal java -jar}'.
 * 
 * @author Phillip Webb
 */
public class Repackager {

	private static final String MAIN_CLASS_ATTRIBUTE = "Main-Class";

	private static final String START_CLASS_ATTRIBUTE = "Start-Class";

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
	 * MANIFEST will be used, or if no manifest entry is found a class the archive will be
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
	 * Repackage the source file so that it can be run using '{@literal java -jar}'
	 * @param libraries the libraries required to run the archive
	 * @throws IOException
	 */
	public void repackage(Libraries libraries) throws IOException {
		repackage(this.source, libraries);
	}

	/**
	 * Repackage to the given destination so that it can be run using '{@literal java -jar}
	 * '
	 * @param destination the destination file (may be the same as the source)
	 * @param libraries the libraries required to run the archive
	 * @throws IOException
	 */
	public void repackage(File destination, Libraries libraries) throws IOException {
		if (destination == null || destination.isDirectory()) {
			throw new IllegalArgumentException("Invalid destination");
		}
		if (libraries == null) {
			throw new IllegalArgumentException("Libraries must not be null");
		}
		destination = destination.getAbsoluteFile();
		File workingSource = this.source;
		if (this.source.equals(destination)) {
			workingSource = new File(this.source.getParentFile(), this.source.getName()
					+ ".original");
			workingSource.delete();
			renameFile(this.source, workingSource);
		}
		destination.delete();
		try {
			JarFile jarFileSource = new JarFile(workingSource);
			try {
				repackage(jarFileSource, destination, libraries);
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

	private void repackage(JarFile sourceJar, File destination, Libraries libraries)
			throws IOException {
		final JarWriter writer = new JarWriter(destination);
		try {
			writer.writeManifest(buildManifest(sourceJar));
			writer.writeEntries(sourceJar);
			libraries.doWithLibraries(new LibraryCallback() {

				@Override
				public void library(File file, LibraryScope scope) throws IOException {
					String destination = Repackager.this.layout.getLibraryDestination(
							file.getName(), scope);
					if (destination != null) {
						writer.writeNestedLibrary(destination, file);
					}
				}
			});
			writer.writeLoaderClasses();
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
			startClass = MainClassFinder.findMainClass(source,
					this.layout.getClassesLocation());
		}
		if (startClass == null) {
			throw new IllegalStateException("Unable to find main class");
		}
		manifest.getMainAttributes().putValue(MAIN_CLASS_ATTRIBUTE,
				this.layout.getLauncherClassName());
		manifest.getMainAttributes().putValue(START_CLASS_ATTRIBUTE, startClass);
		return manifest;
	}

	private void renameFile(File file, File dest) {
		if (!file.renameTo(dest)) {
			throw new IllegalStateException("Unable to rename '" + file + "' to '" + dest
					+ "'");
		}
	}

	private void deleteFile(File file) {
		if (!file.delete()) {
			throw new IllegalStateException("Unable to delete '" + file + "'");
		}
	}

}
