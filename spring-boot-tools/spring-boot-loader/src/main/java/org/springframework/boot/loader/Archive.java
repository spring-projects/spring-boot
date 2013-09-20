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

package org.springframework.boot.loader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.Manifest;

/**
 * An archive that can be launched by the {@link AbstractLauncher}.
 * 
 * @author Phillip Webb
 * @see JarFileArchive
 */
public interface Archive {

	/**
	 * Returns the manifest of the archive.
	 * @return the manifest
	 * @throws IOException
	 */
	Manifest getManifest() throws IOException;

	/**
	 * Returns archive entries.
	 * @return the archive entries
	 */
	Iterable<Entry> getEntries();

	/**
	 * Returns a URL that can be used to load the archive.
	 * @return the archive URL
	 * @throws MalformedURLException
	 */
	URL getUrl() throws MalformedURLException;

	/**
	 * Returns a nest archive from on the the contained entries.
	 * @param entry the entry (may be a directory or file)
	 * @return the nested archive
	 * @throws IOException
	 */
	Archive getNestedArchive(Entry entry) throws IOException;

	/**
	 * Returns a filtered version of the archive.
	 * @param filter the filter to apply
	 * @return a filter archive
	 * @throws IOException
	 */
	Archive getFilteredArchive(EntryFilter filter) throws IOException;

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
		String getName();

	}

	/**
	 * A filter for archive entries.
	 */
	public static interface EntryFilter {

		/**
		 * Apply the jar entry filter.
		 * @param entryName the current entry name. This may be different that the
		 * original entry name if a previous filter has been applied
		 * @param entry the entry to filter
		 * @return the new name of the entry or {@code null} if the entry should not be
		 * included.
		 */
		String apply(String entryName, Entry entry);

	}

}
