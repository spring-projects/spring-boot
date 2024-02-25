/*
 * Copyright 2012-2023 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ReproducibleResourceTransformer;

/**
 * Extension for the <a href="https://maven.apache.org/plugins/maven-shade-plugin/">Maven
 * shade plugin</a> to allow properties files (e.g. {@literal META-INF/spring.factories})
 * to be merged without losing any information.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @since 1.0.0
 */
public class PropertiesMergingResourceTransformer implements ReproducibleResourceTransformer {

	// Set this in pom configuration with <resource>...</resource>
	private String resource;

	private final Properties data = new Properties();

	private long time;

	/**
	 * Return the data the properties being merged.
	 * @return the data
	 */
	public Properties getData() {
		return this.data;
	}

	/**
	 * Determines if the given resource can be transformed by this resource transformer.
	 * @param resource the resource to be checked
	 * @return true if the resource can be transformed, false otherwise
	 */
	@Override
	public boolean canTransformResource(String resource) {
		return this.resource != null && this.resource.equalsIgnoreCase(resource);
	}

	/**
	 * Processes a resource with the given resource name, input stream, and list of
	 * relocators.
	 * @param resource the name of the resource being processed
	 * @param inputStream the input stream of the resource
	 * @param relocators the list of relocators to apply to the resource
	 * @throws IOException if an I/O error occurs during the resource processing
	 * @deprecated This method has been deprecated since version 2.4.0 and will not be
	 * removed in future versions. Please use the overloaded method
	 * {@link #processResource(String, InputStream, List, int)} instead.
	 */
	@Override
	@Deprecated(since = "2.4.0", forRemoval = false)
	public void processResource(String resource, InputStream inputStream, List<Relocator> relocators)
			throws IOException {
		processResource(resource, inputStream, relocators, 0);
	}

	/**
	 * Processes a resource by loading its properties from an input stream and applying
	 * relocator transformations.
	 * @param resource the name of the resource being processed
	 * @param inputStream the input stream containing the properties of the resource
	 * @param relocators the list of relocator transformations to be applied
	 * @param time the time associated with the resource
	 * @throws IOException if an I/O error occurs while loading the properties
	 */
	@Override
	public void processResource(String resource, InputStream inputStream, List<Relocator> relocators, long time)
			throws IOException {
		Properties properties = new Properties();
		properties.load(inputStream);
		properties.forEach((name, value) -> process((String) name, (String) value));
		if (time > this.time) {
			this.time = time;
		}
	}

	/**
	 * Processes the given name and value and updates the property in the data object. If
	 * the property already exists, the value is appended to the existing value with a
	 * comma separator. If the property does not exist, the value is set as the new value
	 * for the property.
	 * @param name the name of the property to be processed
	 * @param value the value to be processed and updated in the property
	 */
	private void process(String name, String value) {
		String existing = this.data.getProperty(name);
		this.data.setProperty(name, (existing != null) ? existing + "," + value : value);
	}

	/**
	 * Returns a boolean value indicating whether the resource has been transformed.
	 * @return {@code true} if the resource has been transformed, {@code false} otherwise.
	 */
	@Override
	public boolean hasTransformedResource() {
		return !this.data.isEmpty();
	}

	/**
	 * Modifies the output stream by merging the properties data into a Jar file.
	 * @param os the JarOutputStream to modify
	 * @throws IOException if an I/O error occurs while modifying the output stream
	 */
	@Override
	public void modifyOutputStream(JarOutputStream os) throws IOException {
		JarEntry jarEntry = new JarEntry(this.resource);
		jarEntry.setTime(this.time);
		os.putNextEntry(jarEntry);
		this.data.store(os, "Merged by PropertiesMergingResourceTransformer");
		os.flush();
		this.data.clear();
	}

	/**
	 * Returns the resource.
	 * @return the resource
	 */
	public String getResource() {
		return this.resource;
	}

	/**
	 * Sets the resource for the PropertiesMergingResourceTransformer.
	 * @param resource the resource to be set
	 */
	public void setResource(String resource) {
		this.resource = resource;
	}

}
