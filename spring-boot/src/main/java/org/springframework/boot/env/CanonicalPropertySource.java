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

import java.util.Set;

/**
 * A source of {@link CanonicalPropertyName}/{@code CanonicalPropertyValue} pairs. A
 * {@link CanonicalPropertySource} provides access to properties that can be accessed in
 * the canonical form.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public interface CanonicalPropertySource {

	/**
	 * Return a single property from the source.
	 * @param name the name of the property (must not be {@code null})
	 * @return the associated value or {@code null}.
	 */
	public CanonicalPropertyValue getProperty(CanonicalPropertyName name);

	/**
	 * Return a set of the canonical property names that are managed by this source.
	 * @return a set of the managed names (never {@code null})
	 */
	public Set<CanonicalPropertyName> getCanonicalPropertyNames();

}
