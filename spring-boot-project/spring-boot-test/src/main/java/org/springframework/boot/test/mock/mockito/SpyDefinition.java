/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.mock.mockito;

import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.mockito.listeners.VerificationStartedEvent;
import org.mockito.listeners.VerificationStartedListener;

import org.springframework.core.ResolvableType;
import org.springframework.core.style.ToStringCreator;
import org.springframework.test.util.AopTestUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A complete definition that can be used to create a Mockito spy.
 *
 * @author Phillip Webb
 */
class SpyDefinition extends Definition {

	private static final int MULTIPLIER = 31;

	private final ResolvableType typeToSpy;

	SpyDefinition(String name, ResolvableType typeToSpy, MockReset reset, boolean proxyTargetAware,
			QualifierDefinition qualifier) {
		super(name, reset, proxyTargetAware, qualifier);
		Assert.notNull(typeToSpy, "TypeToSpy must not be null");
		this.typeToSpy = typeToSpy;

	}

	public ResolvableType getTypeToSpy() {
		return this.typeToSpy;
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
		result = result && ObjectUtils.nullSafeEquals(this.typeToSpy, other.typeToSpy);
		return result;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.typeToSpy);
		return result;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("name", getName()).append("typeToSpy", this.typeToSpy)
				.append("reset", getReset()).toString();
	}

	public <T> T createSpy(Object instance) {
		return createSpy(getName(), instance);
	}

	@SuppressWarnings("unchecked")
	public <T> T createSpy(String name, Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		Assert.isInstanceOf(this.typeToSpy.resolve(), instance);
		if (Mockito.mockingDetails(instance).isSpy()) {
			return (T) instance;
		}
		MockSettings settings = MockReset.withSettings(getReset());
		if (StringUtils.hasLength(name)) {
			settings.name(name);
		}
		settings.spiedInstance(instance);
		settings.defaultAnswer(Mockito.CALLS_REAL_METHODS);
		if (this.isProxyTargetAware()) {
			settings.verificationStartedListeners(new SpringAopBypassingVerificationStartedListener());
		}
		return (T) Mockito.mock(instance.getClass(), settings);
	}

	/**
	 * A {@link VerificationStartedListener} that bypasses any proxy created by Spring AOP
	 * when the verification of a spy starts.
	 */
	private static final class SpringAopBypassingVerificationStartedListener implements VerificationStartedListener {

		@Override
		public void onVerificationStarted(VerificationStartedEvent event) {
			event.setMock(AopTestUtils.getUltimateTargetObject(event.getMock()));
		}

	}

}
