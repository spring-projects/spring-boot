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

package org.springframework.boot.test.mock.mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.mockito.Answers;
import org.mockito.MockSettings;
import org.mockito.Mockito;

import org.springframework.core.ResolvableType;
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

	private final ResolvableType typeToMock;

	private final Set<Class<?>> extraInterfaces;

	private final Answers answer;

	private final boolean serializable;

	MockDefinition(String name, ResolvableType typeToMock, Class<?>[] extraInterfaces,
			Answers answer, boolean serializable, MockReset reset,
			QualifierDefinition qualifier) {
		super(name, reset, false, qualifier);
		Assert.notNull(typeToMock, "TypeToMock must not be null");
		this.typeToMock = typeToMock;
		this.extraInterfaces = asClassSet(extraInterfaces);
		this.answer = (answer != null ? answer : Answers.RETURNS_DEFAULTS);
		this.serializable = serializable;
	}

	private Set<Class<?>> asClassSet(Class<?>[] classes) {
		Set<Class<?>> classSet = new LinkedHashSet<>();
		if (classes != null) {
			classSet.addAll(Arrays.asList(classes));
		}
		return Collections.unmodifiableSet(classSet);
	}

	/**
	 * Return the type that should be mocked.
	 * @return the type to mock; never {@code null}
	 */
	public ResolvableType getTypeToMock() {
		return this.typeToMock;
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
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.typeToMock);
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.extraInterfaces);
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.answer);
		result = MULTIPLIER * result + Boolean.hashCode(this.serializable);
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
		result = result && ObjectUtils.nullSafeEquals(this.typeToMock, other.typeToMock);
		result = result && ObjectUtils.nullSafeEquals(this.extraInterfaces,
				other.extraInterfaces);
		result = result && ObjectUtils.nullSafeEquals(this.answer, other.answer);
		result = result && this.serializable == other.serializable;
		return result;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("name", getName())
				.append("typeToMock", this.typeToMock)
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
		settings.defaultAnswer(MockitoApi.get().getAnswer(this.answer));
		if (this.serializable) {
			settings.serializable();
		}
		return (T) Mockito.mock(this.typeToMock.resolve(), settings);
	}

}
