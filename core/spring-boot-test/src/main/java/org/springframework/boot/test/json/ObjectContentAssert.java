/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.json;

import org.assertj.core.api.AbstractMapAssert;
import org.assertj.core.api.AbstractObjectArrayAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assert;
import org.assertj.core.api.InstanceOfAssertFactories;

import org.springframework.lang.CheckReturnValue;

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
	@CheckReturnValue
	public AbstractObjectArrayAssert<?, Object> asArray() {
		return asInstanceOf(InstanceOfAssertFactories.ARRAY);
	}

	/**
	 * Verifies that the actual value is a map, and returns a map assertion, to allow
	 * chaining of map-specific assertions from this call.
	 * @return a map assertion object
	 */
	@CheckReturnValue
	public AbstractMapAssert<?, ?, Object, Object> asMap() {
		return asInstanceOf(InstanceOfAssertFactories.MAP);
	}

}
