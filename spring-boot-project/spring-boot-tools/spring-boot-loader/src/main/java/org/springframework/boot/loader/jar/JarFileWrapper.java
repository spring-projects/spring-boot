/*
 * Copyright 2012-2020 the original author or authors.
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
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Permission;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * A wrapper used to create a copy of a {@link JarFile} so that it can be safely closed
 * without closing the original.
 *
 * @author Phillip Webb
 */
class JarFileWrapper extends AbstractJarFile {

	private final JarFile parent;

	JarFileWrapper(JarFile parent) throws IOException {
		super(parent.getRootJarFile().getFile());
		this.parent = parent;
		super.close();
	}

	@Override
	URL getUrl() throws MalformedURLException {
		return this.parent.getUrl();
	}

	@Override
	JarFileType getType() {
		return this.parent.getType();
	}

	@Override
	Permission getPermission() {
		return this.parent.getPermission();
	}

	@Override
	public Manifest getManifest() throws IOException {
		return this.parent.getManifest();
	}

	@Override
	public Enumeration<JarEntry> entries() {
		return this.parent.entries();
	}

	@Override
	public Stream<JarEntry> stream() {
		return this.parent.stream();
	}

	@Override
	public JarEntry getJarEntry(String name) {
		return this.parent.getJarEntry(name);
	}

	@Override
	public ZipEntry getEntry(String name) {
		return this.parent.getEntry(name);
	}

	@Override
	InputStream getInputStream() throws IOException {
		return this.parent.getInputStream();
	}

	@Override
	public synchronized InputStream getInputStream(ZipEntry ze) throws IOException {
		return this.parent.getInputStream(ze);
	}

	@Override
	public String getComment() {
		return this.parent.getComment();
	}

	@Override
	public int size() {
		return this.parent.size();
	}

	@Override
	public String toString() {
		return this.parent.toString();
	}

	@Override
	public String getName() {
		return this.parent.getName();
	}

	static JarFile unwrap(java.util.jar.JarFile jarFile) {
		if (jarFile instanceof JarFile) {
			return (JarFile) jarFile;
		}
		if (jarFile instanceof JarFileWrapper) {
			return unwrap(((JarFileWrapper) jarFile).parent);
		}
		throw new IllegalStateException("Not a JarFile or Wrapper");
	}

}
