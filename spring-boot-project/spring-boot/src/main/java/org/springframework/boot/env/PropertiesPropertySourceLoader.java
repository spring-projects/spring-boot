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

package org.springframework.boot.env;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.boot.env.OriginTrackedPropertiesLoader.Document;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * Strategy to load '.properties' files into a {@link PropertySource}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 1.0.0
 */
public class PropertiesPropertySourceLoader implements PropertySourceLoader {

	private static final String XML_FILE_EXTENSION = ".xml";

	@Override
	public String[] getFileExtensions() {
		return new String[] { "properties", "xml" };
	}

	@Override
	public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
		List<Map<String, ?>> properties = loadProperties(resource);
		if (properties.isEmpty()) {
			return Collections.emptyList();
		}
		List<PropertySource<?>> propertySources = new ArrayList<>(properties.size());
		for (int i = 0; i < properties.size(); i++) {
			String documentNumber = (properties.size() != 1) ? " (document #" + i + ")" : "";
			propertySources.add(new OriginTrackedMapPropertySource(name + documentNumber,
					Collections.unmodifiableMap(properties.get(i)), true));
		}
		return propertySources;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<Map<String, ?>> loadProperties(Resource resource) throws IOException {
		String filename = resource.getFilename();
		List<Map<String, ?>> result = new ArrayList<>();
		if (filename != null && filename.endsWith(XML_FILE_EXTENSION)) {
			result.add((Map) PropertiesLoaderUtils.loadProperties(resource));
		}
		else {
			List<Document> documents = new OriginTrackedPropertiesLoader(resource).load();
			documents.forEach((document) -> result.add(document.asMap()));
		}
		return result;
	}

}
