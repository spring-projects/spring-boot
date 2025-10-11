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

package org.springframework.boot.devtools.restart.classloader;

import org.jspecify.annotations.Nullable;

/**
 * A container for files that may be served from a {@link ClassLoader}. Can be used to
 * represent files that have been added, modified or deleted since the original JAR was
 * created.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see ClassLoaderFile
 */
@FunctionalInterface
public interface ClassLoaderFileRepository {

	/**
	 * Empty {@link ClassLoaderFileRepository} implementation.
	 */
	ClassLoaderFileRepository NONE = (name) -> null;

	/**
	 * Return a {@link ClassLoaderFile} for the given name or {@code null} if no file is
	 * contained in this collection.
	 * @param name the name of the file
	 * @return a {@link ClassLoaderFile} or {@code null}
	 */
	@Nullable ClassLoaderFile getFile(@Nullable String name);

}
