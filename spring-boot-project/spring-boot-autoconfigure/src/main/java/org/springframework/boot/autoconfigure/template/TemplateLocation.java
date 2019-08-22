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

package org.springframework.boot.autoconfigure.template;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;

/**
 * Contains a location that templates can be loaded from.
 *
 * @author Phillip Webb
 * @since 1.2.1
 */
public class TemplateLocation {

	private final String path;

	public TemplateLocation(String path) {
		Assert.notNull(path, "Path must not be null");
		this.path = path;
	}

	/**
	 * Determine if this template location exists using the specified
	 * {@link ResourcePatternResolver}.
	 * @param resolver the resolver used to test if the location exists
	 * @return {@code true} if the location exists.
	 */
	public boolean exists(ResourcePatternResolver resolver) {
		Assert.notNull(resolver, "Resolver must not be null");
		if (resolver.getResource(this.path).exists()) {
			return true;
		}
		try {
			return anyExists(resolver);
		}
		catch (IOException ex) {
			return false;
		}
	}

	private boolean anyExists(ResourcePatternResolver resolver) throws IOException {
		String searchPath = this.path;
		if (searchPath.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX)) {
			searchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
					+ searchPath.substring(ResourceLoader.CLASSPATH_URL_PREFIX.length());
		}
		if (searchPath.startsWith(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX)) {
			Resource[] resources = resolver.getResources(searchPath);
			for (Resource resource : resources) {
				if (resource.exists()) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return this.path;
	}

}
