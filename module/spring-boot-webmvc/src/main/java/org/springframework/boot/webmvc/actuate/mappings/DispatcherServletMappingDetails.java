/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.webmvc.actuate.mappings;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.web.mappings.HandlerMethodDescription;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Details of a {@link DispatcherServlet} mapping.
 *
 * @author Andy Wilkinson
 * @author Xiong Tang
 * @since 4.0.0
 */
public class DispatcherServletMappingDetails {

	private @Nullable HandlerMethodDescription handlerMethod;

	private @Nullable HandlerFunctionDescription handlerFunction;

	private @Nullable RequestMappingConditionsDescription requestMappingConditions;

	public @Nullable HandlerMethodDescription getHandlerMethod() {
		return this.handlerMethod;
	}

	void setHandlerMethod(@Nullable HandlerMethodDescription handlerMethod) {
		this.handlerMethod = handlerMethod;
	}

	public @Nullable HandlerFunctionDescription getHandlerFunction() {
		return this.handlerFunction;
	}

	void setHandlerFunction(@Nullable HandlerFunctionDescription handlerFunction) {
		this.handlerFunction = handlerFunction;
	}

	public @Nullable RequestMappingConditionsDescription getRequestMappingConditions() {
		return this.requestMappingConditions;
	}

	void setRequestMappingConditions(@Nullable RequestMappingConditionsDescription requestMappingConditions) {
		this.requestMappingConditions = requestMappingConditions;
	}

}
