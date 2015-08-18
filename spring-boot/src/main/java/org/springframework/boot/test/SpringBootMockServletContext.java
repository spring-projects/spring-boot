/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockServletContext;

/**
 * {@link MockServletContext} implementation for Spring Boot. Respects well know Spring
 * Boot resource locations and uses an empty folder for "/" if no locations can be found.
 *
 * @author Phillip Webb
 */
public class SpringBootMockServletContext extends MockServletContext {

	private static final String[] SPRING_BOOT_RESOURCE_LOCATIONS = new String[] {
			"classpath:META-INF/resources", "classpath:resources", "classpath:static",
			"classpath:public" };

	private final ResourceLoader resourceLoader;

	private File emptyRootFolder;

	public SpringBootMockServletContext(String resourceBasePath) {
		this(resourceBasePath, new FileSystemResourceLoader());
	}

	public SpringBootMockServletContext(String resourceBasePath,
			ResourceLoader resourceLoader) {
		super(resourceBasePath, resourceLoader);
		this.resourceLoader = resourceLoader;
	}

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

	protected final String getResourceBasePathLocation(String path) {
		return super.getResourceLocation(path);
	}

	private boolean exists(String resourceLocation) {
		try {
			Resource resource = this.resourceLoader.getResource(resourceLocation);
			return resource.exists();
		}
		catch (Exception ex) {
			return false;
		}
	}

	@Override
	public URL getResource(String path) throws MalformedURLException {
		URL resource = super.getResource(path);
		if (resource == null && "/".equals(path)) {
			// Liquibase assumes that "/" always exists, if we don't have a folder
			// use a temporary location.
			try {
				if (this.emptyRootFolder == null) {
					synchronized (this) {
						File tempFolder = File.createTempFile("spr", "servlet");
						tempFolder.delete();
						tempFolder.mkdirs();
						tempFolder.deleteOnExit();
						this.emptyRootFolder = tempFolder;
					}
				}
				return this.emptyRootFolder.toURI().toURL();
			}
			catch (IOException ex) {
			}
		}
		return resource;
	}
}
