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

package org.springframework.boot.buildpack.platform.io;

import java.io.IOException;

/**
 * Supplier that can safely throw {@link IOException IO exceptions}.
 *
 * @param <T> the supplied type
 * @author Phillip Webb
 * @since 2.3.0
 */
@FunctionalInterface
public interface IOSupplier<T> {

	/**
	 * Gets the supplied value.
	 * @return the supplied value
	 * @throws IOException on IO error
	 */
	T get() throws IOException;

}
