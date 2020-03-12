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

package org.springframework.boot.loader.tools.layer.library;

import org.springframework.boot.loader.tools.Library;

/**
 * A filter that can tell if a {@link Library} has been included or excluded.
 *
 * @author Madhura Bhave
 * @since 2.3.0
 */
public interface LibraryFilter {

	/**
	 * Return true if the {@link Library} is included by the filter.
	 * @param library the library
	 * @return true if the library is included
	 */
	boolean isLibraryIncluded(Library library);

	/**
	 * Return true if the {@link Library} is excluded by the filter.
	 * @param library the library
	 * @return true if the library is excluded
	 */
	boolean isLibraryExcluded(Library library);

}
