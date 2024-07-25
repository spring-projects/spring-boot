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

package org.springframework.boot.build.architecture.objects.noRequireNonNull;

import org.springframework.util.Assert;

/**
 * This class is used to test the use of `Assert.notNull` for null-checking. It is
 * intended to demonstrate an acceptable approach according to the architecture check
 * rules.
 *
 * @author Ivan Malutin
 */
public class AssertNotNullUsage {

	/**
	 * Validates that the provided object is not null using `Assert.notNull`. This method
	 * is compliant with the architecture check rules.
	 */
	public void ensureObjectNotNull() {
		Assert.notNull(new Object(), "The object must not be null");
	}

}