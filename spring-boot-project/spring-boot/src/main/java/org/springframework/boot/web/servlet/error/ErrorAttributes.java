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

package org.springframework.boot.web.servlet.error;

import java.util.Collections;
import java.util.Map;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

/**
 * Provides access to error attributes which can be logged or presented to the user.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.0.0
 * @see DefaultErrorAttributes
 */
public interface ErrorAttributes {

	/**
	 * Name of the {@link javax.servlet.http.HttpServletRequest#getAttribute(String)
	 * request attribute} holding the error resolved by the {@code ErrorAttributes}
	 * implementation.
	 * @since 2.5.0
	 */
	String ERROR_ATTRIBUTE = ErrorAttributes.class.getName() + ".error";

	/**
	 * Returns a {@link Map} of the error attributes. The map can be used as the model of
	 * an error page {@link ModelAndView}, or returned as a
	 * {@link ResponseBody @ResponseBody}.
	 * @param webRequest the source request
	 * @param options options for error attribute contents
	 * @return a map of error attributes
	 * @since 2.3.0
	 */
	default Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
		return Collections.emptyMap();
	}

	/**
	 * Return the underlying cause of the error or {@code null} if the error cannot be
	 * extracted.
	 * @param webRequest the source request
	 * @return the {@link Exception} that caused the error or {@code null}
	 */
	Throwable getError(WebRequest webRequest);

}
