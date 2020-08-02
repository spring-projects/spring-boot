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

package org.springframework.boot.loader.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link Library} implementation for internal jarmode jars.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public class JarModeLibrary extends Library {

	/**
	 * {@link JarModeLibrary} for layer tools.
	 */
	public static final JarModeLibrary LAYER_TOOLS = new JarModeLibrary("spring-boot-jarmode-layertools");

	JarModeLibrary(String artifactId) {
		this(createCoordinates(artifactId));
	}

	public JarModeLibrary(LibraryCoordinates coordinates) {
		super(getJarName(coordinates), null, LibraryScope.RUNTIME, coordinates, false);
	}

	private static LibraryCoordinates createCoordinates(String artifactId) {
		String version = JarModeLibrary.class.getPackage().getImplementationVersion();
		return LibraryCoordinates.of("org.springframework.boot", artifactId, version);
	}

	private static String getJarName(LibraryCoordinates coordinates) {
		String version = coordinates.getVersion();
		StringBuilder jarName = new StringBuilder(coordinates.getArtifactId());
		if (StringUtils.hasText(version)) {
			jarName.append('-');
			jarName.append(version);
		}
		jarName.append(".jar");
		return jarName.toString();
	}

	@Override
	public InputStream openStream() throws IOException {
		String path = "META-INF/jarmode/" + getCoordinates().getArtifactId() + ".jar";
		URL resource = getClass().getClassLoader().getResource(path);
		Assert.state(resource != null, () -> "Unable to find resource " + path);
		return resource.openStream();
	}

	@Override
	long getLastModified() {
		return 0L;
	}

	@Override
	public File getFile() {
		throw new UnsupportedOperationException("Unable to access jar mode library file");
	}

}
