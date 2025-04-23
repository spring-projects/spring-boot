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

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * {@link ResourceFilePathResolver} for {@link ClassPathResource}.
 *
 * @author Phillip Webb
 */
class ClassPathResourceFilePathResolver implements ResourceFilePathResolver {

	@Override
	public String resolveFilePath(String location, Resource resource) {
		return (resource instanceof ClassPathResource && !isClassPathUrl(location)) ? location : null;
	}

	private boolean isClassPathUrl(String location) {
		return location.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX);
	}

}
