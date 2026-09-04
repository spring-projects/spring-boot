/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.docker.compose.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses simple YAML output from podman-compose config.
 *
 * @author Somil Jain
 */
final class PodmanComposeConfigYamlParser {

	private PodmanComposeConfigYamlParser() {
	}

	static DockerCliComposeConfigResponse parse(String yaml) {
		String name = "";
		Map<String, DockerCliComposeConfigResponse.Service> services = new HashMap<>();

		String currentService = null;
		boolean inServices = false;
		int servicesIndent = -1;

		for (String line : yaml.split("\\r?\\n")) {
			if (line.trim().isEmpty() || line.trim().startsWith("#")) {
				continue;
			}
			int indent = getIndent(line);
			String trimmed = line.trim();

			if (indent == 0) {
				inServices = false;
				if (trimmed.startsWith("name:")) {
					name = extractValue(trimmed, "name:");
				}
				else if (trimmed.startsWith("services:")) {
					inServices = true;
				}
			}
			else if (inServices) {
				if (servicesIndent == -1) {
					servicesIndent = indent;
				}
				if (indent == servicesIndent && trimmed.endsWith(":")) {
					currentService = trimmed.substring(0, trimmed.length() - 1);
				}
				else if (currentService != null && trimmed.startsWith("image:")) {
					String image = extractValue(trimmed, "image:");
					services.put(currentService, new DockerCliComposeConfigResponse.Service(image));
				}
			}
		}

		return new DockerCliComposeConfigResponse(name, services);
	}

	private static int getIndent(String line) {
		int i = 0;
		while (i < line.length() && line.charAt(i) == ' ') {
			i++;
		}
		return i;
	}

	private static String extractValue(String line, String prefix) {
		String value = line.substring(prefix.length()).trim();
		if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
			return value.substring(1, value.length() - 1);
		}
		return value;
	}

}
