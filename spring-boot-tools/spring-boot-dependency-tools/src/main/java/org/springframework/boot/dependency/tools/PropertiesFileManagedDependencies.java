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

package org.springframework.boot.dependency.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * {@link ManagedDependencies} backed by an external properties file (of the form created
 * by the Spring IO platform). The property key should be the groupID and versionId (in
 * the form {@literal group:version}) and the value should be the version.
 * 
 * @author Phillip Webb
 * @since 1.1.0
 */
public class PropertiesFileManagedDependencies extends AbstractManagedDependencies {

	/**
	 * Create a new {@link PropertiesFileManagedDependencies} instance from the specified
	 * input stream.
	 * @param inputStream source input stream (will be closed when properties have been
	 * loaded)
	 * @throws IOException
	 */
	public PropertiesFileManagedDependencies(InputStream inputStream) throws IOException {
		try {
			Properties properties = new Properties();
			properties.load(inputStream);
			initialize(properties);
		}
		finally {
			inputStream.close();
		}
	}

	private void initialize(Properties properties) {
		Map<String, String> sortedMap = new TreeMap<String, String>();
		for (Map.Entry<Object, Object> entry : properties.entrySet()) {
			sortedMap.put(entry.getKey().toString(), entry.getValue().toString());
		}
		for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
			ArtifactAndGroupId artifactAndGroupId = parse(entry.getKey());
			Dependency dependency = artifactAndGroupId.newDependency(entry.getValue());
			add(artifactAndGroupId, dependency);
		}
	}

	private ArtifactAndGroupId parse(String value) {
		String[] parts = value.split("\\:");
		if (parts.length != 2) {
			throw new IllegalStateException("Unable to parse " + value);
		}
		return new ArtifactAndGroupId(parts[0], parts[1]);
	}

}
