/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.cli.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.springframework.util.Assert;

/**
 * {@link ArtifactCoordinatesResolver} backed by a properties file.
 * 
 * @author Andy Wilkinson
 */
public final class PropertiesArtifactCoordinatesResolver implements
		ArtifactCoordinatesResolver {

	private final ClassLoader loader;

	private Properties properties = null;

	public PropertiesArtifactCoordinatesResolver(ClassLoader loader) {
		this.loader = loader;
	}

	@Override
	public String getGroupId(String artifactId) {
		return getProperty(artifactId + ".groupId");
	}

	@Override
	public String getVersion(String artifactId) {
		return getProperty(artifactId + ".version");
	}

	private String getProperty(String name) {
		if (this.properties == null) {
			this.properties = loadProperties();
		}
		String property = this.properties.getProperty(name);
		return property;
	}

	private Properties loadProperties() {
		Properties properties = new Properties();
		InputStream inputStream = this.loader
				.getResourceAsStream("META-INF/springcli.properties");
		Assert.state(inputStream != null, "Unable to load springcli properties");
		try {
			properties.load(inputStream);
			return properties;
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to load springcli properties", ex);
		}
	}

}
