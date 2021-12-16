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

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import javax.validation.MessageInterpolator;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Resolves any message parameters via {@link MessageSource} and then interpolates a
 * message using the underlying {@link MessageInterpolator}.
 *
 * @author Dmytro Nosan
 * @author Scott Frederick
 */
class MessageSourceMessageInterpolator implements MessageInterpolator {

	private static final char PREFIX = '{';

	private static final char SUFFIX = '}';

	private static final char ESCAPE = '\\';

	private final MessageSource messageSource;

	private final MessageInterpolator messageInterpolator;

	MessageSourceMessageInterpolator(MessageSource messageSource, MessageInterpolator messageInterpolator) {
		this.messageSource = messageSource;
		this.messageInterpolator = messageInterpolator;
	}

	@Override
	public String interpolate(String messageTemplate, Context context) {
		return interpolate(messageTemplate, context, LocaleContextHolder.getLocale());
	}

	@Override
	public String interpolate(String messageTemplate, Context context, Locale locale) {
		String message = replaceParameters(messageTemplate, locale);
		return this.messageInterpolator.interpolate(message, context, locale);
	}

	/**
	 * Recursively replaces all message parameters.
	 * <p>
	 * The message parameter prefix <code>&#123;</code> and suffix <code>&#125;</code> can
	 * be escaped using {@code \}, e.g. <code>\&#123;escaped\&#125;</code>.
	 * @param message the message containing the parameters to be replaced
	 * @param locale the locale to use when resolving replacements
	 * @return the message with parameters replaced
	 */
	private String replaceParameters(String message, Locale locale) {
		return replaceParameters(message, locale, new LinkedHashSet<>(4));
	}

	private String replaceParameters(String message, Locale locale, Set<String> visitedParameters) {
		StringBuilder buf = new StringBuilder(message);
		int parentheses = 0;
		int startIndex = -1;
		int endIndex = -1;
		for (int i = 0; i < buf.length(); i++) {
			if (buf.charAt(i) == ESCAPE) {
				i++;
			}
			else if (buf.charAt(i) == PREFIX) {
				if (startIndex == -1) {
					startIndex = i;
				}
				parentheses++;
			}
			else if (buf.charAt(i) == SUFFIX) {
				if (parentheses > 0) {
					parentheses--;
				}
				endIndex = i;
			}
			if (parentheses == 0 && startIndex < endIndex) {
				String parameter = buf.substring(startIndex + 1, endIndex);
				if (!visitedParameters.add(parameter)) {
					throw new IllegalArgumentException("Circular reference '{" + String.join(" -> ", visitedParameters)
							+ " -> " + parameter + "}'");
				}
				String value = replaceParameter(parameter, locale, visitedParameters);
				if (value != null) {
					buf.replace(startIndex, endIndex + 1, value);
					i = startIndex + value.length() - 1;
				}
				visitedParameters.remove(parameter);
				startIndex = -1;
				endIndex = -1;
			}
		}
		return buf.toString();
	}

	private String replaceParameter(String parameter, Locale locale, Set<String> visitedParameters) {
		parameter = replaceParameters(parameter, locale, visitedParameters);
		String value = this.messageSource.getMessage(parameter, null, null, locale);
		return (value != null && !isUsingCodeAsDefaultMessage(value, parameter))
				? replaceParameters(value, locale, visitedParameters) : null;
	}

	private boolean isUsingCodeAsDefaultMessage(String value, String parameter) {
		return value.equals(parameter);
	}

}
