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

package org.springframework.boot.validation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.validation.MessageInterpolator;
import javax.validation.Validation;
import javax.validation.ValidationException;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.util.ClassUtils;

/**
 * {@link ObjectFactory} that can be used to create a {@link MessageInterpolator}.
 * Attempts to pick the most appropriate {@link MessageInterpolator} based on the
 * classpath.
 *
 * @author Phillip Webb
 */
public class MessageInterpolatorFactory implements ObjectFactory<MessageInterpolator> {

	private static final Set<String> FALLBACKS;

	static {
		Set<String> fallbacks = new LinkedHashSet<>();
		fallbacks.add("org.hibernate.validator.messageinterpolation"
				+ ".ParameterMessageInterpolator");
		FALLBACKS = Collections.unmodifiableSet(fallbacks);
	}

	@Override
	public MessageInterpolator getObject() throws BeansException {
		try {
			return Validation.byDefaultProvider().configure()
					.getDefaultMessageInterpolator();
		}
		catch (ValidationException ex) {
			MessageInterpolator fallback = getFallback();
			if (fallback != null) {
				return fallback;
			}
			throw ex;
		}
	}

	private MessageInterpolator getFallback() {
		for (String fallback : FALLBACKS) {
			try {
				return getFallback(fallback);
			}
			catch (Exception ex) {
				// Swallow an continue
			}
		}
		return null;
	}

	private MessageInterpolator getFallback(String fallback) {
		Class<?> interpolatorClass = ClassUtils.resolveClassName(fallback, null);
		Object interpolator = BeanUtils.instantiateClass(interpolatorClass);
		return (MessageInterpolator) interpolator;
	}

}
