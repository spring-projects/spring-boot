/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.devtools.restart.server;

import java.net.URL;

/**
 * Filter URLs based on a source directory name. Used to match URLs from the running
 * classpath against source directory on a remote system.
 *
 * @author Phillip Webb
 * @since 2.3.0
 * @see DefaultSourceDirectoryUrlFilter
 */
@FunctionalInterface
public interface SourceDirectoryUrlFilter {

	/**
	 * Determine if the specified URL matches a source directory.
	 * @param sourceDirectory the source directory
	 * @param url the URL to check
	 * @return {@code true} if the URL matches
	 */
	boolean isMatch(String sourceDirectory, URL url);

}
