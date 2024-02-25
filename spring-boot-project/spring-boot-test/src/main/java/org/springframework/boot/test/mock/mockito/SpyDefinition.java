/*
 * Copyright 2012-2023 the original author or authors.
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

import java.lang.reflect.Proxy;

import org.mockito.AdditionalAnswers;
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

import static org.mockito.Mockito.mock;

/**
 * A complete definition that can be used to create a Mockito spy.
 *
 * @author Phillip Webb
 */
class SpyDefinition extends Definition {

	private static final int MULTIPLIER = 31;

	private final ResolvableType typeToSpy;

	/**
     * Creates a new SpyDefinition with the specified name, type to spy, reset behavior, proxy target awareness, and qualifier.
     * 
     * @param name the name of the spy definition
     * @param typeToSpy the type to spy on
     * @param reset the reset behavior for the spy
     * @param proxyTargetAware whether the spy should be aware of the proxy target
     * @param qualifier the qualifier for the spy
     * @throws IllegalArgumentException if the typeToSpy is null
     */
    SpyDefinition(String name, ResolvableType typeToSpy, MockReset reset, boolean proxyTargetAware,
			QualifierDefinition qualifier) {
		super(name, reset, proxyTargetAware, qualifier);
		Assert.notNull(typeToSpy, "TypeToSpy must not be null");
		this.typeToSpy = typeToSpy;

	}

	/**
     * Returns the ResolvableType object representing the type to spy on.
     * 
     * @return the ResolvableType object representing the type to spy on
     */
    ResolvableType getTypeToSpy() {
		return this.typeToSpy;
	}

	/**
     * Compares this SpyDefinition object to the specified object for equality.
     * Returns true if the specified object is also a SpyDefinition object and
     * has the same values for all fields as this object.
     * 
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
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

	/**
     * Returns a hash code value for the object. This method overrides the default implementation of the {@code hashCode()} method
     * inherited from the {@code Object} class.
     * 
     * The hash code is calculated by multiplying the hash code of the superclass by a constant multiplier and adding the hash code
     * of the {@code typeToSpy} field, using the {@code ObjectUtils.nullSafeHashCode()} method to handle null values.
     * 
     * @return the hash code value for this object
     */
    @Override
	public int hashCode() {
		int result = super.hashCode();
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.typeToSpy);
		return result;
	}

	/**
     * Returns a string representation of the SpyDefinition object.
     * 
     * @return a string representation of the SpyDefinition object
     */
    @Override
	public String toString() {
		return new ToStringCreator(this).append("name", getName())
			.append("typeToSpy", this.typeToSpy)
			.append("reset", getReset())
			.toString();
	}

	/**
     * Creates a spy object for the given instance.
     * 
     * @param instance the object to create a spy for
     * @return a spy object for the given instance
     * @throws IllegalArgumentException if the instance is null
     */
    <T> T createSpy(Object instance) {
		return createSpy(getName(), instance);
	}

	/**
     * Creates a spy object for the given instance.
     * 
     * @param name the name of the spy object
     * @param instance the instance to be spied on
     * @return the spy object
     * @throws IllegalArgumentException if the instance is null or not an instance of the specified type
     */
    @SuppressWarnings("unchecked")
	<T> T createSpy(String name, Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		Assert.isInstanceOf(this.typeToSpy.resolve(), instance);
		if (Mockito.mockingDetails(instance).isSpy()) {
			return (T) instance;
		}
		MockSettings settings = MockReset.withSettings(getReset());
		if (StringUtils.hasLength(name)) {
			settings.name(name);
		}
		if (isProxyTargetAware()) {
			settings.verificationStartedListeners(new SpringAopBypassingVerificationStartedListener());
		}
		Class<?> toSpy;
		if (Proxy.isProxyClass(instance.getClass())) {
			settings.defaultAnswer(AdditionalAnswers.delegatesTo(instance));
			toSpy = this.typeToSpy.toClass();
		}
		else {
			settings.defaultAnswer(Mockito.CALLS_REAL_METHODS);
			settings.spiedInstance(instance);
			toSpy = instance.getClass();
		}
		return (T) mock(toSpy, settings);
	}

	/**
	 * A {@link VerificationStartedListener} that bypasses any proxy created by Spring AOP
	 * when the verification of a spy starts.
	 */
	private static final class SpringAopBypassingVerificationStartedListener implements VerificationStartedListener {

		/**
         * This method is called when the verification process is started.
         * It sets the mock object to the ultimate target object using AopTestUtils.
         *
         * @param event the VerificationStartedEvent object containing the details of the event
         */
        @Override
		public void onVerificationStarted(VerificationStartedEvent event) {
			event.setMock(AopTestUtils.getUltimateTargetObject(event.getMock()));
		}

	}

}
