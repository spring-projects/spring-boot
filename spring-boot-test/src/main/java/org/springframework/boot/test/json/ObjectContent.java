/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test.json;

import org.assertj.core.api.AssertProvider;

import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * Object content usually created from {@link AbstractJsonMarshalTester}. Generally used
 * only to {@link AssertProvider provide} {@link ObjectContentAssert} to AssertJ
 * {@code assertThat} calls.
 *
 * @param <T> the content type
 * @author Phillip Webb
 * @since 1.4.0
 */
public final class ObjectContent<T> implements AssertProvider<ObjectContentAssert<T>> {

	private final ResolvableType type;

	private final T object;

	/**
	 * Create a new {@link ObjectContent} instance.
	 * @param type the type under test (or {@code null} if not known)
	 * @param object the actual object content
	 */
	public ObjectContent(ResolvableType type, T object) {
		Assert.notNull(object, "Object must not be null");
		this.type = type;
		this.object = object;
	}

	@Override
	public ObjectContentAssert<T> assertThat() {
		return new ObjectContentAssert<T>(this.object);
	}

	/**
	 * Return the actual object content.
	 * @return the object content
	 */
	public T getObject() {
		return this.object;
	}

	@Override
	public String toString() {
		return "ObjectContent " + this.object
				+ (this.type == null ? "" : " created from " + this.type);
	}

}
