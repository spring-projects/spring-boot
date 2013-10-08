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

import groovy.lang.GroovyClassLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;

final class PropertiesArtifactCoordinatesResolver implements ArtifactCoordinatesResolver {

	private final GroovyClassLoader loader;

	private Properties properties = null;

	public PropertiesArtifactCoordinatesResolver(GroovyClassLoader loader) {
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
			loadProperties();
		}
		return this.properties.getProperty(name);
	}

	private void loadProperties() {
		Properties properties = new Properties();
		try {
			for (URL url : Collections.list(this.loader
					.getResources("META-INF/springcli.properties"))) {
				InputStream inputStream = url.openStream();
				try {
					properties.load(inputStream);
				}
				catch (IOException ioe) {
					// Swallow and continue
				}
				finally {
					inputStream.close();
				}
			}
		}
		catch (IOException e) {
			// Swallow and continue
		}
		this.properties = properties;
	}

}
