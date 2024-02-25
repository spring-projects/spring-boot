/*
 * Copyright 2012-2022 the original author or authors.
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

	/**
     * Constructs a new JavaCompilerPluginConfiguration object with the specified MavenProject.
     * 
     * @param project the MavenProject object to be associated with this JavaCompilerPluginConfiguration
     */
    JavaCompilerPluginConfiguration(MavenProject project) {
		this.project = project;
	}

	/**
     * Returns the major version of the source code used in the JavaCompilerPluginConfiguration.
     * 
     * @return The major version of the source code.
     */
    String getSourceMajorVersion() {
		String version = getConfigurationValue("source");

		if (version == null) {
			version = getPropertyValue("maven.compiler.source");
		}

		return majorVersionFor(version);
	}

	/**
     * Returns the target major version of the Java compiler.
     * 
     * This method retrieves the target version from the configuration value "target". If the configuration value is null,
     * it retrieves the target version from the property value "maven.compiler.target". The major version is then extracted
     * from the retrieved version string using the majorVersionFor() method.
     * 
     * @return the target major version of the Java compiler
     */
    String getTargetMajorVersion() {
		String version = getConfigurationValue("target");

		if (version == null) {
			version = getPropertyValue("maven.compiler.target");
		}

		return majorVersionFor(version);
	}

	/**
     * Returns the release version of the Java compiler.
     * 
     * This method first checks the "release" configuration value. If it is null,
     * then it checks the "maven.compiler.release" property. The major version of
     * the release version is returned.
     * 
     * @return the major version of the release version
     */
    String getReleaseVersion() {
		String version = getConfigurationValue("release");

		if (version == null) {
			version = getPropertyValue("maven.compiler.release");
		}

		return majorVersionFor(version);
	}

	/**
     * Retrieves the value of a configuration property.
     * 
     * @param propertyName the name of the property to retrieve
     * @return the value of the specified property, or null if the property is not found
     */
    private String getConfigurationValue(String propertyName) {
		Plugin plugin = this.project.getPlugin("org.apache.maven.plugins:maven-compiler-plugin");
		if (plugin != null) {
			Object pluginConfiguration = plugin.getConfiguration();
			if (pluginConfiguration instanceof Xpp3Dom dom) {
				return getNodeValue(dom, propertyName);
			}
		}
		return null;
	}

	/**
     * Retrieves the value of a specified property.
     * 
     * @param propertyName the name of the property to retrieve
     * @return the value of the property as a string, or null if the property does not exist
     */
    private String getPropertyValue(String propertyName) {
		if (this.project.getProperties().containsKey(propertyName)) {
			return this.project.getProperties().get(propertyName).toString();
		}
		return null;
	}

	/**
     * Retrieves the value of a specific node in the given Xpp3Dom object.
     * 
     * @param dom The Xpp3Dom object to search for the node.
     * @param childNames The names of the child nodes to traverse in order to reach the desired node.
     * @return The value of the desired node, or null if the node is not found.
     */
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

	/**
     * Returns the major version for the given version.
     * 
     * @param version the version to get the major version for
     * @return the major version of the given version, or the same version if it does not start with "1."
     */
    private String majorVersionFor(String version) {
		if (version != null && version.startsWith("1.")) {
			return version.substring("1.".length());
		}
		return version;
	}

}
