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

package org.springframework.boot.loader.tools;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;

/**
 * A {@code BundledLibrariesWriter} writes the {@value #BUNDLED_LIBRARIES_FILE_NAME} for
 * consumption by the Actuator.
 *
 * @author Phil Clay
 * @since 2.5.0
 */
public final class BundledLibrariesWriter {

	/**
	 * Name of the file containing the libraries bundled in the application archive.
	 */
	public static final String BUNDLED_LIBRARIES_FILE_NAME = "bundled-libraries.yaml";

	private static final String GROUP_ID = "groupId";

	private static final String ARTIFACT_ID = "artifactId";

	private static final String VERSION = "version";

	/**
	 * Writes details of the given libraries in YAML format to the given output stream.
	 * @param libraries the libraries for which to write details
	 * @param outputStream the output stream to which to write the details of the given
	 * libraries in YAML format
	 * @throws IOException if a problem occurs when writing to the given output stream
	 */
	public void writeBundledLibraries(Collection<Library> libraries, OutputStream outputStream) throws IOException {
		List<Map<String, String>> libraryMaps = createLibraryMaps(libraries);
		writeLibraryMaps(libraryMaps, outputStream);
	}

	private List<Map<String, String>> createLibraryMaps(Collection<Library> libraries) {
		return libraries.stream().map(this::convertLibraryToMap).filter(Objects::nonNull)
				.sorted(Comparator.<Map<String, String>, String>comparing((libraryMap) -> libraryMap.get(GROUP_ID))
						.thenComparing((libraryMap) -> libraryMap.get(ARTIFACT_ID))
						.thenComparing((libraryMap) -> libraryMap.get(VERSION)))
				.collect(Collectors.toList());
	}

	private Map<String, String> convertLibraryToMap(Library library) {
		LibraryCoordinates coordinates = library.getCoordinates();
		if (coordinates == null) {
			return null;
		}
		Map<String, String> libraryMap = new LinkedHashMap<>();
		libraryMap.put(GROUP_ID, coordinates.getGroupId());
		libraryMap.put(ARTIFACT_ID, coordinates.getArtifactId());
		libraryMap.put(VERSION, coordinates.getVersion());
		return libraryMap;
	}

	private void writeLibraryMaps(List<Map<String, String>> libraryMaps, OutputStream outputStream) throws IOException {
		DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setPrettyFlow(true);
		dumperOptions.setDefaultFlowStyle(FlowStyle.BLOCK);

		Yaml yaml = new Yaml(dumperOptions);
		try (Writer writer = new OutputStreamWriter(outputStream)) {
			writer.write(yaml.dump(libraryMaps));
		}
	}

}
