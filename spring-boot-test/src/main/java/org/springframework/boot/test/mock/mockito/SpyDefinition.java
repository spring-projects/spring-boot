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

package org.springframework.boot.test.mock.mockito;

import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;

import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A complete definition that can be used to create a Mockito spy.
 *
 * @author Phillip Webb
 */
class SpyDefinition extends Definition {

	private MockUtil mockUtil = new MockUtil();

	private static final int MULTIPLIER = 31;

	private final Class<?> classToSpy;

	SpyDefinition(String name, Class<?> classToSpy, MockReset reset,
			boolean proxyTargetAware) {
		super(name, reset, proxyTargetAware);
		Assert.notNull(classToSpy, "ClassToSpy must not be null");
		this.classToSpy = classToSpy;

	}

	public Class<?> getClassToSpy() {
		return this.classToSpy;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.classToSpy);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		SpyDefinition other = (SpyDefinition) obj;
		boolean result = super.equals(obj);
		result &= ObjectUtils.nullSafeEquals(this.classToSpy, other.classToSpy);
		return result;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("name", getName())
				.append("classToSpy", this.classToSpy).append("reset", getReset())
				.toString();
	}

	public <T> T createSpy(Object instance) {
		return createSpy(getName(), instance);
	}

	@SuppressWarnings("unchecked")
	public <T> T createSpy(String name, Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		Assert.isInstanceOf(this.classToSpy, instance);
		if (this.mockUtil.isSpy(instance)) {
			return (T) instance;
		}
		MockSettings settings = MockReset.withSettings(getReset());
		if (StringUtils.hasLength(name)) {
			settings.name(name);
		}
		settings.spiedInstance(instance);
		settings.defaultAnswer(Mockito.CALLS_REAL_METHODS);
		return (T) Mockito.mock(instance.getClass(), settings);
	}

}
