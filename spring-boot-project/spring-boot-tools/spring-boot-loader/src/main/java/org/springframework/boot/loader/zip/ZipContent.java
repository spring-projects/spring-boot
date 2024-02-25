/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.loader.zip;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.Cleaner.Cleanable;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.zip.ZipEntry;

import org.springframework.boot.loader.log.DebugLogger;

/**
 * Provides raw access to content from a regular or nested zip file. This class performs
 * the low level parsing of a zip file and provide access to raw entry data that it
 * contains. Unlike {@link java.util.zip.ZipFile}, this implementation can load content
 * from a zip file nested inside another file as long as the entry is not compressed.
 * <p>
 * In order to reduce memory consumption, this implementation stores only the hash of the
 * entry names, the central directory offsets and the original positions. Entries are
 * stored internally in {@code hashCode} order so that a binary search can be used to
 * quickly find an entry by name or determine if the zip file doesn't have a given entry.
 * <p>
 * {@link ZipContent} for a typical Spring Boot application JAR will have somewhere in the
 * region of 10,500 entries which should consume about 122K.
 * <p>
 * {@link ZipContent} results are cached and it is assumed that zip content will not
 * change once loaded. Entries and Strings are not cached and will be recreated on each
 * access which may produce a lot of garbage.
 * <p>
 * This implementation does not use {@link Cleanable} so care must be taken to release
 * {@link ZipContent} resources. The {@link #close()} method should be called explicitly
 * or by try-with-resources. Care must be take to only call close once.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 3.2.0
 */
public final class ZipContent implements Closeable {

	private static final String META_INF = "META-INF/";

	private static final byte[] SIGNATURE_SUFFIX = ".DSA".getBytes(StandardCharsets.UTF_8);

	private static final DebugLogger debug = DebugLogger.get(ZipContent.class);

	private static final Map<Source, ZipContent> cache = new ConcurrentHashMap<>();

	private final Source source;

	private final Kind kind;

	private final FileChannelDataBlock data;

	private final long centralDirectoryPos;

	private final long commentPos;

	private final long commentLength;

	private final int[] lookupIndexes;

	private final int[] nameHashLookups;

	private final int[] relativeCentralDirectoryOffsetLookups;

	private final NameOffsetLookups nameOffsetLookups;

	private final boolean hasJarSignatureFile;

	private SoftReference<CloseableDataBlock> virtualData;

	private SoftReference<Map<Class<?>, Object>> info;

	/**
     * Constructs a new ZipContent object with the specified parameters.
     * 
     * @param source the source of the zip content
     * @param kind the kind of the zip content
     * @param data the data block of the zip content
     * @param centralDirectoryPos the position of the central directory in the zip content
     * @param commentPos the position of the comment in the zip content
     * @param commentLength the length of the comment in the zip content
     * @param lookupIndexes the lookup indexes of the zip content
     * @param nameHashLookups the name hash lookups of the zip content
     * @param relativeCentralDirectoryOffsetLookups the relative central directory offset lookups of the zip content
     * @param nameOffsetLookups the name offset lookups of the zip content
     * @param hasJarSignatureFile indicates if the zip content has a JAR signature file
     */
    private ZipContent(Source source, Kind kind, FileChannelDataBlock data, long centralDirectoryPos, long commentPos,
			long commentLength, int[] lookupIndexes, int[] nameHashLookups, int[] relativeCentralDirectoryOffsetLookups,
			NameOffsetLookups nameOffsetLookups, boolean hasJarSignatureFile) {
		this.source = source;
		this.kind = kind;
		this.data = data;
		this.centralDirectoryPos = centralDirectoryPos;
		this.commentPos = commentPos;
		this.commentLength = commentLength;
		this.lookupIndexes = lookupIndexes;
		this.nameHashLookups = nameHashLookups;
		this.relativeCentralDirectoryOffsetLookups = relativeCentralDirectoryOffsetLookups;
		this.nameOffsetLookups = nameOffsetLookups;
		this.hasJarSignatureFile = hasJarSignatureFile;
	}

	/**
	 * Return the kind of content that was loaded.
	 * @return the content kind
	 * @since 3.2.2
	 */
	public Kind getKind() {
		return this.kind;
	}

	/**
	 * Open a {@link DataBlock} containing the raw zip data. For container zip files, this
	 * may be smaller than the original file since additional bytes are permitted at the
	 * front of a zip file. For nested zip files, this will be only the contents of the
	 * nest zip.
	 * <p>
	 * For nested directory zip files, a virtual data block will be created containing
	 * only the relevant content.
	 * <p>
	 * To release resources, the {@link #close()} method of the data block should be
	 * called explicitly or by try-with-resources.
	 * <p>
	 * The returned data block should not be accessed once {@link #close()} has been
	 * called.
	 * @return the zip data
	 * @throws IOException on I/O error
	 */
	public CloseableDataBlock openRawZipData() throws IOException {
		this.data.open();
		return (!this.nameOffsetLookups.hasAnyEnabled()) ? this.data : getVirtualData();
	}

