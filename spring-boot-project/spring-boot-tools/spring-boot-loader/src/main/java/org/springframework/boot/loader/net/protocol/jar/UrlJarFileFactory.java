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

package org.springframework.boot.loader.net.protocol.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Runtime.Version;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import java.util.jar.JarFile;

import org.springframework.boot.loader.net.protocol.nested.NestedLocation;
import org.springframework.boot.loader.net.util.UrlDecoder;

/**
 * Factory used by {@link UrlJarFiles} to create {@link JarFile} instances.
 *
 * @author Phillip Webb
 * @see UrlJarFile
 * @see UrlNestedJarFile
 */
class UrlJarFileFactory {

	/**
	 * Create a new {@link UrlJarFile} or {@link UrlNestedJarFile} instance.
	 * @param jarFileUrl the jar file URL
	 * @param closeAction the action to call when the file is closed
	 * @return a new {@link JarFile} instance
	 * @throws IOException on I/O error
	 */
	JarFile createJarFile(URL jarFileUrl, Consumer<JarFile> closeAction) throws IOException {
		Runtime.Version version = getVersion(jarFileUrl);
		if (isLocalFileUrl(jarFileUrl)) {
			return createJarFileForLocalFile(jarFileUrl, version, closeAction);
		}
		if (isNestedUrl(jarFileUrl)) {
			return createJarFileForNested(jarFileUrl, version, closeAction);
		}
		return createJarFileForStream(jarFileUrl, version, closeAction);
	}

	private Runtime.Version getVersion(URL url) {
		// The standard JDK handler uses #runtime to indicate that the runtime version
		// should be used. This unfortunately doesn't work for us as
		// jdk.internal.loader.URLClassPath only adds the runtime fragment when the URL
		// is using the internal JDK handler. We need to flip the default to use
		// the runtime version. See gh-38050
		return "base".equals(url.getRef()) ? JarFile.baseVersion() : JarFile.runtimeVersion();
	}

	private boolean isLocalFileUrl(URL url) {
		return url.getProtocol().equalsIgnoreCase("file") && isLocal(url.getHost());
	}

	private boolean isLocal(String host) {
		return host == null || host.isEmpty() || host.equals("~") || host.equalsIgnoreCase("localhost");
	}

	private JarFile createJarFileForLocalFile(URL url, Runtime.Version version, Consumer<JarFile> closeAction)
			throws IOException {
		String path = UrlDecoder.decode(url.getPath());
		return new UrlJarFile(new File(path), version, closeAction);
	}

	private JarFile createJarFileForNested(URL url, Runtime.Version version, Consumer<JarFile> closeAction)
			throws IOException {
		NestedLocation location = NestedLocation.fromUrl(url);
		return new UrlNestedJarFile(location.path().toFile(), location.nestedEntryName(), version, closeAction);
	}

	private JarFile createJarFileForStream(URL url, Version version, Consumer<JarFile> closeAction) throws IOException {
		try (InputStream in = url.openStream()) {
			return createJarFileForStream(in, version, closeAction);
		}
	}

	private JarFile createJarFileForStream(InputStream in, Version version, Consumer<JarFile> closeAction)
			throws IOException {
		Path local = Files.createTempFile("jar_cache", null);
		try {
			Files.copy(in, local, StandardCopyOption.REPLACE_EXISTING);
			JarFile jarFile = new UrlJarFile(local.toFile(), version, closeAction);
			local.toFile().deleteOnExit();
			return jarFile;
		}
		catch (Throwable ex) {
			deleteIfPossible(local, ex);
			throw ex;
		}
	}

	private void deleteIfPossible(Path local, Throwable cause) {
		try {
			Files.delete(local);
		}
		catch (IOException ex) {
			cause.addSuppressed(ex);
		}
	}

	static boolean isNestedUrl(URL url) {
		return url.getProtocol().equalsIgnoreCase("nested");
	}

}
