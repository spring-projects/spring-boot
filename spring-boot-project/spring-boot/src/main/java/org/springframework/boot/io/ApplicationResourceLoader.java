/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.io;

import org.springframework.core.io.ContextResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * A {@link DefaultResourceLoader} with any {@link ProtocolResolver ProtocolResolvers}
 * registered in a {@code spring.factories} file applied to it. Plain paths without a
 * qualifier will resolve to file system resources. This is different from
 * {@code DefaultResourceLoader}, which resolves unqualified paths to classpath resources.
 *
 * @author Scott Frederick
 * @since 3.3.0
 */
public class ApplicationResourceLoader extends DefaultResourceLoader {

	/**
	 * Create a new {@code ApplicationResourceLoader}.
	 */
	public ApplicationResourceLoader() {
		this(null);
	}

	/**
	 * Create a new {@code ApplicationResourceLoader}.
	 * @param classLoader the {@link ClassLoader} to load class path resources with, or
	 * {@code null} for using the thread context class loader at the time of actual
	 * resource access
	 */
	public ApplicationResourceLoader(ClassLoader classLoader) {
		super(classLoader);
		SpringFactoriesLoader loader = SpringFactoriesLoader.forDefaultResourceLocation(classLoader);
		getProtocolResolvers().addAll(loader.load(ProtocolResolver.class));
	}

	@Override
	protected Resource getResourceByPath(String path) {
		return new FileSystemContextResource(path);
	}

	private static class FileSystemContextResource extends FileSystemResource implements ContextResource {

		FileSystemContextResource(String path) {
			super(path);
		}

		@Override
		public String getPathWithinContext() {
			return getPath();
		}

	}

}