	/**
     * Retrieves the virtual data block.
     * 
     * @return The virtual data block.
     * @throws IOException If an I/O error occurs.
     */
    private CloseableDataBlock getVirtualData() throws IOException {
		CloseableDataBlock virtualData = (this.virtualData != null) ? this.virtualData.get() : null;
		if (virtualData != null) {
			return virtualData;
		}
		virtualData = createVirtualData();
		this.virtualData = new SoftReference<>(virtualData);
		return virtualData;
	}

	/**
     * Creates a virtual data block for the ZipContent.
     * 
     * @return a CloseableDataBlock representing the virtual data block
     * @throws IOException if an I/O error occurs while creating the virtual data block
     */
    private CloseableDataBlock createVirtualData() throws IOException {
		int size = size();
		NameOffsetLookups nameOffsetLookups = this.nameOffsetLookups.emptyCopy();
		ZipCentralDirectoryFileHeaderRecord[] centralRecords = new ZipCentralDirectoryFileHeaderRecord[size];
		long[] centralRecordPositions = new long[size];
		for (int i = 0; i < size; i++) {
			int lookupIndex = ZipContent.this.lookupIndexes[i];
			long pos = getCentralDirectoryFileHeaderRecordPos(lookupIndex);
			nameOffsetLookups.enable(i, this.nameOffsetLookups.isEnabled(lookupIndex));
			centralRecords[i] = ZipCentralDirectoryFileHeaderRecord.load(this.data, pos);
			centralRecordPositions[i] = pos;
		}
		return new VirtualZipDataBlock(this.data, nameOffsetLookups, centralRecords, centralRecordPositions);
	}

	/**
	 * Returns the number of entries in the ZIP file.
	 * @return the number of entries
	 */
	public int size() {
		return this.lookupIndexes.length;
	}

	/**
	 * Return the zip comment, if any.
	 * @return the comment or {@code null}
	 */
	public String getComment() {
		try {
			return ZipString.readString(this.data, this.commentPos, this.commentLength);
		}
		catch (UncheckedIOException ex) {
			if (ex.getCause() instanceof ClosedChannelException) {
				throw new IllegalStateException("Zip content closed", ex);
			}
			throw ex;
		}
	}

	/**
	 * Return the entry with the given name, if any.
	 * @param name the name of the entry to find
	 * @return the entry or {@code null}
	 */
	public Entry getEntry(CharSequence name) {
		return getEntry(null, name);
	}

	/**
	 * Return the entry with the given name, if any.
	 * @param namePrefix an optional prefix for the name
	 * @param name the name of the entry to find
	 * @return the entry or {@code null}
	 */
	public Entry getEntry(CharSequence namePrefix, CharSequence name) {
		int nameHash = nameHash(namePrefix, name);
		int lookupIndex = getFirstLookupIndex(nameHash);
		int size = size();
		while (lookupIndex >= 0 && lookupIndex < size && this.nameHashLookups[lookupIndex] == nameHash) {
			long pos = getCentralDirectoryFileHeaderRecordPos(lookupIndex);
			ZipCentralDirectoryFileHeaderRecord centralRecord = loadZipCentralDirectoryFileHeaderRecord(pos);
			if (hasName(lookupIndex, centralRecord, pos, namePrefix, name)) {
				return new Entry(lookupIndex, centralRecord);
			}
			lookupIndex++;
		}
		return null;
	}

	/**
	 * Return if an entry with the given name exists.
	 * @param namePrefix an optional prefix for the name
	 * @param name the name of the entry to find
	 * @return the entry or {@code null}
	 */
	public boolean hasEntry(CharSequence namePrefix, CharSequence name) {
		int nameHash = nameHash(namePrefix, name);
		int lookupIndex = getFirstLookupIndex(nameHash);
		int size = size();
		while (lookupIndex >= 0 && lookupIndex < size && this.nameHashLookups[lookupIndex] == nameHash) {
			long pos = getCentralDirectoryFileHeaderRecordPos(lookupIndex);
			ZipCentralDirectoryFileHeaderRecord centralRecord = loadZipCentralDirectoryFileHeaderRecord(pos);
			if (hasName(lookupIndex, centralRecord, pos, namePrefix, name)) {
				return true;
			}
			lookupIndex++;
		}
		return false;
	}

