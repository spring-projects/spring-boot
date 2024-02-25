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

package org.springframework.boot.buildpack.platform.docker.type;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.boot.buildpack.platform.json.MappedObject;

/**
 * Image configuration information.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 2.3.0
 */
public class ImageConfig extends MappedObject {

	private final Map<String, String> labels;

	private final Map<String, String> configEnv;

	/**
	 * Constructs a new ImageConfig object with the provided JSON node.
	 * @param node the JSON node containing the image configuration data
	 */
	ImageConfig(JsonNode node) {
		super(node, MethodHandles.lookup());
		this.labels = extractLabels();
		this.configEnv = parseConfigEnv();
	}

	/**
	 * Extracts the labels from the ImageConfig object.
	 * @return a Map containing the labels extracted from the ImageConfig object
	 */
	@SuppressWarnings("unchecked")
	private Map<String, String> extractLabels() {
		Map<String, String> labels = valueAt("/Labels", Map.class);
		if (labels == null) {
			return Collections.emptyMap();
		}
		return labels;
	}

	/**
	 * Parses the configuration environment and returns a map of key-value pairs. The
	 * configuration environment is retrieved from the specified path "/Env". If the
	 * configuration environment is not found, an empty map is returned.
	 * @return A map containing the parsed configuration environment. The keys are the
	 * names of the environment variables, and the values are their corresponding values.
	 * The map is unmodifiable.
	 */
	private Map<String, String> parseConfigEnv() {
		String[] entries = valueAt("/Env", String[].class);
		if (entries == null) {
			return Collections.emptyMap();
		}
		Map<String, String> env = new LinkedHashMap<>();
		for (String entry : entries) {
			int i = entry.indexOf('=');
			String name = (i != -1) ? entry.substring(0, i) : entry;
			String value = (i != -1) ? entry.substring(i + 1) : null;
			env.put(name, value);
		}
		return Collections.unmodifiableMap(env);
	}

	/**
	 * Returns a deep copy of the JsonNode object.
	 * @return a deep copy of the JsonNode object
	 */
	JsonNode getNodeCopy() {
		return super.getNode().deepCopy();
	}

	/**
	 * Return the image labels. If the image has no labels, an empty {@code Map} is
	 * returned.
	 * @return the image labels, never {@code null}
	 */
	public Map<String, String> getLabels() {
		return this.labels;
	}

	/**
	 * Return the image environment variables. If the image has no environment variables,
	 * an empty {@code Map} is returned.
	 * @return the env, never {@code null}
	 */
	public Map<String, String> getEnv() {
		return this.configEnv;
	}

	/**
	 * Create an updated copy of this image config.
	 * @param update consumer to apply updates
	 * @return an updated image config
	 */
	public ImageConfig copy(Consumer<Update> update) {
		return new Update(this).run(update);

	}

	/**
	 * Update class used to change data when creating a copy.
	 */
	public static final class Update {

		private final ObjectNode copy;

		/**
		 * Updates the image configuration by making a deep copy of the source image
		 * configuration.
		 * @param source the image configuration to be updated
		 */
		private Update(ImageConfig source) {
			this.copy = source.getNode().deepCopy();
		}

		/**
		 * Runs the update function and returns a new ImageConfig object.
		 * @param update the update function to be executed
		 * @return a new ImageConfig object with the updated values
		 */
		private ImageConfig run(Consumer<Update> update) {
			update.accept(this);
			return new ImageConfig(this.copy);
		}

		/**
		 * Update the image config with an additional label.
		 * @param label the label name
		 * @param value the label value
		 */
		public void withLabel(String label, String value) {
			JsonNode labels = this.copy.at("/Labels");
			if (labels.isMissingNode()) {
				labels = this.copy.putObject("Labels");
			}
			((ObjectNode) labels).put(label, value);
		}

	}

}
