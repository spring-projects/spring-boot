/*
 * Copyright 2012-2025 the original author or authors.
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

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Strategy interface registered in {@code spring.factories} and used by
 * {@link ApplicationResourceLoader} to determine the file path of loaded resource when it
 * can also be represented as a {@link FileSystemResource}.
 *
 * @author Phillip Webb
 * @since 3.4.5
 */
public interface ResourceFilePathResolver {

	/**
	 * Return the {@code path} of the given resource if it can also be represented as a
	 * {@link FileSystemResource}.
	 * @param location the location used to create the resource
	 * @param resource the resource to check
	 * @return the file path of the resource or {@code null} if the it is not possible to
	 * represent the resource as a {@link FileSystemResource}.
	 */
	String resolveFilePath(String location, Resource resource);

}
