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

package org.springframework.boot.test.mock.web;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;

import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockServletContext;

/**
 * {@link MockServletContext} implementation for Spring Boot. Respects well-known Spring
 * Boot resource locations and uses an empty directory for "/" if no locations can be
 * found.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
public class SpringBootMockServletContext extends MockServletContext {

	private static final String[] SPRING_BOOT_RESOURCE_LOCATIONS = new String[] { "classpath:META-INF/resources",
			"classpath:resources", "classpath:static", "classpath:public" };

	private final ResourceLoader resourceLoader;

	private File emptyRootDirectory;

	/**
	 * Constructs a new SpringBootMockServletContext with the specified resource base
	 * path.
	 * @param resourceBasePath the base path for the resources in the servlet context
	 */
	public SpringBootMockServletContext(String resourceBasePath) {
		this(resourceBasePath, new FileSystemResourceLoader());
	}

	/**
	 * Constructs a new SpringBootMockServletContext with the specified resource base path
	 * and resource loader.
	 * @param resourceBasePath the base path for the resources in the servlet context
	 * @param resourceLoader the resource loader used to load the resources
	 */
	public SpringBootMockServletContext(String resourceBasePath, ResourceLoader resourceLoader) {
		super(resourceBasePath, resourceLoader);
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Retrieves the resource location for the given path. If the path does not start with
	 * a "/", it is appended to the path. The resource location is determined by checking
	 * if the resource exists in the base path location, and if not, checking in the
	 * Spring Boot resource locations. If the resource is not found in any of the
	 * locations, the super class's getResourceLocation method is called.
	 * @param path the path of the resource
	 * @return the resource location if found, otherwise the result of the super class's
	 * getResourceLocation method
	 */
	@Override
	protected String getResourceLocation(String path) {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		String resourceLocation = getResourceBasePathLocation(path);
		if (exists(resourceLocation)) {
			return resourceLocation;
		}
		for (String prefix : SPRING_BOOT_RESOURCE_LOCATIONS) {
			resourceLocation = prefix + path;
			if (exists(resourceLocation)) {
				return resourceLocation;
			}
		}
		return super.getResourceLocation(path);
	}

	/**
	 * Returns the resource base path location for the specified path.
	 * @param path the path of the resource
	 * @return the resource base path location
	 */
	protected final String getResourceBasePathLocation(String path) {
		return super.getResourceLocation(path);
	}

	/**
	 * Checks if a resource exists at the specified location.
	 * @param resourceLocation the location of the resource to check
	 * @return true if the resource exists, false otherwise
	 */
	private boolean exists(String resourceLocation) {
		try {
			Resource resource = this.resourceLoader.getResource(resourceLocation);
			return resource.exists();
		}
		catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Retrieves the resource URL for the given path.
	 * @param path the path of the resource
	 * @return the URL of the resource, or null if not found
	 * @throws MalformedURLException if the path is not a valid URL
	 */
	@Override
	public URL getResource(String path) throws MalformedURLException {
		URL resource = super.getResource(path);
		if (resource == null && "/".equals(path)) {
			// Liquibase assumes that "/" always exists, if we don't have a directory
			// use a temporary location.
			try {
				if (this.emptyRootDirectory == null) {
					synchronized (this) {
						File tempDirectory = Files.createTempDirectory("spr-servlet").toFile();
						tempDirectory.deleteOnExit();
						this.emptyRootDirectory = tempDirectory;
					}
				}
				return this.emptyRootDirectory.toURI().toURL();
			}
			catch (IOException ex) {
				// Ignore
			}
		}
		return resource;
	}

}