	/**
	 * Return the entry at the specified index.
	 * @param index the entry index
	 * @return the entry
	 * @throws IndexOutOfBoundsException if the index is out of bounds
	 */
	public Entry getEntry(int index) {
		int lookupIndex = ZipContent.this.lookupIndexes[index];
		long pos = getCentralDirectoryFileHeaderRecordPos(lookupIndex);
		ZipCentralDirectoryFileHeaderRecord centralRecord = loadZipCentralDirectoryFileHeaderRecord(pos);
		return new Entry(lookupIndex, centralRecord);
	}

	/**
     * Loads the ZipCentralDirectoryFileHeaderRecord from the specified position in the data.
     *
     * @param pos the position in the data to load the record from
     * @return the loaded ZipCentralDirectoryFileHeaderRecord
     * @throws IllegalStateException if the Zip content is closed
     * @throws UncheckedIOException if an IOException occurs while loading the record
     */
    private ZipCentralDirectoryFileHeaderRecord loadZipCentralDirectoryFileHeaderRecord(long pos) {
		try {
			return ZipCentralDirectoryFileHeaderRecord.load(this.data, pos);
		}
		catch (IOException ex) {
			if (ex instanceof ClosedChannelException) {
				throw new IllegalStateException("Zip content closed", ex);
			}
			throw new UncheckedIOException(ex);
		}
	}

	/**
     * Calculates the hash value for a given name by combining the hash values of the name prefix and the name itself.
     * 
     * @param namePrefix the prefix of the name (can be null)
     * @param name the name to calculate the hash value for
     * @return the calculated hash value for the name
     */
    private int nameHash(CharSequence namePrefix, CharSequence name) {
		int nameHash = 0;
		nameHash = (namePrefix != null) ? ZipString.hash(nameHash, namePrefix, false) : nameHash;
		nameHash = ZipString.hash(nameHash, name, true);
		return nameHash;
	}

	/**
     * Returns the index of the first occurrence of the specified name hash in the nameHashLookups array.
     * 
     * @param nameHash the name hash to search for
     * @return the index of the first occurrence of the specified name hash, or -1 if not found
     */
    private int getFirstLookupIndex(int nameHash) {
		int lookupIndex = Arrays.binarySearch(this.nameHashLookups, 0, this.nameHashLookups.length, nameHash);
		if (lookupIndex < 0) {
			return -1;
		}
		while (lookupIndex > 0 && this.nameHashLookups[lookupIndex - 1] == nameHash) {
			lookupIndex--;
		}
		return lookupIndex;
	}

	/**
     * Returns the position of the central directory file header record based on the given lookup index.
     * 
     * @param lookupIndex the index used to lookup the relative central directory offset
     * @return the position of the central directory file header record
     */
    private long getCentralDirectoryFileHeaderRecordPos(int lookupIndex) {
		return this.centralDirectoryPos + this.relativeCentralDirectoryOffsetLookups[lookupIndex];
	}

	/**
     * Checks if the given name exists in the zip file at the specified lookup index.
     * 
     * @param lookupIndex the index of the lookup
     * @param centralRecord the central directory file header record
     * @param pos the position in the zip file
     * @param namePrefix the prefix of the name to match (can be null)
     * @param name the name to match
     * @return true if the name exists, false otherwise
     */
    private boolean hasName(int lookupIndex, ZipCentralDirectoryFileHeaderRecord centralRecord, long pos,
			CharSequence namePrefix, CharSequence name) {
		int offset = this.nameOffsetLookups.get(lookupIndex);
		pos += ZipCentralDirectoryFileHeaderRecord.FILE_NAME_OFFSET + offset;
		int len = centralRecord.fileNameLength() - offset;
		ByteBuffer buffer = ByteBuffer.allocate(ZipString.BUFFER_SIZE);
		if (namePrefix != null) {
			int startsWithNamePrefix = ZipString.startsWith(buffer, this.data, pos, len, namePrefix);
			if (startsWithNamePrefix == -1) {
				return false;
			}
			pos += startsWithNamePrefix;
			len -= startsWithNamePrefix;
		}
		return ZipString.matches(buffer, this.data, pos, len, name, true);
	}

	/**
	 * Get or compute information based on the {@link ZipContent}.
	 * @param <I> the info type to get or compute
	 * @param type the info type to get or compute
	 * @param function the function used to compute the information
	 * @return the computed or existing information
	 */
	@SuppressWarnings("unchecked")
	public <I> I getInfo(Class<I> type, Function<ZipContent, I> function) {
		Map<Class<?>, Object> info = (this.info != null) ? this.info.get() : null;
		if (info == null) {
			info = new ConcurrentHashMap<>();
			this.info = new SoftReference<>(info);
		}
		return (I) info.computeIfAbsent(type, (key) -> {
			debug.log("Getting %s info from zip '%s'", type.getName(), this);
			return function.apply(this);
		});
	}

	/**
	 * Returns {@code true} if this zip contains a jar signature file
	 * ({@code META-INF/*.DSA}).
	 * @return if the zip contains a jar signature file
	 */
	public boolean hasJarSignatureFile() {
		return this.hasJarSignatureFile;
	}

