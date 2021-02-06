/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.libraries;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import org.springframework.boot.actuate.libraries.Libraries.Builder;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Contributes a pre-computed list of libraries.
 *
 * @author Phil Clay
 * @since 2.5.0
 */
public class BasicLibrariesContributor implements LibrariesContributor {

	private final Libraries libraries;

	public BasicLibrariesContributor(Libraries libraries) {
		Assert.notNull(libraries, "libraries must not be null");
		this.libraries = libraries;
	}

	@Override
	public void contribute(Builder builder) {
		this.libraries.getDetails().forEach(builder::addLibraries);
	}

	/**
	 * Creates and returns a {@link LibrariesContributor} that contributes the libraries
	 * read from the given {@code yamlResource}.
	 *
	 * <p>
	 * The {@code yamlResource} is expected to contain a list of maps. Each map in the
	 * list contains details of a single library.
	 * </p>
	 *
	 * <p>
	 * For example, a yaml resource might contain a list of libraries with maven
	 * coordinates:
	 * </p>
	 *
	 * <pre>
	 * - groupId: group.a
	 *   artifactId: artifact-a
	 *   version: 1.0.0
	 * - groupId: group.b
	 *   artifactId: artifact.b
	 *   version: 2.0.0
	 * </pre>
	 * @param category the category of library to which to contribute the libraries read
	 * from the yaml {@code yamlResource}
	 * @param yamlResource the yaml resource from which to read a list of libraries.
	 * @return a {@link LibrariesContributor} that contributes the libraries read from the
	 * given {@code yamlResource}
	 * @throws IOException if there was a problem reading the yamlResource
	 */
	public static LibrariesContributor fromYamlResource(String category, Resource yamlResource) throws IOException {

		Yaml yaml = new Yaml();
		try (InputStream inputStream = yamlResource.getInputStream()) {
			List<Map<String, Object>> librariesList = yaml.load(inputStream);

			Builder librariesBuilder = new Builder();
			librariesList.forEach((library) -> librariesBuilder.addLibrary(category, library));
			return new BasicLibrariesContributor(librariesBuilder.build());
		}
	}

}
