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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

/**
 * Encapsulates a class path and allows argument parameters to be created. On Windows an
 * argument file is used whenever possible since the maximum command line length is
 * limited.
 *
 * @author Stephane Nicoll
 * @author Dmytro Nosan
 * @author Phillip Webb
 */
final class ClassPath {

	private static final Collector<CharSequence, ?, String> JOIN_BY_PATH_SEPARATOR = Collectors
		.joining(File.pathSeparator);

	private final boolean preferArgFile;

	private final String path;

	private ClassPath(boolean preferArgFile, String path) {
		this.preferArgFile = preferArgFile;
		this.path = path;
	}

	/**
	 * Return the args to append to a java command line call (including {@code -cp}).
	 * @param allowArgFile if an arg file can be used
	 * @return the command line arguments
	 */
	List<String> args(boolean allowArgFile) {
		return (!this.path.isEmpty()) ? List.of("-cp", classPathArg(allowArgFile)) : Collections.emptyList();
	}

	private String classPathArg(boolean allowArgFile) {
		if (this.preferArgFile && allowArgFile) {
			try {
				return "@" + createArgFile();
			}
			catch (IOException ex) {
				return this.path;
			}
		}
		return this.path;
	}

	@Override
	public String toString() {
		return this.path;
	}

	private Path createArgFile() throws IOException {
		Path argFile = Files.createTempFile("spring-boot-", ".argfile");
		argFile.toFile().deleteOnExit();
		Files.writeString(argFile, "\"" + this.path.replace("\\", "\\\\") + "\"", charset());
		return argFile;
	}

	private Charset charset() {
		try {
			String nativeEncoding = System.getProperty("native.encoding");
			return (nativeEncoding != null) ? Charset.forName(nativeEncoding) : Charset.defaultCharset();
		}
		catch (UnsupportedCharsetException ex) {
			return Charset.defaultCharset();
		}
	}

	/**
	 * Factory method to create a {@link ClassPath} of the given URLs.
	 * @param urls the class path URLs
	 * @return a new {@link ClassPath} instance
	 */
	static ClassPath of(URL... urls) {
		return of(Arrays.asList(urls));
	}

	/**
	 * Factory method to create a {@link ClassPath} of the given URLs.
	 * @param urls the class path URLs
	 * @return a new {@link ClassPath} instance
	 */
	static ClassPath of(List<URL> urls) {
		return of(System::getProperty, urls);
	}

	/**
	 * Factory method to create a {@link ClassPath} of the given URLs.
	 * @param getSystemProperty {@link UnaryOperator} allowing access to system properties
	 * @param urls the class path URLs
	 * @return a new {@link ClassPath} instance
	 */
	static ClassPath of(UnaryOperator<String> getSystemProperty, List<URL> urls) {
		boolean preferArgFile = urls.size() > 1 && isWindows(getSystemProperty);
		return new ClassPath(preferArgFile, urls.stream().map(ClassPath::toPathString).collect(JOIN_BY_PATH_SEPARATOR));
	}

	private static boolean isWindows(UnaryOperator<String> getSystemProperty) {
		String os = getSystemProperty.apply("os.name");
		return StringUtils.hasText(os) && os.toLowerCase(Locale.ROOT).contains("win");
	}

	private static String toPathString(URL url) {
		try {
			return Paths.get(url.toURI()).toString();
		}
		catch (URISyntaxException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

}
