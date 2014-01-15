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

package org.springframework.boot.cli.compiler.grape;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.springframework.util.Assert;

/**
 * A {@link ManagedDependenciesFactory} that uses a properties file to configure the list
 * of managed dependencies that it returns.
 * 
 * @author Andy Wilkinson
 */
class PropertiesManagedDependenciesFactory implements ManagedDependenciesFactory {

	private static final String PROPERTY_SUFFIX_GROUP_ID = ".groupId";

	private static final String PROPERTY_SUFFIX_VERSION = ".version";

	private final List<Dependency> managedDependencies;

	public PropertiesManagedDependenciesFactory() {
		Properties properties = loadProperties();
		this.managedDependencies = getManagedDependencies(properties);
	}

	private static Properties loadProperties() {
		Properties properties = new Properties();
		InputStream inputStream = PropertiesManagedDependenciesFactory.class
				.getClassLoader().getResourceAsStream("META-INF/springcli.properties");
		Assert.state(inputStream != null, "Unable to load springcli properties");
		try {
			properties.load(inputStream);
			return properties;
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to load springcli properties", ex);
		}
	}

	private static List<Dependency> getManagedDependencies(Properties properties) {
		List<Dependency> dependencies = new ArrayList<Dependency>();

		for (Entry<Object, Object> entry : properties.entrySet()) {
			String propertyName = (String) entry.getKey();
			if (propertyName.endsWith(PROPERTY_SUFFIX_GROUP_ID)) {
				String artifactId = propertyName.substring(0, propertyName.length()
						- PROPERTY_SUFFIX_GROUP_ID.length());
				String groupId = (String) entry.getValue();
				String version = properties.getProperty(artifactId
						+ PROPERTY_SUFFIX_VERSION);

				if (version != null) {
					Artifact artifact = new DefaultArtifact(groupId, artifactId, "jar",
							version);
					dependencies.add(new Dependency(artifact, JavaScopes.COMPILE));
				}
			}
		}

		return dependencies;
	}

	@Override
	public List<Dependency> getManagedDependencies() {
		return new ArrayList<Dependency>(this.managedDependencies);
	}

}
