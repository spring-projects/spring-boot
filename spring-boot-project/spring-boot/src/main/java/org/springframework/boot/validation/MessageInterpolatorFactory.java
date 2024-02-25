/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.validation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.validation.MessageInterpolator;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.MessageSource;
import org.springframework.util.ClassUtils;

/**
 * {@link ObjectFactory} that can be used to create a {@link MessageInterpolator}.
 * Attempts to pick the most appropriate {@link MessageInterpolator} based on the
 * classpath.
 *
 * @author Phillip Webb
 * @since 1.5.0
 */
public class MessageInterpolatorFactory implements ObjectFactory<MessageInterpolator> {

	private static final Set<String> FALLBACKS;

	static {
		Set<String> fallbacks = new LinkedHashSet<>();
		fallbacks.add("org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator");
		FALLBACKS = Collections.unmodifiableSet(fallbacks);
	}

	private final MessageSource messageSource;

	/**
	 * Constructs a new MessageInterpolatorFactory with the specified parent factory.
	 * @param parentFactory the parent factory to be used for message interpolation, or
	 * null if none
	 */
	public MessageInterpolatorFactory() {
		this(null);
	}

	/**
	 * Creates a new {@link MessageInterpolatorFactory} that will produce a
	 * {@link MessageInterpolator} that uses the given {@code messageSource} to resolve
	 * any message parameters before final interpolation.
	 * @param messageSource message source to be used by the interpolator
	 * @since 2.6.0
	 */
	public MessageInterpolatorFactory(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Retrieves the MessageInterpolator object.
	 * @return The MessageInterpolator object.
	 * @throws BeansException If there is an error retrieving the MessageInterpolator
	 * object.
	 */
	@Override
	public MessageInterpolator getObject() throws BeansException {
		MessageInterpolator messageInterpolator = getMessageInterpolator();
		if (this.messageSource != null) {
			return new MessageSourceMessageInterpolator(this.messageSource, messageInterpolator);
		}
		return messageInterpolator;
	}

	/**
	 * Returns the message interpolator for the default provider.
	 * @return the message interpolator
	 * @throws ValidationException if an error occurs while retrieving the message
	 * interpolator
	 */
	private MessageInterpolator getMessageInterpolator() {
		try {
			return Validation.byDefaultProvider().configure().getDefaultMessageInterpolator();
		}
		catch (ValidationException ex) {
			MessageInterpolator fallback = getFallback();
			if (fallback != null) {
				return fallback;
			}
			throw ex;
		}
	}

	/**
	 * Returns the fallback MessageInterpolator.
	 *
	 * This method attempts to retrieve the fallback MessageInterpolator by iterating
	 * through the list of fallbacks defined in the FALLBACKS array. It tries to retrieve
	 * the fallback MessageInterpolator using each fallback name in the array, and if
	 * successful, returns the fallback MessageInterpolator. If an exception occurs during
	 * the retrieval process, it is caught and the method continues to the next fallback
	 * name. If no fallback MessageInterpolator is found, null is returned.
	 * @return the fallback MessageInterpolator, or null if no fallback is found
	 */
	private MessageInterpolator getFallback() {
		for (String fallback : FALLBACKS) {
			try {
				return getFallback(fallback);
			}
			catch (Exception ex) {
				// Swallow and continue
			}
		}
		return null;
	}

	/**
	 * Returns a fallback MessageInterpolator based on the provided class name.
	 * @param fallback the class name of the fallback MessageInterpolator
	 * @return the fallback MessageInterpolator instance
	 */
	private MessageInterpolator getFallback(String fallback) {
		Class<?> interpolatorClass = ClassUtils.resolveClassName(fallback, null);
		Object interpolator = BeanUtils.instantiateClass(interpolatorClass);
		return (MessageInterpolator) interpolator;
	}

}
