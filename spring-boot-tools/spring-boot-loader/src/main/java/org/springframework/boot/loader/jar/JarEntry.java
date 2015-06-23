/*
 * Copyright 2012-2015 the original author or authors.
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
 */
public class JarEntry extends java.util.jar.JarEntry {

	private final JarEntryData source;

	private Certificate[] certificates;

	private CodeSigner[] codeSigners;

	public JarEntry(JarEntryData source) {
		super(source.getName().toString());
		this.source = source;
	}

	/**
	 * Return the source {@link JarEntryData} that was used to create this entry.
	 * @return the source of the entry
	 */
	public JarEntryData getSource() {
		return this.source;
	}

	/**
	 * Return a {@link URL} for this {@link JarEntry}.
	 * @return the URL for the entry
	 * @throws MalformedURLException if the URL is not valid
	 */
	public URL getUrl() throws MalformedURLException {
		return new URL(this.source.getSource().getUrl(), getName());
	}

	@Override
	public Attributes getAttributes() throws IOException {
		Manifest manifest = this.source.getSource().getManifest();
		return (manifest == null ? null : manifest.getAttributes(getName()));
	}

	@Override
	public Certificate[] getCertificates() {
		if (this.source.getSource().isSigned() && this.certificates == null) {
			this.source.getSource().setupEntryCertificates();
		}
		return this.certificates;
	}

	@Override
	public CodeSigner[] getCodeSigners() {
		if (this.source.getSource().isSigned() && this.codeSigners == null) {
			this.source.getSource().setupEntryCertificates();
		}
		return this.codeSigners;
	}

	void setupCertificates(java.util.jar.JarEntry entry) {
		this.certificates = entry.getCertificates();
		this.codeSigners = entry.getCodeSigners();
	}

}
