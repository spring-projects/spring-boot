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

package org.springframework.boot.loader.jar;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.springframework.boot.loader.zip.ZipContent;

/**
 * Security information ({@link Certificate} and {@link CodeSigner} details) for entries
 * in the jar.
 *
 * @author Phillip Webb
 */
final class SecurityInfo {

	static final SecurityInfo NONE = new SecurityInfo(null, null);

	private final Certificate[][] certificateLookups;

	private final CodeSigner[][] codeSignerLookups;

	private SecurityInfo(Certificate[][] entryCertificates, CodeSigner[][] entryCodeSigners) {
		this.certificateLookups = entryCertificates;
		this.codeSignerLookups = entryCodeSigners;
	}

	Certificate[] getCertificates(ZipContent.Entry contentEntry) {
		return (this.certificateLookups != null) ? clone(this.certificateLookups[contentEntry.getLookupIndex()]) : null;
	}

	CodeSigner[] getCodeSigners(ZipContent.Entry contentEntry) {
		return (this.codeSignerLookups != null) ? clone(this.codeSignerLookups[contentEntry.getLookupIndex()]) : null;
	}

	private <T> T[] clone(T[] array) {
		return (array != null) ? array.clone() : null;
	}

	/**
	 * Get the {@link SecurityInfo} for the given {@link ZipContent}.
	 * @param content the zip content
	 * @return the security info
	 */
	static SecurityInfo get(ZipContent content) {
		if (!content.hasJarSignatureFile()) {
			return NONE;
		}
		try {
			return load(content);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * Load security info from the jar file. We need to use {@link JarInputStream} to
	 * obtain the security info since we don't have an actual real file to read. This
	 * isn't that fast, but hopefully doesn't happen too often and the result is cached.
	 * @param content the zip content
	 * @return the security info
	 * @throws IOException on I/O error
	 */
	@SuppressWarnings("resource")
	private static SecurityInfo load(ZipContent content) throws IOException {
		int size = content.size();
		boolean hasSecurityInfo = false;
		Certificate[][] entryCertificates = new Certificate[size][];
		CodeSigner[][] entryCodeSigners = new CodeSigner[size][];
		try (JarEntriesStream entries = new JarEntriesStream(content.openRawZipData().asInputStream())) {
			JarEntry entry = entries.getNextEntry();
			while (entry != null) {
				ZipContent.Entry relatedEntry = content.getEntry(entry.getName());
				if (relatedEntry != null && entries.matches(relatedEntry.isDirectory(),
						relatedEntry.getUncompressedSize(), relatedEntry.getCompressionMethod(),
						() -> relatedEntry.openContent().asInputStream())) {
					Certificate[] certificates = entry.getCertificates();
					CodeSigner[] codeSigners = entry.getCodeSigners();
					if (certificates != null || codeSigners != null) {
						hasSecurityInfo = true;
						entryCertificates[relatedEntry.getLookupIndex()] = certificates;
						entryCodeSigners[relatedEntry.getLookupIndex()] = codeSigners;
					}
				}
				entry = entries.getNextEntry();
			}
		}
		return (!hasSecurityInfo) ? NONE : new SecurityInfo(entryCertificates, entryCodeSigners);
	}

}
