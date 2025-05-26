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

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;

/**
 * Helper class to iterate entries in a jar file and check that content matches a related
 * entry.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class JarEntriesStream implements Closeable {

	private static final int BUFFER_SIZE = 4 * 1024;

	private final JarInputStream in;

	private final byte[] inBuffer = new byte[BUFFER_SIZE];

	private final byte[] compareBuffer = new byte[BUFFER_SIZE];

	private final Inflater inflater = new Inflater(true);

	private JarEntry entry;

	JarEntriesStream(InputStream in) throws IOException {
		this.in = new JarInputStream(in);
	}

	JarEntry getNextEntry() throws IOException {
		this.entry = this.in.getNextJarEntry();
		this.inflater.reset();
		return this.entry;
	}

	boolean matches(boolean directory, int size, int compressionMethod, InputStreamSupplier streamSupplier)
			throws IOException {
		if (this.entry.isDirectory() != directory) {
			fail("directory");
		}
		if (this.entry.getMethod() != compressionMethod) {
			fail("compression method");
		}
		if (this.entry.isDirectory()) {
			this.in.closeEntry();
			return true;
		}
		try (DataInputStream expected = new DataInputStream(getInputStream(size, streamSupplier))) {
			assertSameContent(expected);
		}
		return true;
	}

	private InputStream getInputStream(int size, InputStreamSupplier streamSupplier) throws IOException {
		InputStream inputStream = streamSupplier.get();
		return (this.entry.getMethod() != ZipEntry.DEFLATED) ? inputStream
				: new ZipInflaterInputStream(inputStream, this.inflater, size);
	}

	private void assertSameContent(DataInputStream expected) throws IOException {
		int len;
		while ((len = this.in.read(this.inBuffer)) > 0) {
			try {
				expected.readFully(this.compareBuffer, 0, len);
				if (Arrays.equals(this.inBuffer, 0, len, this.compareBuffer, 0, len)) {
					continue;
				}
			}
			catch (EOFException ex) {
				// Continue and throw exception due to mismatched content length.
			}
			fail("content");
		}
		if (expected.read() != -1) {
			fail("content");
		}
	}

	private void fail(String check) {
		throw new IllegalStateException("Content mismatch when reading security info for entry '%s' (%s check)"
			.formatted(this.entry.getName(), check));
	}

	@Override
	public void close() throws IOException {
		this.inflater.end();
		this.in.close();
	}

	@FunctionalInterface
	interface InputStreamSupplier {

		InputStream get() throws IOException;

	}

}
