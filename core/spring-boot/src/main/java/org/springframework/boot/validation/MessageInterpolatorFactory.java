/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.validation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.validation.MessageInterpolator;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import org.jspecify.annotations.Nullable;

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

	private final @Nullable MessageSource messageSource;

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
	public MessageInterpolatorFactory(@Nullable MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
	public MessageInterpolator getObject() throws BeansException {
		MessageInterpolator messageInterpolator = getMessageInterpolator();
		if (this.messageSource != null) {
			return new MessageSourceMessageInterpolator(this.messageSource, messageInterpolator);
		}
		return messageInterpolator;
	}

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

	private @Nullable MessageInterpolator getFallback() {
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

	private MessageInterpolator getFallback(String fallback) {
		Class<?> interpolatorClass = ClassUtils.resolveClassName(fallback, null);
		Object interpolator = BeanUtils.instantiateClass(interpolatorClass);
		return (MessageInterpolator) interpolator;
	}

}