	/**
	 * Close this jar file, releasing the underlying file if this was the last reference.
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		this.data.close();
	}

	/**
     * Returns a string representation of the ZipContent object.
     * 
     * @return a string representation of the ZipContent object
     */
    @Override
	public String toString() {
		return this.source.toString();
	}

	/**
	 * Open {@link ZipContent} from the specified path. The resulting {@link ZipContent}
	 * <em>must</em> be {@link #close() closed} by the caller.
	 * @param path the zip path
	 * @return a {@link ZipContent} instance
	 * @throws IOException on I/O error
	 */
	public static ZipContent open(Path path) throws IOException {
		return open(new Source(path.toAbsolutePath(), null));
	}

	/**
	 * Open nested {@link ZipContent} from the specified path. The resulting
	 * {@link ZipContent} <em>must</em> be {@link #close() closed} by the caller.
	 * @param path the zip path
	 * @param nestedEntryName the nested entry name to open
	 * @return a {@link ZipContent} instance
	 * @throws IOException on I/O error
	 */
	public static ZipContent open(Path path, String nestedEntryName) throws IOException {
		return open(new Source(path.toAbsolutePath(), nestedEntryName));
	}

	/**
     * Opens the zip content from the given source.
     * 
     * @param source the source from which to load the zip content
     * @return the opened zip content
     * @throws IOException if an I/O error occurs while opening the zip content
     */
    private static ZipContent open(Source source) throws IOException {
		ZipContent zipContent = cache.get(source);
		if (zipContent != null) {
			debug.log("Opening existing cached zip content for %s", zipContent);
			zipContent.data.open();
			return zipContent;
		}
		debug.log("Loading zip content from %s", source);
		zipContent = Loader.load(source);
		ZipContent previouslyCached = cache.putIfAbsent(source, zipContent);
		if (previouslyCached != null) {
			debug.log("Closing zip content from %s since cache was populated from another thread", source);
			zipContent.close();
			previouslyCached.data.open();
			return previouslyCached;
		}
		return zipContent;
	}

	/**
	 * Zip content kinds.
	 *
	 * @since 3.2.2
	 */
	public enum Kind {

		/**
		 * Content from a standard zip file.
		 */
		ZIP,

		/**
		 * Content from nested zip content.
		 */
		NESTED_ZIP,

		/**
		 * Content from a nested zip directory.
		 */
		NESTED_DIRECTORY

	}

	/**
	 * The source of {@link ZipContent}. Used as a cache key.
	 *
	 * @param path the path of the zip or container zip
	 * @param nestedEntryName the name of the nested entry to use or {@code null}
	 */
	private record Source(Path path, String nestedEntryName) {

		/**
		 * Return if this is the source of a nested zip.
		 * @return if this is for a nested zip
		 */
		boolean isNested() {
			return this.nestedEntryName != null;
		}

		/**
     * Returns a string representation of the ZipContent object.
     * If the object is not nested, it returns the path as a string.
     * If the object is nested, it returns the path followed by the nested entry name enclosed in square brackets.
     *
     * @return a string representation of the ZipContent object
     */
    @Override
		public String toString() {
			return (!isNested()) ? path().toString() : path() + "[" + nestedEntryName() + "]";
		}

	}

	/**
	 * Internal class used to load the zip content create a new {@link ZipContent}
	 * instance.
	 */
	private static final class Loader {

		private final ByteBuffer buffer = ByteBuffer.allocate(ZipString.BUFFER_SIZE);

		private final Source source;

		private final FileChannelDataBlock data;

		private final long centralDirectoryPos;

		private final int[] index;

		private int[] nameHashLookups;

		private int[] relativeCentralDirectoryOffsetLookups;

		private final NameOffsetLookups nameOffsetLookups;

		private int cursor;

		/**
         * Constructs a new Loader object with the given parameters.
         * 
         * @param source the source object used for loading data
         * @param directoryEntry the directory entry object representing the directory
         * @param data the data block containing file channel data
         * @param centralDirectoryPos the position of the central directory
         * @param maxSize the maximum size of the loader
         */
        private Loader(Source source, Entry directoryEntry, FileChannelDataBlock data, long centralDirectoryPos,
				int maxSize) {
			this.source = source;
			this.data = data;
			this.centralDirectoryPos = centralDirectoryPos;
			this.index = new int[maxSize];
			this.nameHashLookups = new int[maxSize];
			this.relativeCentralDirectoryOffsetLookups = new int[maxSize];
			this.nameOffsetLookups = (directoryEntry != null)
					? new NameOffsetLookups(directoryEntry.getName().length(), maxSize) : NameOffsetLookups.NONE;
		}

