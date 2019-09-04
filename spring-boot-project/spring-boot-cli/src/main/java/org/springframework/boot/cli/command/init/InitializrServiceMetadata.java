/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.cli.command.init;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Define the metadata available for a particular service instance.
 *
 * @author Stephane Nicoll
 */
class InitializrServiceMetadata {

	private static final String DEPENDENCIES_EL = "dependencies";

	private static final String TYPE_EL = "type";

	private static final String VALUES_EL = "values";

	private static final String NAME_ATTRIBUTE = "name";

	private static final String ID_ATTRIBUTE = "id";

	private static final String DESCRIPTION_ATTRIBUTE = "description";

	private static final String ACTION_ATTRIBUTE = "action";

	private static final String DEFAULT_ATTRIBUTE = "default";

	private final Map<String, Dependency> dependencies;

	private final MetadataHolder<String, ProjectType> projectTypes;

	private final Map<String, String> defaults;

	/**
	 * Creates a new instance using the specified root {@link JSONObject}.
	 * @param root the root JSONObject
	 * @throws JSONException on JSON parsing failure
	 */
	InitializrServiceMetadata(JSONObject root) throws JSONException {
		this.dependencies = parseDependencies(root);
		this.projectTypes = parseProjectTypes(root);
		this.defaults = Collections.unmodifiableMap(parseDefaults(root));
	}

	InitializrServiceMetadata(ProjectType defaultProjectType) {
		this.dependencies = new HashMap<>();
		this.projectTypes = new MetadataHolder<>();
		this.projectTypes.getContent().put(defaultProjectType.getId(), defaultProjectType);
		this.projectTypes.setDefaultItem(defaultProjectType);
		this.defaults = new HashMap<>();
	}

	/**
	 * Return the dependencies supported by the service.
	 * @return the supported dependencies
	 */
	Collection<Dependency> getDependencies() {
		return this.dependencies.values();
	}

	/**
	 * Return the dependency with the specified id or {@code null} if no such dependency
	 * exists.
	 * @param id the id
	 * @return the dependency or {@code null}
	 */
	Dependency getDependency(String id) {
		return this.dependencies.get(id);
	}

	/**
	 * Return the project types supported by the service.
	 * @return the supported project types
	 */
	Map<String, ProjectType> getProjectTypes() {
		return this.projectTypes.getContent();
	}

	/**
	 * Return the default type to use or {@code null} if the metadata does not define any
	 * default.
	 * @return the default project type or {@code null}
	 */
	ProjectType getDefaultType() {
		if (this.projectTypes.getDefaultItem() != null) {
			return this.projectTypes.getDefaultItem();
		}
		String defaultTypeId = getDefaults().get("type");
		if (defaultTypeId != null) {
			return this.projectTypes.getContent().get(defaultTypeId);
		}
		return null;
	}

	/**
	 * Returns the defaults applicable to the service.
	 * @return the defaults of the service
	 */
	Map<String, String> getDefaults() {
		return this.defaults;
	}

	private Map<String, Dependency> parseDependencies(JSONObject root) throws JSONException {
		Map<String, Dependency> result = new HashMap<>();
		if (!root.has(DEPENDENCIES_EL)) {
			return result;
		}
		JSONObject dependencies = root.getJSONObject(DEPENDENCIES_EL);
		JSONArray array = dependencies.getJSONArray(VALUES_EL);
		for (int i = 0; i < array.length(); i++) {
			JSONObject group = array.getJSONObject(i);
			parseGroup(group, result);
		}
		return result;
	}

	private MetadataHolder<String, ProjectType> parseProjectTypes(JSONObject root) throws JSONException {
		MetadataHolder<String, ProjectType> result = new MetadataHolder<>();
		if (!root.has(TYPE_EL)) {
			return result;
		}
		JSONObject type = root.getJSONObject(TYPE_EL);
		JSONArray array = type.getJSONArray(VALUES_EL);
		String defaultType = (type.has(DEFAULT_ATTRIBUTE) ? type.getString(DEFAULT_ATTRIBUTE) : null);
		for (int i = 0; i < array.length(); i++) {
			JSONObject typeJson = array.getJSONObject(i);
			ProjectType projectType = parseType(typeJson, defaultType);
			result.getContent().put(projectType.getId(), projectType);
			if (projectType.isDefaultType()) {
				result.setDefaultItem(projectType);
			}
		}
		return result;
	}

	private Map<String, String> parseDefaults(JSONObject root) throws JSONException {
		Map<String, String> result = new HashMap<>();
		Iterator<?> keys = root.keys();
		while (keys.hasNext()) {
			String key = (String) keys.next();
			Object o = root.get(key);
			if (o instanceof JSONObject) {
				JSONObject child = (JSONObject) o;
				if (child.has(DEFAULT_ATTRIBUTE)) {
					result.put(key, child.getString(DEFAULT_ATTRIBUTE));
				}
			}
		}
		return result;
	}

	private void parseGroup(JSONObject group, Map<String, Dependency> dependencies) throws JSONException {
		if (group.has(VALUES_EL)) {
			JSONArray content = group.getJSONArray(VALUES_EL);
			for (int i = 0; i < content.length(); i++) {
				Dependency dependency = parseDependency(content.getJSONObject(i));
				dependencies.put(dependency.getId(), dependency);
			}
		}
	}

	private Dependency parseDependency(JSONObject object) throws JSONException {
		String id = getStringValue(object, ID_ATTRIBUTE, null);
		String name = getStringValue(object, NAME_ATTRIBUTE, null);
		String description = getStringValue(object, DESCRIPTION_ATTRIBUTE, null);
		return new Dependency(id, name, description);
	}

	private ProjectType parseType(JSONObject object, String defaultId) throws JSONException {
		String id = getStringValue(object, ID_ATTRIBUTE, null);
		String name = getStringValue(object, NAME_ATTRIBUTE, null);
		String action = getStringValue(object, ACTION_ATTRIBUTE, null);
		boolean defaultType = id.equals(defaultId);
		Map<String, String> tags = new HashMap<>();
		if (object.has("tags")) {
			JSONObject jsonTags = object.getJSONObject("tags");
			tags.putAll(parseStringItems(jsonTags));
		}
		return new ProjectType(id, name, action, defaultType, tags);
	}

	private String getStringValue(JSONObject object, String name, String defaultValue) throws JSONException {
		return object.has(name) ? object.getString(name) : defaultValue;
	}

	private Map<String, String> parseStringItems(JSONObject json) throws JSONException {
		Map<String, String> result = new HashMap<>();
		for (Iterator<?> iterator = json.keys(); iterator.hasNext();) {
			String key = (String) iterator.next();
			Object value = json.get(key);
			if (value instanceof String) {
				result.put(key, (String) value);
			}
		}
		return result;
	}

	private static final class MetadataHolder<K, T> {

		private final Map<K, T> content;

		private T defaultItem;

		private MetadataHolder() {
			this.content = new HashMap<>();
		}

		Map<K, T> getContent() {
			return this.content;
		}

		T getDefaultItem() {
			return this.defaultItem;
		}

		void setDefaultItem(T defaultItem) {
			this.defaultItem = defaultItem;
		}

	}

}
