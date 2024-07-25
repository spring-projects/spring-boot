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

package org.springframework.boot.build.architecture.objects.noRequireNonNullWithSupplier;

import org.springframework.util.Assert;

/**
 * This class uses `Assert.notNull(Object, Supplier)` instead of
 * `Objects.requireNonNull(Object, Supplier)`, and should pass the architecture check.
 *
 * @author Ivan Malutin
 */
public class NoRequireNonNullWithSupplierUsage {

	/**
	 * Example method that uses `Assert.notNull(Object, Supplier)`, which should not be
	 * flagged by the architecture check.
	 */
	public void exampleMethod() {
		Assert.notNull(new Object(), () -> "Object must not be null");
	}

}
