/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.env;

import java.io.IOException;
import java.util.List;

import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * Strategy interface located via {@link SpringFactoriesLoader} and used to load a
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
