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

package org.springframework.boot.loader.archive;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.jar.Manifest;

import org.springframework.boot.loader.Launcher;
import org.springframework.boot.loader.util.AsciiBytes;

/**
 * An archive that can be launched by the {@link Launcher}.
 *
 * @author Phillip Webb
 * @see JarFileArchive
 */
public abstract class Archive {

	/**
	 * Returns a URL that can be used to load the archive.
	 * @return the archive URL
	 * @throws MalformedURLException if the URL is malformed
	 */
	public abstract URL getUrl() throws MalformedURLException;

	/**
	 * Obtain the main class that should be used to launch the application. By default
	 * this method uses a {@code Start-Class} manifest entry.
	 * @return the main class
	 * @throws Exception if the main class cannot be obtained
	 */
	public String getMainClass() throws Exception {
		Manifest manifest = getManifest();
		String mainClass = null;
		if (manifest != null) {
			mainClass = manifest.getMainAttributes().getValue("Start-Class");
		}
		if (mainClass == null) {
			throw new IllegalStateException(
					"No 'Start-Class' manifest entry specified in " + this);
		}
		return mainClass;
	}

	@Override
	public String toString() {
		try {
			return getUrl().toString();
		}
		catch (Exception ex) {
			return "archive";
		}
	}

	/**
	 * Returns the manifest of the archive.
	 * @return the manifest
	 * @throws IOException if the manifest cannot be read
	 */
	public abstract Manifest getManifest() throws IOException;

	/**
	 * Returns all entries from the archive.
	 * @return the archive entries
	 */
	public abstract Collection<Entry> getEntries();

	/**
	 * Returns nested {@link Archive}s for entries that match the specified filter.
	 * @param filter the filter used to limit entries
	 * @return nested archives
	 * @throws IOException if nested archives cannot be read
	 */
	public abstract List<Archive> getNestedArchives(EntryFilter filter)
			throws IOException;

	/**
	 * Returns a filtered version of the archive.
	 * @param filter the filter to apply
	 * @return a filter archive
	 * @throws IOException if the archive cannot be read
	 */
	public abstract Archive getFilteredArchive(EntryRenameFilter filter)
			throws IOException;

	/**
	 * Represents a single entry in the archive.
	 */
	public static interface Entry {

		/**
		 * Returns {@code true} if the entry represents a directory.
		 * @return if the entry is a directory
		 */
		boolean isDirectory();

		/**
		 * Returns the name of the entry
		 * @return the name of the entry
		 */
		AsciiBytes getName();

	}

	/**
	 * Strategy interface to filter {@link Entry Entries}.
	 */
	public static interface EntryFilter {

		/**
		 * Apply the jar entry filter.
		 * @param entry the entry to filter
		 * @return {@code true} if the filter matches
		 */
		boolean matches(Entry entry);

	}

	/**
	 * Strategy interface to filter or rename {@link Entry Entries}.
	 */
	public static interface EntryRenameFilter {

		/**
		 * Apply the jar entry filter.
		 * @param entryName the current entry name. This may be different that the
		 * original entry name if a previous filter has been applied
		 * @param entry the entry to filter
		 * @return the new name of the entry or {@code null} if the entry should not be
		 * included.
		 */
		AsciiBytes apply(AsciiBytes entryName, Entry entry);

	}

}
