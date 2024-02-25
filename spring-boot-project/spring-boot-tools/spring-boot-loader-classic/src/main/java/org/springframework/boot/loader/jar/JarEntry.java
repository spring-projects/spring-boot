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
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Extended variant of {@link java.util.jar.JarEntry} returned by {@link JarFile}s.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class JarEntry extends java.util.jar.JarEntry implements FileHeader {

	private final int index;

	private final AsciiBytes name;

	private final AsciiBytes headerName;

	private final JarFile jarFile;

	private final long localHeaderOffset;

	private volatile JarEntryCertification certification;

	/**
     * Constructs a new JarEntry object.
     * 
     * @param jarFile the JarFile object representing the JAR file containing this entry
     * @param index the index of this entry within the JAR file
     * @param header the CentralDirectoryFileHeader object representing the header of this entry
     * @param nameAlias the alternative name for this entry, if available
     */
    JarEntry(JarFile jarFile, int index, CentralDirectoryFileHeader header, AsciiBytes nameAlias) {
		super((nameAlias != null) ? nameAlias.toString() : header.getName().toString());
		this.index = index;
		this.name = (nameAlias != null) ? nameAlias : header.getName();
		this.headerName = header.getName();
		this.jarFile = jarFile;
		this.localHeaderOffset = header.getLocalHeaderOffset();
		setCompressedSize(header.getCompressedSize());
		setMethod(header.getMethod());
		setCrc(header.getCrc());
		setComment(header.getComment().toString());
		setSize(header.getSize());
		setTime(header.getTime());
		if (header.hasExtra()) {
			setExtra(header.getExtra());
		}
	}

	/**
     * Returns the index of this JarEntry.
     *
     * @return the index of this JarEntry
     */
    int getIndex() {
		return this.index;
	}

	/**
     * Returns the ASCII bytes representation of the name of this JarEntry.
     *
     * @return the ASCII bytes representation of the name of this JarEntry
     */
    AsciiBytes getAsciiBytesName() {
		return this.name;
	}

	/**
     * Checks if the JarEntry has the specified name and suffix.
     * 
     * @param name   the name to be checked
     * @param suffix the suffix to be checked
     * @return true if the JarEntry has the specified name and suffix, false otherwise
     */
    @Override
	public boolean hasName(CharSequence name, char suffix) {
		return this.headerName.matches(name, suffix);
	}

	/**
	 * Return a {@link URL} for this {@link JarEntry}.
	 * @return the URL for the entry
	 * @throws MalformedURLException if the URL is not valid
	 */
	URL getUrl() throws MalformedURLException {
		return new URL(this.jarFile.getUrl(), getName());
	}

	/**
     * Returns the attributes of this JarEntry.
     * 
     * @return the attributes of this JarEntry, or null if the JarEntry does not have any attributes
     * @throws IOException if an I/O error occurs while reading the manifest file
     */
    @Override
	public Attributes getAttributes() throws IOException {
		Manifest manifest = this.jarFile.getManifest();
		return (manifest != null) ? manifest.getAttributes(getName()) : null;
	}

	/**
     * Returns an array of certificates associated with this JarEntry.
     * 
     * @return an array of certificates associated with this JarEntry
     */
    @Override
	public Certificate[] getCertificates() {
		return getCertification().getCertificates();
	}

	/**
     * Returns an array of CodeSigner objects representing the signers of the JAR entry.
     *
     * @return an array of CodeSigner objects representing the signers of the JAR entry
     */
    @Override
	public CodeSigner[] getCodeSigners() {
		return getCertification().getCodeSigners();
	}

	/**
     * Returns the certification status of the JarEntry.
     * 
     * @return the certification status of the JarEntry. Returns JarEntryCertification.NONE if the JarEntry is not signed.
     */
    private JarEntryCertification getCertification() {
		if (!this.jarFile.isSigned()) {
			return JarEntryCertification.NONE;
		}
		JarEntryCertification certification = this.certification;
		if (certification == null) {
			certification = this.jarFile.getCertification(this);
			this.certification = certification;
		}
		return certification;
	}

	/**
     * Returns the offset of the local file header for this JarEntry.
     *
     * @return the offset of the local file header
     */
    @Override
	public long getLocalHeaderOffset() {
		return this.localHeaderOffset;
	}

}
