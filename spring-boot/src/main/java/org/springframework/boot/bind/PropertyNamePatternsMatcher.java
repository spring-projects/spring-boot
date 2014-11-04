/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.bind;

/**
 * Strategy interface used to check if a property name matches specific criteria.
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
interface PropertyNamePatternsMatcher {

	PropertyNamePatternsMatcher NONE = new PropertyNamePatternsMatcher() {

		@Override
		public boolean matches(String propertyName) {
			return false;
		}

	};

	/**
	 * Return {@code true} of the property name matches.
	 * @param propertyName the property name
	 * @return {@code true} if the property name matches
	 */
	boolean matches(String propertyName);

}
