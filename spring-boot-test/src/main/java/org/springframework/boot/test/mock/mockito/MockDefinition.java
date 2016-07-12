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
import org.mockito.stubbing.Answer;

import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A complete definition that can be used to create a Mockito mock.
 *
 * @author Phillip Webb
 */
class MockDefinition extends Definition {

	private static final int MULTIPLIER = 31;

	private final Class<?> classToMock;

	private final Set<Class<?>> extraInterfaces;

	private final Answers answer;

	private final boolean serializable;

	MockDefinition(Class<?> classToMock) {
		this(null, classToMock, null, null, false, null, true);
	}

	MockDefinition(String name, Class<?> classToMock, Class<?>[] extraInterfaces,
			Answers answer, boolean serializable, MockReset reset,
			boolean proxyTargetAware) {
		super(name, reset, proxyTargetAware);
		Assert.notNull(classToMock, "ClassToMock must not be null");
		this.classToMock = classToMock;
		this.extraInterfaces = asClassSet(extraInterfaces);
		this.answer = (answer != null ? answer : Answers.RETURNS_DEFAULTS);
		this.serializable = serializable;
	}

	private Set<Class<?>> asClassSet(Class<?>[] classes) {
		Set<Class<?>> classSet = new LinkedHashSet<Class<?>>();
		if (classes != null) {
			classSet.addAll(Arrays.asList(classes));
		}
		return Collections.unmodifiableSet(classSet);
	}

	/**
	 * Return the class that should be mocked.
	 * @return the class to mock; never {@code null}
	 */
	public Class<?> getClassToMock() {
		return this.classToMock;
	}

	/**
	 * Return the extra interfaces.
	 * @return the extra interfaces or an empty set
	 */
	public Set<Class<?>> getExtraInterfaces() {
		return this.extraInterfaces;
	}

	/**
	 * Return the answers mode.
	 * @return the answers mode; never {@code null}
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

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.classToMock);
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.extraInterfaces);
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.answer);
		result = MULTIPLIER * result + (this.serializable ? 1231 : 1237);
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
		boolean result = super.equals(obj);
		result &= ObjectUtils.nullSafeEquals(this.classToMock, other.classToMock);
		result &= ObjectUtils.nullSafeEquals(this.extraInterfaces, other.extraInterfaces);
		result &= ObjectUtils.nullSafeEquals(this.answer, other.answer);
		result &= this.serializable == other.serializable;
		return result;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("name", getName())
				.append("classToMock", this.classToMock)
				.append("extraInterfaces", this.extraInterfaces)
				.append("answer", this.answer).append("serializable", this.serializable)
				.append("reset", getReset()).toString();
	}

	public <T> T createMock() {
		return createMock(getName());
	}

	@SuppressWarnings("unchecked")
	public <T> T createMock(String name) {
		MockSettings settings = MockReset.withSettings(getReset());
		if (StringUtils.hasLength(name)) {
			settings.name(name);
		}
		if (!this.extraInterfaces.isEmpty()) {
			settings.extraInterfaces(this.extraInterfaces.toArray(new Class<?>[] {}));
		}
		settings.defaultAnswer(getAnswer(this.answer));
		if (this.serializable) {
			settings.serializable();
		}
		return (T) Mockito.mock(this.classToMock, settings);
	}

	private Answer<?> getAnswer(Answers answer) {
		if (Answer.class.isInstance(answer)) {
			// With Mockito 2.0 we can directly cast the answer
			return (Answer<?>) ((Object) answer);
		}
		return answer.get();
	}

}
