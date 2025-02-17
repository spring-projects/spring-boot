/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.maven;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * Utility class that represents `argument` as a file. Mostly used to avoid `Path too long
 * on ...` on Windows.
 *
 * @author Moritz Halbritter
 * @author Dmytro Nosan
 */
final class ArgFile {

	private final Path path;

	private ArgFile(Path path) {
		this.path = path.toAbsolutePath();
	}

	/**
	 * Creates a new {@code ArgFile} with the given content.
	 * @param content the content to write to the argument file
	 * @return a new {@code ArgFile}
	 * @throws IOException if an I/O error occurs
	 */
	static ArgFile create(CharSequence content) throws IOException {
		Path tempFile = Files.createTempFile("spring-boot-", ".argfile");
		tempFile.toFile().deleteOnExit();
		ArgFile argFile = new ArgFile(tempFile);
		argFile.write(content);
		return argFile;
	}

	private void write(CharSequence content) throws IOException {
		Files.writeString(this.path, "\"" + escape(content) + "\"", getCharset());
	}

	private Charset getCharset() {
		String nativeEncoding = System.getProperty("native.encoding");
		if (nativeEncoding == null) {
			return Charset.defaultCharset();
		}
		try {
			return Charset.forName(nativeEncoding);
		}
		catch (UnsupportedCharsetException ex) {
			return Charset.defaultCharset();
		}
	}

	private String escape(CharSequence content) {
		return content.toString().replace("\\", "\\\\");
	}

	@Override
	public String toString() {
		return this.path.toString();
	}

}
