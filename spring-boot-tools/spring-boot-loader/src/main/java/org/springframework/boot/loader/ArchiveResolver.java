/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.loader;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.security.ProtectionDomain;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;

/**
 * Resolves the {@link Archive} from which a {@link Class} was loaded.
 * 
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
public class ArchiveResolver {

	/**
	 * Resolves the {@link Archive} that contains the given {@code clazz}.
	 * @param clazz The class whose containing archive is to be resolved
	 * 
	 * @return The class's containing archive
	 * @throws IOException if an error occurs when resolving the containing archive
	 */
	public Archive resolveArchive(Class<?> clazz) throws IOException {
		File root = resolveArchiveLocation(clazz);
		return (root.isDirectory() ? new ExplodedArchive(root) : new JarFileArchive(root));
	}

	/**
	 * Resolves the location of the archive that contains the given {@code clazz}.
	 * @param clazz The class for which the location of the containing archive is to be
	 * resolved
	 * 
	 * @return The location of the class's containing archive
	 * @throws IOException if an error occurs when resolving the containing archive's
	 * location
	 */
	public File resolveArchiveLocation(Class<?> clazz) throws IOException {
		ProtectionDomain protectionDomain = getClass().getProtectionDomain();
		CodeSource codeSource = protectionDomain.getCodeSource();

		if (codeSource != null) {
			File root;
			URL location = codeSource.getLocation();
			URLConnection connection = location.openConnection();
			if (connection instanceof JarURLConnection) {
				root = new File(((JarURLConnection) connection).getJarFile().getName());
			}
			else {
				root = new File(location.getPath());
			}

			if (!root.exists()) {
				throw new IllegalStateException(
						"Unable to determine code source archive from " + root);
			}
			return root;
		}
		throw new IllegalStateException("Unable to determine code source archive");
	}
}
