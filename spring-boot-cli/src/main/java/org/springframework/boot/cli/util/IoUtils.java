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

package org.springframework.boot.cli.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;

/**
 * General IO utility methods
 * 
 * @author Andy Wilkinson
 */
public class IoUtils {

	private IoUtils() {

	}

	/**
	 * Reads the entire contents of the resource referenced by {@code uri} and returns
	 * them as a String.
	 * @param uri The resource to read
	 * @return The contents of the resource
	 */
	public static String readEntirely(String uri) {
		try {
			InputStream stream = URI.create(uri).toURL().openStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line;
			StringBuilder result = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				result.append(line);
			}
			return result.toString();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Copies the data read from {@code input} into {@code output}.
	 * <p>
	 * <strong>Note:</strong> it is the caller's responsibility to close the streams
	 * @param input The stream to read data from
	 * @param output The stream to write data to
	 * @throws IOException if the copy fails
	 */
	public static void copy(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[4096];
		int read;
		while ((read = input.read(buffer)) >= 0) {
			output.write(buffer, 0, read);
		}
	}

	/**
	 * Quietly closes the given {@link Closeables Closeable}. Any exceptions thrown by
	 * {@link Closeable#close() close()} are swallowed. Any {@code null}
	 * {@code Closeables} are ignored.
	 * @param closeables The {@link Closeable closeables} to close
	 */
	public static void closeQuietly(Closeable... closeables) {
		for (Closeable closeable : closeables) {
			if (closeable != null) {
				try {
					closeable.close();
				}
				catch (IOException ioe) {
					// Closing quietly
				}
			}
		}

	}
}
