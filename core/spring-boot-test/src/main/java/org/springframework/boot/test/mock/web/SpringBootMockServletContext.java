/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.mock.web;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;

import org.jspecify.annotations.Nullable;

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

	private @Nullable File emptyRootDirectory;

	public SpringBootMockServletContext(String resourceBasePath) {
		this(resourceBasePath, new FileSystemResourceLoader());
	}

	public SpringBootMockServletContext(String resourceBasePath, ResourceLoader resourceLoader) {
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
	public @Nullable URL getResource(String path) throws MalformedURLException {
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
