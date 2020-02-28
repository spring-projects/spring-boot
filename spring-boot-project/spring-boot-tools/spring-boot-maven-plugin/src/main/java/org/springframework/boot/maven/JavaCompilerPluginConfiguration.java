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

package org.springframework.boot.maven;

import java.util.Arrays;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Provides access to the Maven Java Compiler plugin configuration.
 *
 * @author Scott Frederick
 */
class JavaCompilerPluginConfiguration {

	private final MavenProject project;

	JavaCompilerPluginConfiguration(MavenProject project) {
		this.project = project;
	}

	String getSourceMajorVersion() {
		String version = getConfigurationValue("source");

		if (version == null) {
			version = getPropertyValue("maven.compiler.source");
		}

		return majorVersionFor(version);
	}

	String getTargetMajorVersion() {
		String version = getConfigurationValue("target");

		if (version == null) {
			version = getPropertyValue("maven.compiler.target");
		}

		return majorVersionFor(version);
	}

	private String getConfigurationValue(String propertyName) {
		Plugin plugin = this.project.getPlugin("org.apache.maven.plugins:maven-compiler-plugin");
		if (plugin != null) {
			Object pluginConfiguration = plugin.getConfiguration();
			if (pluginConfiguration instanceof Xpp3Dom) {
				Xpp3Dom dom = (Xpp3Dom) pluginConfiguration;
				return getNodeValue(dom, propertyName);
			}
		}
		return null;
	}

	private String getPropertyValue(String propertyName) {
		if (this.project.getProperties().containsKey(propertyName)) {
			return this.project.getProperties().get(propertyName).toString();
		}
		return null;
	}

	private String getNodeValue(Xpp3Dom dom, String... childNames) {
		Xpp3Dom childNode = dom.getChild(childNames[0]);

		if (childNode == null) {
			return null;
		}

		if (childNames.length > 1) {
			return getNodeValue(childNode, Arrays.copyOfRange(childNames, 1, childNames.length));
		}

		return childNode.getValue();
	}

	private String majorVersionFor(String version) {
		if (version != null && version.startsWith("1.")) {
			return version.substring("1.".length());
		}
		return version;
	}

}
