/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.env;

import java.io.IOException;
import java.util.List;

import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * Strategy interface located through {@link SpringFactoriesLoader} and used to load a
 * {@link PropertySource}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.0.0
 */
public interface PropertySourceLoader {

	/**
	 * Returns the file extensions that the loader supports (excluding the '.').
	 * @return the file extensions
	 */
	String[] getFileExtensions();

	/**
	 * Load the resource into one or more property sources. Implementations may either
	 * return a list containing a single source, or in the case of a multi-document format
	 * such as yaml a source for each document in the resource.
	 * @param name the root name of the property source. If multiple documents are loaded
	 * an additional suffix should be added to the name for each source loaded.
	 * @param resource the resource to load
	 * @return a list property sources
	 * @throws IOException if the source cannot be loaded
	 */
	List<PropertySource<?>> load(String name, Resource resource) throws IOException;

}
