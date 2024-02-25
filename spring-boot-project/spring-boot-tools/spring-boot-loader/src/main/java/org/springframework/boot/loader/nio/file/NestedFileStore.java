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

package org.springframework.boot.loader.nio.file;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

import org.springframework.boot.loader.net.protocol.nested.NestedLocation;

/**
 * {@link FileStore} implementation for {@link NestedLocation nested} jar files.
 *
 * @author Phillip Webb
 * @see NestedFileSystemProvider
 */
class NestedFileStore extends FileStore {

	private final NestedFileSystem fileSystem;

	/**
     * Constructs a new NestedFileStore object with the specified NestedFileSystem.
     * 
     * @param fileSystem the NestedFileSystem to be associated with the NestedFileStore
     */
    NestedFileStore(NestedFileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}

	/**
     * Returns the name of the file system associated with this NestedFileStore.
     * 
     * @return the name of the file system
     */
    @Override
	public String name() {
		return this.fileSystem.toString();
	}

	/**
     * Returns the type of the file store as "nestedfs".
     * 
     * @return the type of the file store
     */
    @Override
	public String type() {
		return "nestedfs";
	}

	/**
     * Returns a boolean value indicating whether the file store is read-only.
     * 
     * @return true if the file store is read-only, false otherwise
     */
    @Override
	public boolean isReadOnly() {
		return this.fileSystem.isReadOnly();
	}

	/**
     * Returns the total space in bytes available in the file store.
     *
     * @return the total space in bytes
     * @throws IOException if an I/O error occurs
     */
    @Override
	public long getTotalSpace() throws IOException {
		return 0;
	}

	/**
     * Returns the amount of space available for this file store, in bytes.
     *
     * @return the amount of space available for this file store, in bytes.
     * @throws IOException if an I/O error occurs.
     */
    @Override
	public long getUsableSpace() throws IOException {
		return 0;
	}

	/**
     * Returns the amount of unallocated space in the file store.
     *
     * @return the amount of unallocated space in bytes
     * @throws IOException if an I/O error occurs
     */
    @Override
	public long getUnallocatedSpace() throws IOException {
		return 0;
	}

	/**
     * Returns a boolean value indicating whether the specified file attribute view is supported by the nested file store.
     *
     * @param type the class representing the file attribute view
     * @return {@code true} if the specified file attribute view is supported, {@code false} otherwise
     */
    @Override
	public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
		return getJarPathFileStore().supportsFileAttributeView(type);
	}

	/**
     * Returns a boolean value indicating whether the specified file attribute view is supported by the nested file store.
     *
     * @param name the name of the file attribute view
     * @return {@code true} if the specified file attribute view is supported, {@code false} otherwise
     */
    @Override
	public boolean supportsFileAttributeView(String name) {
		return getJarPathFileStore().supportsFileAttributeView(name);
	}

	/**
     * Returns the file store attribute view of the specified type for the nested file store.
     * 
     * @param type the class object representing the type of the file store attribute view
     * @return the file store attribute view of the specified type
     * @throws UnsupportedOperationException if the attribute view is not available for the nested file store
     * @throws IllegalArgumentException if the specified type is null
     * @throws ClassCastException if the specified type does not implement the FileStoreAttributeView interface
     */
    @Override
	public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
		return getJarPathFileStore().getFileStoreAttributeView(type);
	}

	/**
     * Retrieves the value of the specified attribute from the NestedFileStore.
     * 
     * @param attribute the name of the attribute to retrieve
     * @return the value of the specified attribute
     * @throws IOException if an I/O error occurs while retrieving the attribute
     * @throws UncheckedIOException if an unchecked I/O error occurs while retrieving the attribute
     * @throws Throwable if the cause of the unchecked I/O error is a Throwable
     */
    @Override
	public Object getAttribute(String attribute) throws IOException {
		try {
			return getJarPathFileStore().getAttribute(attribute);
		}
		catch (UncheckedIOException ex) {
			throw ex.getCause();
		}
	}

	/**
     * Returns the file store for the JAR path of the file system.
     * 
     * @return the file store for the JAR path
     * @throws UncheckedIOException if an I/O error occurs while retrieving the file store
     */
    protected FileStore getJarPathFileStore() {
		try {
			return Files.getFileStore(this.fileSystem.getJarPath());
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
