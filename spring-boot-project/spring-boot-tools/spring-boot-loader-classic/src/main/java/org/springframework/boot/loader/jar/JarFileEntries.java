/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader.jar;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.springframework.boot.loader.data.RandomAccessData;

/**
 * Provides access to entries from a {@link JarFile}. In order to reduce memory
 * consumption entry details are stored using arrays. The {@code hashCodes} array stores
 * the hash code of the entry name, the {@code centralDirectoryOffsets} provides the
 * offset to the central directory record and {@code positions} provides the original
 * order position of the entry. The arrays are stored in hashCode order so that a binary
 * search can be used to find a name.
 * <p>
 * A typical Spring Boot application will have somewhere in the region of 10,500 entries
 * which should consume about 122K.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class JarFileEntries implements CentralDirectoryVisitor, Iterable<JarEntry> {

	private static final Runnable NO_VALIDATION = () -> {
	};

	private static final String META_INF_PREFIX = "META-INF/";

	private static final Name MULTI_RELEASE = new Name("Multi-Release");

	private static final int BASE_VERSION = 8;

	private static final int RUNTIME_VERSION = Runtime.version().feature();

	private static final long LOCAL_FILE_HEADER_SIZE = 30;

	private static final char SLASH = '/';

	private static final char NO_SUFFIX = 0;

	protected static final int ENTRY_CACHE_SIZE = 25;

	private final JarFile jarFile;

	private final JarEntryFilter filter;

	private RandomAccessData centralDirectoryData;

	private int size;

	private int[] hashCodes;

	private Offsets centralDirectoryOffsets;

	private int[] positions;

	private Boolean multiReleaseJar;

	private JarEntryCertification[] certifications;

	private final Map<Integer, FileHeader> entriesCache = Collections
		.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {

			/**
     * Removes the eldest entry from the map if the size of the map exceeds the entry cache size.
     * 
     * @param eldest the eldest entry in the map
     * @return true if the eldest entry was removed, false otherwise
     */
    @Override
			protected boolean removeEldestEntry(Map.Entry<Integer, FileHeader> eldest) {
				return size() >= ENTRY_CACHE_SIZE;
			}

		});

	/**
     * Constructs a new JarFileEntries object with the specified JarFile and JarEntryFilter.
     * 
     * @param jarFile the JarFile to retrieve entries from
     * @param filter the JarEntryFilter to apply to the entries
     */
    JarFileEntries(JarFile jarFile, JarEntryFilter filter) {
		this.jarFile = jarFile;
		this.filter = filter;
	}

	/**
     * Visits the start of the central directory end record.
     * 
     * @param endRecord the central directory end record
     * @param centralDirectoryData the central directory data
     */
    @Override
	public void visitStart(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData) {
		int maxSize = endRecord.getNumberOfRecords();
		this.centralDirectoryData = centralDirectoryData;
		this.hashCodes = new int[maxSize];
		this.centralDirectoryOffsets = Offsets.from(endRecord);
		this.positions = new int[maxSize];
	}

	/**
     * Visits the file header of a central directory file in a JAR file.
     * 
     * @param fileHeader The central directory file header to visit.
     * @param dataOffset The offset of the file data in the JAR file.
     */
    @Override
	public void visitFileHeader(CentralDirectoryFileHeader fileHeader, long dataOffset) {
		AsciiBytes name = applyFilter(fileHeader.getName());
		if (name != null) {
			add(name, dataOffset);
		}
	}

	/**
     * Adds a new entry to the JarFileEntries.
     * 
     * @param name The name of the entry as an AsciiBytes object.
     * @param dataOffset The offset of the entry's data in the central directory.
     */
    private void add(AsciiBytes name, long dataOffset) {
		this.hashCodes[this.size] = name.hashCode();
		this.centralDirectoryOffsets.set(this.size, dataOffset);
		this.positions[this.size] = this.size;
		this.size++;
	}

	/**
     * Sorts the entries in the JarFileEntries object and updates the positions array accordingly.
     * 
     * This method uses the quicksort algorithm to sort the entries in ascending order based on their positions.
     * 
     * After sorting, the positions array is updated to reflect the new positions of the entries.
     * 
     * @param None
     * @return None
     */
    @Override
	public void visitEnd() {
		sort(0, this.size - 1);
		int[] positions = this.positions;
		this.positions = new int[positions.length];
		for (int i = 0; i < this.size; i++) {
			this.positions[positions[i]] = i;
		}
	}

	/**
     * Returns the size of the JarFileEntries object.
     *
     * @return the size of the JarFileEntries object
     */
    int getSize() {
		return this.size;
	}

	/**
     * Sorts the array of hashCodes using the Quick sort algorithm.
     * 
     * @param left  the starting index of the array
     * @param right the ending index of the array
     */
    private void sort(int left, int right) {
		// Quick sort algorithm, uses hashCodes as the source but sorts all arrays
		if (left < right) {
			int pivot = this.hashCodes[left + (right - left) / 2];
			int i = left;
			int j = right;
			while (i <= j) {
				while (this.hashCodes[i] < pivot) {
					i++;
				}
				while (this.hashCodes[j] > pivot) {
					j--;
				}
				if (i <= j) {
					swap(i, j);
					i++;
					j--;
				}
			}
			if (left < j) {
				sort(left, j);
			}
			if (right > i) {
				sort(i, right);
			}
		}
	}

	/**
     * Swaps the elements at the specified indices in the hashCodes, centralDirectoryOffsets, and positions arrays.
     *
     * @param i the index of the first element to be swapped
     * @param j the index of the second element to be swapped
     */
    private void swap(int i, int j) {
		swap(this.hashCodes, i, j);
		this.centralDirectoryOffsets.swap(i, j);
		swap(this.positions, i, j);
	}

	/**
     * Returns an iterator over the entries in this JarFileEntries object.
     * 
     * @return an iterator over the entries in this JarFileEntries object
     */
    @Override
	public Iterator<JarEntry> iterator() {
		return new EntryIterator(NO_VALIDATION);
	}

	/**
     * Returns an iterator over the entries in this JarFileEntries object.
     * 
     * @param validator the validator to be executed before returning each entry
     * @return an iterator over the entries in this JarFileEntries object
     */
    Iterator<JarEntry> iterator(Runnable validator) {
		return new EntryIterator(validator);
	}

	/**
     * Checks if the JarFileEntries contains an entry with the specified name.
     *
     * @param name the name of the entry to check
     * @return {@code true} if the JarFileEntries contains an entry with the specified name, {@code false} otherwise
     */
    boolean containsEntry(CharSequence name) {
		return getEntry(name, FileHeader.class, true) != null;
	}

	/**
     * Returns the JarEntry object associated with the specified name.
     * 
     * @param name the name of the entry to retrieve
     * @return the JarEntry object associated with the specified name
     */
    JarEntry getEntry(CharSequence name) {
		return getEntry(name, JarEntry.class, true);
	}

	/**
     * Retrieves the input stream for the specified file name.
     * 
     * @param name The name of the file to retrieve the input stream for.
     * @return The input stream for the specified file.
     * @throws IOException If an I/O error occurs while retrieving the input stream.
     */
    InputStream getInputStream(String name) throws IOException {
		FileHeader entry = getEntry(name, FileHeader.class, false);
		return getInputStream(entry);
	}

	/**
     * Returns an InputStream for reading the contents of the specified FileHeader entry.
     * 
     * @param entry the FileHeader entry to retrieve the InputStream for
     * @return an InputStream for reading the contents of the entry, or null if the entry is null
     * @throws IOException if an I/O error occurs while retrieving the InputStream
     */
    InputStream getInputStream(FileHeader entry) throws IOException {
		if (entry == null) {
			return null;
		}
		InputStream inputStream = getEntryData(entry).getInputStream();
		if (entry.getMethod() == ZipEntry.DEFLATED) {
			inputStream = new ZipInflaterInputStream(inputStream, (int) entry.getSize());
		}
		return inputStream;
	}

	/**
     * Retrieves the data of a specific entry in the Jar file.
     * 
     * @param name The name of the entry to retrieve the data for.
     * @return The data of the entry as a RandomAccessData object.
     * @throws IOException If an I/O error occurs while retrieving the entry data.
     */
    RandomAccessData getEntryData(String name) throws IOException {
		FileHeader entry = getEntry(name, FileHeader.class, false);
		if (entry == null) {
			return null;
		}
		return getEntryData(entry);
	}

	/**
     * Retrieves the data of a specific entry in the JAR file.
     * 
     * @param entry The file header of the entry to retrieve the data for.
     * @return The random access data of the entry.
     * @throws IOException If an I/O error occurs while reading the data.
     */
    private RandomAccessData getEntryData(FileHeader entry) throws IOException {
		// aspectjrt-1.7.4.jar has a different ext bytes length in the
		// local directory to the central directory. We need to re-read
		// here to skip them
		RandomAccessData data = this.jarFile.getData();
		byte[] localHeader = data.read(entry.getLocalHeaderOffset(), LOCAL_FILE_HEADER_SIZE);
		long nameLength = Bytes.littleEndianValue(localHeader, 26, 2);
		long extraLength = Bytes.littleEndianValue(localHeader, 28, 2);
		return data.getSubsection(entry.getLocalHeaderOffset() + LOCAL_FILE_HEADER_SIZE + nameLength + extraLength,
				entry.getCompressedSize());
	}

	/**
     * Retrieves the entry with the specified name and type from the JarFileEntries.
     * 
     * @param name         the name of the entry to retrieve
     * @param type         the type of the entry to retrieve
     * @param cacheEntry   indicates whether to cache the retrieved entry
     * @param <T>          the type of the entry
     * @return             the retrieved entry, or null if not found
     */
    private <T extends FileHeader> T getEntry(CharSequence name, Class<T> type, boolean cacheEntry) {
		T entry = doGetEntry(name, type, cacheEntry, null);
		if (!isMetaInfEntry(name) && isMultiReleaseJar()) {
			int version = RUNTIME_VERSION;
			AsciiBytes nameAlias = (entry instanceof JarEntry jarEntry) ? jarEntry.getAsciiBytesName()
					: new AsciiBytes(name.toString());
			while (version > BASE_VERSION) {
				T versionedEntry = doGetEntry("META-INF/versions/" + version + "/" + name, type, cacheEntry, nameAlias);
				if (versionedEntry != null) {
					return versionedEntry;
				}
				version--;
			}
		}
		return entry;
	}

	/**
     * Checks if the given name is a META-INF entry.
     * 
     * @param name the name to check
     * @return true if the name starts with the META-INF prefix, false otherwise
     */
    private boolean isMetaInfEntry(CharSequence name) {
		return name.toString().startsWith(META_INF_PREFIX);
	}

	/**
     * Checks if the JAR file is a multi-release JAR.
     * 
     * @return true if the JAR file is a multi-release JAR, false otherwise
     */
    private boolean isMultiReleaseJar() {
		Boolean multiRelease = this.multiReleaseJar;
		if (multiRelease != null) {
			return multiRelease;
		}
		try {
			Manifest manifest = this.jarFile.getManifest();
			if (manifest == null) {
				multiRelease = false;
			}
			else {
				Attributes attributes = manifest.getMainAttributes();
				multiRelease = attributes.containsKey(MULTI_RELEASE);
			}
		}
		catch (IOException ex) {
			multiRelease = false;
		}
		this.multiReleaseJar = multiRelease;
		return multiRelease;
	}

	/**
     * Retrieves the entry with the specified name and type from the JarFileEntries.
     * 
     * @param name       the name of the entry to retrieve
     * @param type       the type of the entry to retrieve
     * @param cacheEntry flag indicating whether to cache the retrieved entry
     * @param nameAlias  an alternative name for the entry
     * @param <T>        the type of the entry, must extend FileHeader
     * @return the retrieved entry, or null if not found
     */
    private <T extends FileHeader> T doGetEntry(CharSequence name, Class<T> type, boolean cacheEntry,
			AsciiBytes nameAlias) {
		int hashCode = AsciiBytes.hashCode(name);
		T entry = getEntry(hashCode, name, NO_SUFFIX, type, cacheEntry, nameAlias);
		if (entry == null) {
			hashCode = AsciiBytes.hashCode(hashCode, SLASH);
			entry = getEntry(hashCode, name, SLASH, type, cacheEntry, nameAlias);
		}
		return entry;
	}

	/**
     * Retrieves the entry with the specified hash code, name, suffix, and type from the JarFileEntries.
     * 
     * @param hashCode the hash code of the entry
     * @param name the name of the entry
     * @param suffix the suffix of the entry
     * @param type the type of the entry
     * @param cacheEntry specifies whether to cache the entry
     * @param nameAlias the alias name of the entry
     * @return the entry with the specified hash code, name, suffix, and type, or null if not found
     */
    private <T extends FileHeader> T getEntry(int hashCode, CharSequence name, char suffix, Class<T> type,
			boolean cacheEntry, AsciiBytes nameAlias) {
		int index = getFirstIndex(hashCode);
		while (index >= 0 && index < this.size && this.hashCodes[index] == hashCode) {
			T entry = getEntry(index, type, cacheEntry, nameAlias);
			if (entry.hasName(name, suffix)) {
				return entry;
			}
			index++;
		}
		return null;
	}

	/**
     * Retrieves the entry at the specified index from the JarFileEntries.
     * 
     * @param index      the index of the entry to retrieve
     * @param type       the class type of the entry
     * @param cacheEntry a boolean indicating whether to cache the entry
     * @param nameAlias  an AsciiBytes object representing the name alias
     * @return the entry at the specified index
     * @throws IllegalStateException if an IOException occurs
     */
    @SuppressWarnings("unchecked")
	private <T extends FileHeader> T getEntry(int index, Class<T> type, boolean cacheEntry, AsciiBytes nameAlias) {
		try {
			long offset = this.centralDirectoryOffsets.get(index);
			FileHeader cached = this.entriesCache.get(index);
			FileHeader entry = (cached != null) ? cached
					: CentralDirectoryFileHeader.fromRandomAccessData(this.centralDirectoryData, offset, this.filter);
			if (CentralDirectoryFileHeader.class.equals(entry.getClass()) && type.equals(JarEntry.class)) {
				entry = new JarEntry(this.jarFile, index, (CentralDirectoryFileHeader) entry, nameAlias);
			}
			if (cacheEntry && cached != entry) {
				this.entriesCache.put(index, entry);
			}
			return (T) entry;
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
     * Returns the first index of the specified hash code in the hashCodes array.
     * 
     * @param hashCode the hash code to search for
     * @return the first index of the specified hash code, or -1 if not found
     */
    private int getFirstIndex(int hashCode) {
		int index = Arrays.binarySearch(this.hashCodes, 0, this.size, hashCode);
		if (index < 0) {
			return -1;
		}
		while (index > 0 && this.hashCodes[index - 1] == hashCode) {
			index--;
		}
		return index;
	}

	/**
     * Clears the cache of entries.
     */
    void clearCache() {
		this.entriesCache.clear();
	}

	/**
     * Applies the filter to the given name and returns the filtered name.
     * If a filter is set, the filter will be applied to the name; otherwise, the original name will be returned.
     *
     * @param name the name to be filtered
     * @return the filtered name
     */
    private AsciiBytes applyFilter(AsciiBytes name) {
		return (this.filter != null) ? this.filter.apply(name) : name;
	}

	/**
     * Retrieves the certification for the given JarEntry.
     * 
     * @param entry The JarEntry for which the certification is to be retrieved.
     * @return The certification for the given JarEntry, or JarEntryCertification.NONE if no certification is found.
     * @throws IOException If an I/O error occurs while retrieving the certification.
     */
    JarEntryCertification getCertification(JarEntry entry) throws IOException {
		JarEntryCertification[] certifications = this.certifications;
		if (certifications == null) {
			certifications = new JarEntryCertification[this.size];
			// We fall back to use JarInputStream to obtain the certs. This isn't that
			// fast, but hopefully doesn't happen too often.
			try (JarInputStream certifiedJarStream = new JarInputStream(this.jarFile.getData().getInputStream())) {
				java.util.jar.JarEntry certifiedEntry;
				while ((certifiedEntry = certifiedJarStream.getNextJarEntry()) != null) {
					// Entry must be closed to trigger a read and set entry certificates
					certifiedJarStream.closeEntry();
					int index = getEntryIndex(certifiedEntry.getName());
					if (index != -1) {
						certifications[index] = JarEntryCertification.from(certifiedEntry);
					}
				}
			}
			this.certifications = certifications;
		}
		JarEntryCertification certification = certifications[entry.getIndex()];
		return (certification != null) ? certification : JarEntryCertification.NONE;
	}

	/**
     * Returns the index of the entry with the specified name.
     * 
     * @param name the name of the entry
     * @return the index of the entry, or -1 if not found
     */
    private int getEntryIndex(CharSequence name) {
		int hashCode = AsciiBytes.hashCode(name);
		int index = getFirstIndex(hashCode);
		while (index >= 0 && index < this.size && this.hashCodes[index] == hashCode) {
			FileHeader candidate = getEntry(index, FileHeader.class, false, null);
			if (candidate.hasName(name, NO_SUFFIX)) {
				return index;
			}
			index++;
		}
		return -1;
	}

	/**
     * Swaps two elements in an array.
     *
     * @param array the array containing the elements to be swapped
     * @param i the index of the first element to be swapped
     * @param j the index of the second element to be swapped
     */
    private static void swap(int[] array, int i, int j) {
		int temp = array[i];
		array[i] = array[j];
		array[j] = temp;
	}

	/**
     * Swaps the elements at the specified indices in the given array.
     *
     * @param array the array in which the elements should be swapped
     * @param i the index of the first element to be swapped
     * @param j the index of the second element to be swapped
     */
    private static void swap(long[] array, int i, int j) {
		long temp = array[i];
		array[i] = array[j];
		array[j] = temp;
	}

	/**
	 * Iterator for contained entries.
	 */
	private final class EntryIterator implements Iterator<JarEntry> {

		private final Runnable validator;

		private int index = 0;

		/**
         * Constructs a new EntryIterator with the specified validator.
         * 
         * @param validator the Runnable object to be executed as a validator
         * 
         * @throws NullPointerException if the validator is null
         */
        private EntryIterator(Runnable validator) {
			this.validator = validator;
			validator.run();
		}

		/**
         * Returns true if there is another entry in the iterator, false otherwise.
         * 
         * @return true if there is another entry in the iterator, false otherwise
         */
        @Override
		public boolean hasNext() {
			this.validator.run();
			return this.index < JarFileEntries.this.size;
		}

		/**
         * Returns the next JarEntry in the iteration.
         * 
         * @return the next JarEntry in the iteration
         * @throws NoSuchElementException if there are no more elements in the iteration
         */
        @Override
		public JarEntry next() {
			this.validator.run();
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			int entryIndex = JarFileEntries.this.positions[this.index];
			this.index++;
			return getEntry(entryIndex, JarEntry.class, false, null);
		}

	}

	/**
	 * Interface to manage offsets to central directory records. Regular zip files are
	 * backed by an {@code int[]} based implementation, Zip64 files are backed by a
	 * {@code long[]} and will consume more memory.
	 */
	private interface Offsets {

		void set(int index, long value);

		long get(int index);

		void swap(int i, int j);

		static Offsets from(CentralDirectoryEndRecord endRecord) {
			int size = endRecord.getNumberOfRecords();
			return endRecord.isZip64() ? new Zip64Offsets(size) : new ZipOffsets(size);
		}

	}

	/**
	 * {@link Offsets} implementation for regular zip files.
	 */
	private static final class ZipOffsets implements Offsets {

		private final int[] offsets;

		/**
         * Constructs a new ZipOffsets object with the specified size.
         * 
         * @param size the size of the ZipOffsets object
         */
        private ZipOffsets(int size) {
			this.offsets = new int[size];
		}

		/**
         * Swaps the elements at the specified indices in the offsets array.
         *
         * @param i the index of the first element to be swapped
         * @param j the index of the second element to be swapped
         */
        @Override
		public void swap(int i, int j) {
			JarFileEntries.swap(this.offsets, i, j);
		}

		/**
         * Sets the value at the specified index in the offsets array.
         * 
         * @param index the index at which to set the value
         * @param value the value to be set at the specified index
         */
        @Override
		public void set(int index, long value) {
			this.offsets[index] = (int) value;
		}

		/**
         * Returns the offset at the specified index in the ZipOffsets object.
         *
         * @param index the index of the offset to retrieve
         * @return the offset at the specified index
         */
        @Override
		public long get(int index) {
			return this.offsets[index];
		}

	}

	/**
	 * {@link Offsets} implementation for zip64 files.
	 */
	private static final class Zip64Offsets implements Offsets {

		private final long[] offsets;

		/**
         * Initializes a new instance of the Zip64Offsets class with the specified size.
         * 
         * @param size The size of the offsets array.
         */
        private Zip64Offsets(int size) {
			this.offsets = new long[size];
		}

		/**
         * Swaps the elements at the specified indices in the offsets array.
         *
         * @param i the index of the first element to be swapped
         * @param j the index of the second element to be swapped
         */
        @Override
		public void swap(int i, int j) {
			JarFileEntries.swap(this.offsets, i, j);
		}

		/**
         * Sets the value at the specified index in the offsets array.
         *
         * @param index the index at which to set the value
         * @param value the value to be set
         */
        @Override
		public void set(int index, long value) {
			this.offsets[index] = value;
		}

		/**
         * Returns the offset at the specified index in the Zip64Offsets array.
         *
         * @param index the index of the offset to retrieve
         * @return the offset at the specified index
         */
        @Override
		public long get(int index) {
			return this.offsets[index];
		}

	}

}
