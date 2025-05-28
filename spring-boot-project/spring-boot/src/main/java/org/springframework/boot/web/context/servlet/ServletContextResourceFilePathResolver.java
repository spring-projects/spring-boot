/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.web.context.servlet;

import org.springframework.boot.io.ApplicationResourceLoader;
import org.springframework.boot.io.ApplicationResourceLoader.FilePathResolver;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.support.ServletContextResource;

/**
 * {@link FilePathResolver} for {@link ServletContextResource}.
 *
 * @author Phillip Webb
 */
class ServletContextResourceFilePathResolver implements ApplicationResourceLoader.FilePathResolver {

	private static final String RESOURCE_CLASS_NAME = "org.springframework.web.context.support.ServletContextResource";

	private final Class<?> resourceClass;

	ServletContextResourceFilePathResolver() {
		ClassLoader classLoader = getClass().getClassLoader();
		this.resourceClass = ClassUtils.isPresent(RESOURCE_CLASS_NAME, classLoader)
				? ClassUtils.resolveClassName(RESOURCE_CLASS_NAME, classLoader) : null;
	}

	@Override
	public String resolveFilePath(String location, Resource resource) {
		return (this.resourceClass != null && this.resourceClass.isInstance(resource)) ? location : null;
	}

}
