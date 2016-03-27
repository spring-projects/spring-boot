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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.mockito.Answers;
import org.mockito.MockSettings;
import org.mockito.Mockito;

import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A complete definition that can be used to create a Mockito mock.
 *
 * @author Phillip Webb
 */
class MockDefinition {

	private static final int MULTIPLIER = 31;

	private final String name;

	private final Class<?> classToMock;

	private final Set<Class<?>> extraInterfaces;

	private final Answers answer;

	private final boolean serializable;

	private final MockReset reset;

	MockDefinition(Class<?> classToMock) {
		this(null, classToMock, null, null, false, null);
	}

	MockDefinition(String name, Class<?> classToMock, Class<?>[] extraInterfaces,
			Answers answer, boolean serializable, MockReset reset) {
		Assert.notNull(classToMock, "ClassToMock must not be null");
		this.name = name;
		this.classToMock = classToMock;
		this.extraInterfaces = asClassSet(extraInterfaces);
		this.answer = (answer != null ? answer : Answers.RETURNS_DEFAULTS);
		this.serializable = serializable;
		this.reset = (reset != null ? reset : MockReset.AFTER);
	}

	private Set<Class<?>> asClassSet(Class<?>[] classes) {
		Set<Class<?>> classSet = new LinkedHashSet<Class<?>>();
		if (classes != null) {
			classSet.addAll(Arrays.asList(classes));
		}
		return Collections.unmodifiableSet(classSet);
	}

	/**
	 * Return the name for bean.
	 * @return the name or {@code null}
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the classes that should be mocked.
	 * @return the class to mock; never {@code null}
	 */
	public Class<?> getClassToMock() {
		return this.classToMock;
	}

	/**
	 * Return the extra interfaces.
	 * @return the extra interfaces or an empty array
	 */
	public Set<Class<?>> getExtraInterfaces() {
		return this.extraInterfaces;
	}

	/**
	 * Return the answers mode.
	 * @return the answer the answers mode; never {@code null}
	 */
	public Answers getAnswer() {
		return this.answer;
	}

	/**
	 * Return if the mock is serializable.
	 * @return if the mock is serializable
	 */
	public boolean isSerializable() {
		return this.serializable;
	}

	/**
	 * Return the mock reset mode.
	 * @return the reset mode
	 */
	public MockReset getReset() {
		return this.reset;
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.name);
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.classToMock);
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.extraInterfaces);
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.answer);
		result = MULTIPLIER * result + (this.serializable ? 1231 : 1237);
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.reset);
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
		MockDefinition other = (MockDefinition) obj;
		boolean result = true;
		result &= ObjectUtils.nullSafeEquals(this.name, other.name);
		result &= ObjectUtils.nullSafeEquals(this.classToMock, other.classToMock);
		result &= ObjectUtils.nullSafeEquals(this.extraInterfaces, other.extraInterfaces);
		result &= ObjectUtils.nullSafeEquals(this.answer, other.answer);
		result &= this.serializable == other.serializable;
		result &= ObjectUtils.nullSafeEquals(this.reset, other.reset);
		return result;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("name", this.name)
				.append("classToMock", this.classToMock)
				.append("extraInterfaces", this.extraInterfaces)
				.append("answer", this.answer).append("serializable", this.serializable)
				.append("reset", this.reset).toString();
	}

	public <T> T createMock() {
		return createMock(this.name);
	}

	@SuppressWarnings("unchecked")
	public <T> T createMock(String name) {
		MockSettings settings = MockReset.withSettings(this.reset);
		if (StringUtils.hasLength(name)) {
			settings.name(name);
		}
		if (!this.extraInterfaces.isEmpty()) {
			settings.extraInterfaces(this.extraInterfaces.toArray(new Class<?>[] {}));
		}
		settings.defaultAnswer(this.answer.get());
		if (this.serializable) {
			settings.serializable();
		}
		return (T) Mockito.mock(this.classToMock, settings);
	}

}
