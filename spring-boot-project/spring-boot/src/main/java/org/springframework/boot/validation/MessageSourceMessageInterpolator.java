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

package org.springframework.boot.validation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import jakarta.validation.MessageInterpolator;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Resolves any message parameters through {@link MessageSource} and then interpolates a
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

	/**
     * Constructs a new MessageSourceMessageInterpolator with the specified MessageSource and MessageInterpolator.
     * 
     * @param messageSource the MessageSource to be used for resolving messages
     * @param messageInterpolator the MessageInterpolator to be used for interpolating message parameters
     */
    MessageSourceMessageInterpolator(MessageSource messageSource, MessageInterpolator messageInterpolator) {
		this.messageSource = messageSource;
		this.messageInterpolator = messageInterpolator;
	}

	/**
     * Interpolates the given message template with the given context and returns the interpolated string.
     * Uses the current locale obtained from the LocaleContextHolder.
     *
     * @param messageTemplate the message template to be interpolated
     * @param context the context containing the variables to be replaced in the message template
     * @return the interpolated string
     */
    @Override
	public String interpolate(String messageTemplate, Context context) {
		return interpolate(messageTemplate, context, LocaleContextHolder.getLocale());
	}

	/**
     * Interpolates the given message template with the provided context and locale.
     * 
     * @param messageTemplate the message template to be interpolated
     * @param context the context containing the variables to be replaced in the message template
     * @param locale the locale in which the message should be interpolated
     * @return the interpolated message
     */
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

	/**
     * Replaces parameters in the given message with their corresponding values.
     * 
     * @param message the message string to replace parameters in
     * @param locale the locale to use for parameter replacement
     * @param visitedParameters a set of visited parameters to detect circular references
     * @return the message string with parameters replaced
     * @throws IllegalArgumentException if a circular reference is detected
     */
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

	/**
     * Replaces the parameter in the given message with its corresponding value from the message source.
     * 
     * @param parameter the parameter to be replaced
     * @param locale the locale to be used for message retrieval
     * @param visitedParameters a set of visited parameters to avoid infinite recursion
     * @return the message with the parameter replaced by its value, or null if the value is not found or is the same as the parameter
     */
    private String replaceParameter(String parameter, Locale locale, Set<String> visitedParameters) {
		parameter = replaceParameters(parameter, locale, visitedParameters);
		String value = this.messageSource.getMessage(parameter, null, null, locale);
		return (value != null && !isUsingCodeAsDefaultMessage(value, parameter))
				? replaceParameters(value, locale, visitedParameters) : null;
	}

	/**
     * Checks if the given value is using the code as the default message.
     * 
     * @param value the value to check
     * @param parameter the parameter to compare with the value
     * @return true if the value is using the code as the default message, false otherwise
     */
    private boolean isUsingCodeAsDefaultMessage(String value, String parameter) {
		return value.equals(parameter);
	}

}