		/**
         * Adds a ZipCentralDirectoryFileHeaderRecord to the loader.
         * 
         * @param centralRecord The ZipCentralDirectoryFileHeaderRecord to be added.
         * @param pos The position of the record in the file.
         * @param enableNameOffset Flag indicating whether to enable name offset.
         * @throws IOException If an I/O error occurs.
         */
        private void add(ZipCentralDirectoryFileHeaderRecord centralRecord, long pos, boolean enableNameOffset)
				throws IOException {
			int nameOffset = this.nameOffsetLookups.enable(this.cursor, enableNameOffset);
			int hash = ZipString.hash(this.buffer, this.data,
					pos + ZipCentralDirectoryFileHeaderRecord.FILE_NAME_OFFSET + nameOffset,
					centralRecord.fileNameLength() - nameOffset, true);
			this.nameHashLookups[this.cursor] = hash;
			this.relativeCentralDirectoryOffsetLookups[this.cursor] = (int) ((pos - this.centralDirectoryPos));
			this.index[this.cursor] = this.cursor;
			this.cursor++;
		}

		/**
         * Finishes the creation of a ZipContent object.
         * 
         * @param kind                  The kind of ZipContent.
         * @param commentPos            The position of the comment in the Zip file.
         * @param commentLength         The length of the comment in the Zip file.
         * @param hasJarSignatureFile   Indicates if the Zip file has a JAR signature file.
         * @return                      The created ZipContent object.
         */
        private ZipContent finish(Kind kind, long commentPos, long commentLength, boolean hasJarSignatureFile) {
			if (this.cursor != this.nameHashLookups.length) {
				this.nameHashLookups = Arrays.copyOf(this.nameHashLookups, this.cursor);
				this.relativeCentralDirectoryOffsetLookups = Arrays.copyOf(this.relativeCentralDirectoryOffsetLookups,
						this.cursor);
			}
			int size = this.nameHashLookups.length;
			sort(0, size - 1);
			int[] lookupIndexes = new int[size];
			for (int i = 0; i < size; i++) {
				lookupIndexes[this.index[i]] = i;
			}
			return new ZipContent(this.source, kind, this.data, this.centralDirectoryPos, commentPos, commentLength,
					lookupIndexes, this.nameHashLookups, this.relativeCentralDirectoryOffsetLookups,
					this.nameOffsetLookups, hasJarSignatureFile);
		}

