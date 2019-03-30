/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.web.mappings.reactive;

import org.springframework.boot.actuate.web.mappings.HandlerMethodDescription;
import org.springframework.web.reactive.DispatcherHandler;

/**
 * Details of a {@link DispatcherHandler} mapping.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class DispatcherHandlerMappingDetails {

	private HandlerMethodDescription handlerMethod;

	private HandlerFunctionDescription handlerFunction;

	private RequestMappingConditionsDescription requestMappingConditions;

	public HandlerMethodDescription getHandlerMethod() {
		return this.handlerMethod;
	}

	void setHandlerMethod(HandlerMethodDescription handlerMethod) {
		this.handlerMethod = handlerMethod;
	}

	public HandlerFunctionDescription getHandlerFunction() {
		return this.handlerFunction;
	}

	void setHandlerFunction(HandlerFunctionDescription handlerFunction) {
		this.handlerFunction = handlerFunction;
	}

	public RequestMappingConditionsDescription getRequestMappingConditions() {
		return this.requestMappingConditions;
	}

	void setRequestMappingConditions(
			RequestMappingConditionsDescription requestMappingConditions) {
		this.requestMappingConditions = requestMappingConditions;
	}

}
