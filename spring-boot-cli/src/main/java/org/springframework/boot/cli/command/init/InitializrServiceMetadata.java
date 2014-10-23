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

package org.springframework.boot.cli.command.init;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Define the metadata available for a particular service instance.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
class InitializrServiceMetadata {

	private static final String DEPENDENCIES_EL = "dependencies";

	private static final String TYPES_EL = "types";

	private static final String DEFAULTS_EL = "defaults";

	private static final String CONTENT_EL = "content";

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
	 */
	InitializrServiceMetadata(JSONObject root) {
		this.dependencies = parseDependencies(root);
		this.projectTypes = parseProjectTypes(root);
		this.defaults = Collections.unmodifiableMap(parseDefaults(root));
	}

	InitializrServiceMetadata(ProjectType defaultProjectType) {
		this.dependencies = new HashMap<String, Dependency>();
		this.projectTypes = new MetadataHolder<String, ProjectType>();
		this.projectTypes.getContent().put(defaultProjectType.getId(), defaultProjectType);
		this.projectTypes.setDefaultItem(defaultProjectType);
		this.defaults = new HashMap<String, String>();
	}

	/**
	 * Return the dependencies supported by the service.
	 */
	public Collection<Dependency> getDependencies() {
		return dependencies.values();
	}

	/**
	 * Return the dependency with the specified id or {@code null} if no
	 * such dependency exists.
	 */
	public Dependency getDependency(String id) {
		return dependencies.get(id);
	}

	/**
	 * Return the project types supported by the service.
	 */
	public Map<String, ProjectType> getProjectTypes() {
		return projectTypes.getContent();
	}

	/**
	 * Return the default type to use or {@code null} or the metadata does
	 * not define any default.
	 */
	public ProjectType getDefaultType() {
		if (projectTypes.getDefaultItem() != null) {
			return projectTypes.getDefaultItem();
		}
		String defaultTypeId = getDefaults().get("type");
		if (defaultTypeId != null) {
			return projectTypes.getContent().get(defaultTypeId);
		}
		return null;
	}

	/**
	 * Returns the defaults applicable to the service.
	 */
	public Map<String, String> getDefaults() {
		return defaults;
	}

	private Map<String, Dependency> parseDependencies(JSONObject root) {
		Map<String, Dependency> result = new HashMap<String, Dependency>();
		if (!root.has(DEPENDENCIES_EL)) {
			return result;
		}
		JSONArray array = root.getJSONArray(DEPENDENCIES_EL);
		for (int i = 0; i < array.length(); i++) {
			JSONObject group = array.getJSONObject(i);
			parseGroup(group, result);
		}
		return result;
	}

	private MetadataHolder<String, ProjectType> parseProjectTypes(JSONObject root) {
		MetadataHolder<String, ProjectType> result = new MetadataHolder<String, ProjectType>();
		if (!root.has(TYPES_EL)) {
			return result;
		}
		JSONArray array = root.getJSONArray(TYPES_EL);
		for (int i = 0; i < array.length(); i++) {
			JSONObject typeJson = array.getJSONObject(i);
			ProjectType projectType = parseType(typeJson);
			result.getContent().put(projectType.getId(), projectType);
			if (projectType.isDefaultType()) {
				result.setDefaultItem(projectType);
			}
		}
		return result;
	}

	private Map<String, String> parseDefaults(JSONObject root) {
		Map<String, String> result = new HashMap<String, String>();
		if (!root.has(DEFAULTS_EL)) {
			return result;
		}
		JSONObject defaults = root.getJSONObject(DEFAULTS_EL);
		result.putAll(parseStringItems(defaults));
		return result;
	}

	private void parseGroup(JSONObject group, Map<String, Dependency> dependencies) {
		if (group.has(CONTENT_EL)) {
			JSONArray content = group.getJSONArray(CONTENT_EL);
			for (int i = 0; i < content.length(); i++) {
				Dependency dependency = parseDependency(content.getJSONObject(i));
				dependencies.put(dependency.getId(), dependency);
			}
		}
	}

	private Dependency parseDependency(JSONObject object) {
		Dependency dependency = new Dependency();
		dependency.setName(getStringValue(object, NAME_ATTRIBUTE, null));
		dependency.setId(getStringValue(object, ID_ATTRIBUTE, null));
		dependency.setDescription(getStringValue(object, DESCRIPTION_ATTRIBUTE, null));
		return dependency;
	}

	private ProjectType parseType(JSONObject object) {
		String id = getStringValue(object, ID_ATTRIBUTE, null);
		String name = getStringValue(object, NAME_ATTRIBUTE, null);
		String action = getStringValue(object, ACTION_ATTRIBUTE, null);
		boolean defaultType = getBooleanValue(object, DEFAULT_ATTRIBUTE, false);
		Map<String, String> tags = new HashMap<String, String>();
		if (object.has("tags")) {
			JSONObject jsonTags = object.getJSONObject("tags");
			tags.putAll(parseStringItems(jsonTags));
		}
		return new ProjectType(id, name, action, defaultType, tags);
	}

	private String getStringValue(JSONObject object, String name, String defaultValue) {
		return object.has(name) ? object.getString(name) : defaultValue;
	}

	private boolean getBooleanValue(JSONObject object, String name, boolean defaultValue) {
		return object.has(name) ? object.getBoolean(name) : defaultValue;
	}

	private Map<String, String> parseStringItems(JSONObject json) {
		Map<String, String> result = new HashMap<String, String>();
		for (Object k : json.keySet()) {
			String key = (String) k;
			Object value = json.get(key);
			if (value instanceof String) {
				result.put(key, (String) value);
			}
		}
		return result;
	}

	private static class MetadataHolder<K, T> {

		private final Map<K, T> content;

		private T defaultItem;

		private MetadataHolder() {
			this.content = new HashMap<K, T>();
		}

		public Map<K, T> getContent() {
			return content;
		}

		public T getDefaultItem() {
			return defaultItem;
		}

		public void setDefaultItem(T defaultItem) {
			this.defaultItem = defaultItem;
		}
	}

}
