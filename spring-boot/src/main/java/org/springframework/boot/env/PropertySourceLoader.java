/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.env;

import java.io.IOException;

import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * Strategy interface located via {@link SpringFactoriesLoader} and used to load a
 * {@link PropertySource}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
public interface PropertySourceLoader {

	/**
	 * Returns the file extensions that the loader supports (excluding the '.').
	 * @return the file extensions
	 */
	String[] getFileExtensions();

	/**
	 * Load the resource into a property source.
	 * @param name the name of the property source
	 * @param resource the resource to load
	 * @param profile the name of the profile to load or {@code null}. The profile can be
	 * used to load multi-document files (such as YAML). Simple property formats should
	 * {@code null} when asked to load a profile.
	 * @return a property source or {@code null}
	 * @throws IOException
	 */
	PropertySource<?> load(String name, Resource resource, String profile)
			throws IOException;

}
