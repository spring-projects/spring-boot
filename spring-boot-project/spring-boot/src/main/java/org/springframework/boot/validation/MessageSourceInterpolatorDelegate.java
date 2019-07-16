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

package org.springframework.boot.validation;

import java.util.Locale;

import javax.validation.MessageInterpolator;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Resolves any message parameters via {@link MessageSource} and then interpolates a
 * message using the underlying {@link MessageInterpolator}.
 *
 * @author Dmytro Nosan
 */
class MessageSourceInterpolatorDelegate implements MessageInterpolator {

	private static final MessageParameterPlaceholderHelper helper = new MessageParameterPlaceholderHelper();

	private final MessageSource messageSource;

	private final MessageInterpolator messageInterpolator;

	MessageSourceInterpolatorDelegate(MessageSource messageSource, MessageInterpolator messageInterpolator) {
		this.messageSource = messageSource;
		this.messageInterpolator = messageInterpolator;
	}

	@Override
	public String interpolate(String messageTemplate, Context context) {
		return interpolate(messageTemplate, context, LocaleContextHolder.getLocale());
	}

	@Override
	public String interpolate(String messageTemplate, Context context, Locale locale) {
		String message = helper.replaceParameters(messageTemplate,
				(parameter) -> this.messageSource.getMessage(parameter, null, null, locale));
		return this.messageInterpolator.interpolate(message, context, locale);
	}

}