		/**
         * Sorts the array using the Quick sort algorithm.
         * 
         * @param left  the starting index of the array to be sorted
         * @param right the ending index of the array to be sorted
         */
        private void sort(int left, int right) {
			// Quick sort algorithm, uses nameHashCode as the source but sorts all arrays
			if (left < right) {
				int pivot = this.nameHashLookups[left + (right - left) / 2];
				int i = left;
				int j = right;
				while (i <= j) {
					while (this.nameHashLookups[i] < pivot) {
						i++;
					}
					while (this.nameHashLookups[j] > pivot) {
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
         * Swaps the elements at the specified indices in the index, nameHashLookups, relativeCentralDirectoryOffsetLookups, and nameOffsetLookups arrays.
         * 
         * @param i the index of the first element to be swapped
         * @param j the index of the second element to be swapped
         */
        private void swap(int i, int j) {
			swap(this.index, i, j);
			swap(this.nameHashLookups, i, j);
			swap(this.relativeCentralDirectoryOffsetLookups, i, j);
			this.nameOffsetLookups.swap(i, j);
		}

		/**
         * Swaps the elements at the specified indices in the given array.
         *
         * @param array the array in which the elements should be swapped
         * @param i the index of the first element to be swapped
         * @param j the index of the second element to be swapped
         */
        private static void swap(int[] array, int i, int j) {
			int temp = array[i];
			array[i] = array[j];
			array[j] = temp;
		}

		/**
         * Loads the content from the given source.
         * 
         * @param source the source from which to load the content
         * @return the loaded ZipContent
         * @throws IOException if an I/O error occurs while loading the content
         */
        static ZipContent load(Source source) throws IOException {
			if (!source.isNested()) {
				return loadNonNested(source);
			}
			try (ZipContent zip = open(source.path())) {
				Entry entry = zip.getEntry(source.nestedEntryName());
				if (entry == null) {
					throw new IOException("Nested entry '%s' not found in container zip '%s'"
						.formatted(source.nestedEntryName(), source.path()));
				}
				return (!entry.isDirectory()) ? loadNestedZip(source, entry) : loadNestedDirectory(source, zip, entry);
			}
		}

		/**
         * Loads a non-nested zip file from the given source.
         * 
         * @param source the source of the zip file
         * @return the loaded zip content
         * @throws IOException if an I/O error occurs while loading the zip file
         */
        private static ZipContent loadNonNested(Source source) throws IOException {
			debug.log("Loading non-nested zip '%s'", source.path());
			return openAndLoad(source, Kind.ZIP, new FileChannelDataBlock(source.path()));
		}

		/**
         * Loads a nested zip file from the given source and entry.
         * 
         * @param source the source of the nested zip file
         * @param entry the entry representing the nested zip file in the container zip
         * @return the loaded zip content
         * @throws IOException if an I/O error occurs while loading the nested zip file
         */
        private static ZipContent loadNestedZip(Source source, Entry entry) throws IOException {
			if (entry.centralRecord.compressionMethod() != ZipEntry.STORED) {
				throw new IOException("Nested entry '%s' in container zip '%s' must not be compressed"
					.formatted(source.nestedEntryName(), source.path()));
			}
			debug.log("Loading nested zip entry '%s' from '%s'", source.nestedEntryName(), source.path());
			return openAndLoad(source, Kind.NESTED_ZIP, entry.getContent());
		}

		/**
         * Opens and loads the content from the specified source, kind, and data block.
         * 
         * @param source the source of the content
         * @param kind the kind of content
         * @param data the data block containing the content
         * @return the loaded ZipContent object
         * @throws IOException if an I/O error occurs while opening or loading the content
         * @throws RuntimeException if a runtime exception occurs while opening or loading the content
         */
        private static ZipContent openAndLoad(Source source, Kind kind, FileChannelDataBlock data) throws IOException {
			try {
				data.open();
				return loadContent(source, kind, data);
			}
			catch (IOException | RuntimeException ex) {
				data.close();
				throw ex;
			}
		}

		/**
         * Loads the content of a ZIP file from the given source, kind, and data block.
         * 
         * @param source The source of the ZIP file.
         * @param kind The kind of the ZIP file.
         * @param data The data block containing the ZIP file.
         * @return The loaded ZIP content.
         * @throws IOException If an I/O error occurs while loading the ZIP content.
         */
        private static ZipContent loadContent(Source source, Kind kind, FileChannelDataBlock data) throws IOException {
			ZipEndOfCentralDirectoryRecord.Located locatedEocd = ZipEndOfCentralDirectoryRecord.load(data);
			ZipEndOfCentralDirectoryRecord eocd = locatedEocd.endOfCentralDirectoryRecord();
			long eocdPos = locatedEocd.pos();
			Zip64EndOfCentralDirectoryLocator zip64Locator = Zip64EndOfCentralDirectoryLocator.find(data, eocdPos);
			Zip64EndOfCentralDirectoryRecord zip64Eocd = Zip64EndOfCentralDirectoryRecord.load(data, zip64Locator);
			data = data.slice(getStartOfZipContent(data, eocd, zip64Eocd));
			long centralDirectoryPos = (zip64Eocd != null) ? zip64Eocd.offsetToStartOfCentralDirectory()
					: Integer.toUnsignedLong(eocd.offsetToStartOfCentralDirectory());
			long numberOfEntries = (zip64Eocd != null) ? zip64Eocd.totalNumberOfCentralDirectoryEntries()
					: Short.toUnsignedInt(eocd.totalNumberOfCentralDirectoryEntries());
			if (numberOfEntries < 0) {
				throw new IllegalStateException("Invalid number of zip entries in " + source);
			}
			if (numberOfEntries > Integer.MAX_VALUE) {
				throw new IllegalStateException("Too many zip entries in " + source);
			}
			Loader loader = new Loader(source, null, data, centralDirectoryPos, (int) numberOfEntries);
			ByteBuffer signatureNameSuffixBuffer = ByteBuffer.allocate(SIGNATURE_SUFFIX.length);
			boolean hasJarSignatureFile = false;
			long pos = centralDirectoryPos;
			for (int i = 0; i < numberOfEntries; i++) {
				ZipCentralDirectoryFileHeaderRecord centralRecord = ZipCentralDirectoryFileHeaderRecord.load(data, pos);
				if (!hasJarSignatureFile) {
					long filenamePos = pos + ZipCentralDirectoryFileHeaderRecord.FILE_NAME_OFFSET;
					if (centralRecord.fileNameLength() > SIGNATURE_SUFFIX.length && ZipString.startsWith(loader.buffer,
							data, filenamePos, centralRecord.fileNameLength(), META_INF) >= 0) {
						signatureNameSuffixBuffer.clear();
						data.readFully(signatureNameSuffixBuffer,
								filenamePos + centralRecord.fileNameLength() - SIGNATURE_SUFFIX.length);
						hasJarSignatureFile = Arrays.equals(SIGNATURE_SUFFIX, signatureNameSuffixBuffer.array());
					}
				}
				loader.add(centralRecord, pos, false);
				pos += centralRecord.size();
			}
			long commentPos = locatedEocd.pos() + ZipEndOfCentralDirectoryRecord.COMMENT_OFFSET;
			return loader.finish(kind, commentPos, eocd.commentLength(), hasJarSignatureFile);
		}

		/**
		 * Returns the location in the data that the archive actually starts. For most
		 * files the archive data will start at 0, however, it is possible to have
		 * prefixed bytes (often used for startup scripts) at the beginning of the data.
		 * @param data the source data
		 * @param eocd the end of central directory record
		 * @param zip64Eocd the zip64 end of central directory record or {@code null}
		 * @return the offset within the data where the archive begins
		 * @throws IOException on I/O error
		 */
		private static long getStartOfZipContent(FileChannelDataBlock data, ZipEndOfCentralDirectoryRecord eocd,
				Zip64EndOfCentralDirectoryRecord zip64Eocd) throws IOException {
			long specifiedOffsetToStartOfCentralDirectory = (zip64Eocd != null)
					? zip64Eocd.offsetToStartOfCentralDirectory() : eocd.offsetToStartOfCentralDirectory();
			long sizeOfCentralDirectoryAndEndRecords = getSizeOfCentralDirectoryAndEndRecords(eocd, zip64Eocd);
			long actualOffsetToStartOfCentralDirectory = data.size() - sizeOfCentralDirectoryAndEndRecords;
			return actualOffsetToStartOfCentralDirectory - specifiedOffsetToStartOfCentralDirectory;
		}

		/**
         * Calculates the size of the central directory and end records in a ZIP file.
         * 
         * @param eocd The end of central directory record.
         * @param zip64Eocd The ZIP64 end of central directory record.
         * @return The size of the central directory and end records.
         */
        private static long getSizeOfCentralDirectoryAndEndRecords(ZipEndOfCentralDirectoryRecord eocd,
				Zip64EndOfCentralDirectoryRecord zip64Eocd) {
			long result = 0;
			result += eocd.size();
			if (zip64Eocd != null) {
				result += Zip64EndOfCentralDirectoryLocator.SIZE;
				result += zip64Eocd.size();
			}
			result += (zip64Eocd != null) ? zip64Eocd.sizeOfCentralDirectory() : eocd.sizeOfCentralDirectory();
			return result;
		}

		/**
         * Loads a nested directory entry from a source into a zip content.
         * 
         * @param source          the source of the nested directory entry
         * @param zip             the zip content to load the nested directory entry into
         * @param directoryEntry  the directory entry to load
         * @return                the loaded zip content with the nested directory entry
         * @throws IOException    if an I/O error occurs during loading
         * @throws IllegalArgumentException if the nested entry name does not end with '/'
         */
        private static ZipContent loadNestedDirectory(Source source, ZipContent zip, Entry directoryEntry)
				throws IOException {
			debug.log("Loading nested directory entry '%s' from '%s'", source.nestedEntryName(), source.path());
			if (!source.nestedEntryName().endsWith("/")) {
				throw new IllegalArgumentException("Nested entry name must end with '/'");
			}
			String directoryName = directoryEntry.getName();
			zip.data.open();
			try {
				Loader loader = new Loader(source, directoryEntry, zip.data, zip.centralDirectoryPos, zip.size());
				for (int cursor = 0; cursor < zip.size(); cursor++) {
					int index = zip.lookupIndexes[cursor];
					if (index != directoryEntry.getLookupIndex()) {
						long pos = zip.getCentralDirectoryFileHeaderRecordPos(index);
						ZipCentralDirectoryFileHeaderRecord centralRecord = ZipCentralDirectoryFileHeaderRecord
							.load(zip.data, pos);
						long namePos = pos + ZipCentralDirectoryFileHeaderRecord.FILE_NAME_OFFSET;
						short nameLen = centralRecord.fileNameLength();
						if (ZipString.startsWith(loader.buffer, zip.data, namePos, nameLen, directoryName) != -1) {
							loader.add(centralRecord, pos, true);
						}
					}
				}
				return loader.finish(Kind.NESTED_DIRECTORY, zip.commentPos, zip.commentLength, zip.hasJarSignatureFile);
			}
			catch (IOException | RuntimeException ex) {
				zip.data.close();
				throw ex;
			}
		}

	}

	/**
	 * A single zip content entry.
	 */
	public class Entry {

		private final int lookupIndex;

		private final ZipCentralDirectoryFileHeaderRecord centralRecord;

		private volatile String name;

		private volatile FileChannelDataBlock content;

		/**
		 * Create a new {@link Entry} instance.
		 * @param lookupIndex the lookup index of the entry
		 * @param centralRecord the {@link ZipCentralDirectoryFileHeaderRecord} for the
		 * entry
		 */
		Entry(int lookupIndex, ZipCentralDirectoryFileHeaderRecord centralRecord) {
			this.lookupIndex = lookupIndex;
			this.centralRecord = centralRecord;
		}

		/**
		 * Return the lookup index of the entry. Each entry has a unique lookup index but
		 * they aren't the same as the order that the entry was loaded.
		 * @return the entry lookup index
		 */
		public int getLookupIndex() {
			return this.lookupIndex;
		}

		/**
		 * Return {@code true} if this is a directory entry.
		 * @return if the entry is a directory
		 */
		public boolean isDirectory() {
			return getName().endsWith("/");
		}

		/**
		 * Returns {@code true} if this entry has a name starting with the given prefix.
		 * @param prefix the required prefix
		 * @return if the entry name starts with the prefix
		 */
		public boolean hasNameStartingWith(CharSequence prefix) {
			String name = this.name;
			if (name != null) {
				return name.startsWith(prefix.toString());
			}
			long pos = getCentralDirectoryFileHeaderRecordPos(this.lookupIndex)
					+ ZipCentralDirectoryFileHeaderRecord.FILE_NAME_OFFSET;
			return ZipString.startsWith(null, ZipContent.this.data, pos, this.centralRecord.fileNameLength(),
					prefix) != -1;
		}

		/**
		 * Return the name of this entry.
		 * @return the entry name
		 */
		public String getName() {
			String name = this.name;
			if (name == null) {
				int offset = ZipContent.this.nameOffsetLookups.get(this.lookupIndex);
				long pos = getCentralDirectoryFileHeaderRecordPos(this.lookupIndex)
						+ ZipCentralDirectoryFileHeaderRecord.FILE_NAME_OFFSET + offset;
				name = ZipString.readString(ZipContent.this.data, pos, this.centralRecord.fileNameLength() - offset);
				this.name = name;
			}
			return name;
		}

		/**
		 * Return the compression method for this entry.
		 * @return the compression method
		 * @see ZipEntry#STORED
		 * @see ZipEntry#DEFLATED
		 */
		public int getCompressionMethod() {
			return this.centralRecord.compressionMethod();
		}

		/**
		 * Return the uncompressed size of this entry.
		 * @return the uncompressed size
		 */
		public int getUncompressedSize() {
			return this.centralRecord.uncompressedSize();
		}

		/**
		 * Open a {@link DataBlock} providing access to raw contents of the entry (not
		 * including the local file header).
		 * <p>
		 * To release resources, the {@link #close()} method of the data block should be
		 * called explicitly or by try-with-resources.
		 * @return the contents of the entry
		 * @throws IOException on I/O error
		 */
		public CloseableDataBlock openContent() throws IOException {
			FileChannelDataBlock content = getContent();
			content.open();
			return content;
		}

		/**
         * Retrieves the content of the file channel data block.
         * 
         * @return The file channel data block content.
         * @throws IOException If an I/O error occurs.
         */
        private FileChannelDataBlock getContent() throws IOException {
			FileChannelDataBlock content = this.content;
			if (content == null) {
				int pos = this.centralRecord.offsetToLocalHeader();
				checkNotZip64Extended(pos);
				ZipLocalFileHeaderRecord localHeader = ZipLocalFileHeaderRecord.load(ZipContent.this.data, pos);
				int size = this.centralRecord.compressedSize();
				checkNotZip64Extended(size);
				content = ZipContent.this.data.slice(pos + localHeader.size(), size);
				this.content = content;
			}
			return content;
		}

		/**
         * Checks if the given value is equal to 0xFFFFFFFF, which indicates that Zip64 extended information extra fields are not supported.
         * 
         * @param value the value to check
         * @throws IOException if Zip64 extended information extra fields are not supported
         */
        private void checkNotZip64Extended(int value) throws IOException {
			if (value == 0xFFFFFFFF) {
				throw new IOException("Zip64 extended information extra fields are not supported");
			}
		}

		/**
		 * Adapt the raw entry into a {@link ZipEntry} or {@link ZipEntry} subclass.
		 * @param <E> the entry type
		 * @param factory the factory used to create the {@link ZipEntry}
		 * @return a fully populated zip entry
		 */
		public <E extends ZipEntry> E as(Function<String, E> factory) {
			return as((entry, name) -> factory.apply(name));
		}

		/**
		 * Adapt the raw entry into a {@link ZipEntry} or {@link ZipEntry} subclass.
		 * @param <E> the entry type
		 * @param factory the factory used to create the {@link ZipEntry}
		 * @return a fully populated zip entry
		 */
		public <E extends ZipEntry> E as(BiFunction<Entry, String, E> factory) {
			try {
				E result = factory.apply(this, getName());
				long pos = getCentralDirectoryFileHeaderRecordPos(this.lookupIndex);
				this.centralRecord.copyTo(ZipContent.this.data, pos, result);
				return result;
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

	}

}
