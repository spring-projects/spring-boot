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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Helper class to build the -cp (classpath) argument of a java process.
 *
 * @author Stephane Nicoll
 * @author Dmytro Nosan
 */
class ClasspathBuilder {

	private final List<URL> urls;

	protected ClasspathBuilder(List<URL> urls) {
		this.urls = urls;
	}

	/**
	 * Builds a classpath string or an argument file representing the classpath, depending
	 * on the operating system.
	 * @param urls an array of {@link URL} representing the elements of the classpath
	 * @return the classpath; on Windows, the path to an argument file is returned,
	 * prefixed with '@'
	 */
	static ClasspathBuilder forURLs(List<URL> urls) {
		return new ClasspathBuilder(new ArrayList<>(urls));
	}

	/**
	 * Builds a classpath string or an argument file representing the classpath, depending
	 * on the operating system.
	 * @param urls an array of {@link URL} representing the elements of the classpath
	 * @return the classpath; on Windows, the path to an argument file is returned,
	 * prefixed with '@'
	 */
	static ClasspathBuilder forURLs(URL... urls) {
		return new ClasspathBuilder(Arrays.asList(urls));
	}

	Classpath build() {
		if (ObjectUtils.isEmpty(this.urls)) {
			return new Classpath("", Collections.emptyList());
		}
		if (this.urls.size() == 1) {
			Path file = toFile(this.urls.get(0));
			return new Classpath(file.toString(), List.of(file));
		}
		List<Path> files = this.urls.stream().map(ClasspathBuilder::toFile).toList();
		String argument = files.stream().map(Object::toString).collect(Collectors.joining(File.pathSeparator));
		if (needsClasspathArgFile()) {
			argument = createArgFile(argument);
		}
		return new Classpath(argument, files);
	}

	protected boolean needsClasspathArgFile() {
		String os = System.getProperty("os.name");
		if (!StringUtils.hasText(os)) {
			return false;
		}
		// Windows limits the maximum command length, so we use an argfile
		return os.toLowerCase(Locale.ROOT).contains("win");
	}

	/**
	 * Create a temporary file with the given {@code} classpath. Return a suitable
	 * argument to load the file, that is the full path prefixed by {@code @}.
	 * @param classpath the classpath to use
	 * @return a suitable argument for the classpath using a file
	 */
	private String createArgFile(String classpath) {
		try {
			return "@" + writeClasspathToFile(classpath);
		}
		catch (IOException ex) {
			return classpath;
		}
	}

	private Path writeClasspathToFile(CharSequence classpath) throws IOException {
		Path tempFile = Files.createTempFile("spring-boot-", ".argfile");
		tempFile.toFile().deleteOnExit();
		Files.writeString(tempFile, "\"" + escape(classpath) + "\"", getCharset());
		return tempFile;
	}

	private static Charset getCharset() {
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

	private static String escape(CharSequence content) {
		return content.toString().replace("\\", "\\\\");
	}

	private static Path toFile(URL url) {
		try {
			return Paths.get(url.toURI());
		}
		catch (URISyntaxException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	static final class Classpath {

		private final String argument;

		private final List<Path> elements;

		private Classpath(String argument, List<Path> elements) {
			this.argument = argument;
			this.elements = elements;
		}

		/**
		 * Return the {@code -cp} argument value.
		 * @return the argument to use
		 */
		String argument() {
			return this.argument;
		}

		/**
		 * Return the classpath elements.
		 * @return the JAR files to use
		 */
		Stream<Path> elements() {
			return this.elements.stream();
		}

	}

}
