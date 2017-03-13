/*
 * Copyright 2012-2017 the original author or authors.
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

import org.springframework.core.env.PropertySource;

/**
 * An additional interface that may be implemented by a {@link PropertySource} that can
 * return origin information. For example a property source that's backed by a file may
 * return origin information for line and column numbers.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public interface OriginCapablePropertySource {

	/**
	 * Return the origin of the given property name or {@code null} if the origin cannot
	 * be determined.
	 * @param name the property name
	 * @return the origin of the property or {@code null}
	 */
	PropertyOrigin getPropertyOrigin(String name);

}
