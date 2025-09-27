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

package org.springframework.boot.test.json;

import org.assertj.core.api.AbstractMapAssert;
import org.assertj.core.api.AbstractObjectArrayAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assert;
import org.assertj.core.api.InstanceOfAssertFactories;

/**
 * AssertJ {@link Assert} for {@link ObjectContent}.
 *
 * @param <A> the actual type
 * @author Phillip Webb
 * @author Stefano Cordio
 * @since 1.4.0
 */
public class ObjectContentAssert<A> extends AbstractObjectAssert<ObjectContentAssert<A>, A> {

	protected ObjectContentAssert(A actual) {
		super(actual, ObjectContentAssert.class);
	}

	/**
	 * Verifies that the actual value is an array, and returns an array assertion, to
	 * allow chaining of array-specific assertions from this call.
	 * @return an array assertion object
	 */
	public AbstractObjectArrayAssert<?, Object> asArray() {
		return asInstanceOf(InstanceOfAssertFactories.ARRAY);
	}

	/**
	 * Verifies that the actual value is a map, and returns a map assertion, to allow
	 * chaining of map-specific assertions from this call.
	 * @return a map assertion object
	 */
	public AbstractMapAssert<?, ?, Object, Object> asMap() {
		return asInstanceOf(InstanceOfAssertFactories.MAP);
	}

}
