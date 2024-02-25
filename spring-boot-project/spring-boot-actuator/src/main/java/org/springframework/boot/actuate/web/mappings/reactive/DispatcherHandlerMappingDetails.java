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

	/**
     * Returns the handler method associated with this DispatcherHandlerMappingDetails.
     *
     * @return the handler method
     */
    public HandlerMethodDescription getHandlerMethod() {
		return this.handlerMethod;
	}

	/**
     * Sets the handler method for this DispatcherHandlerMappingDetails.
     * 
     * @param handlerMethod the HandlerMethodDescription to set as the handler method
     */
    void setHandlerMethod(HandlerMethodDescription handlerMethod) {
		this.handlerMethod = handlerMethod;
	}

	/**
     * Returns the HandlerFunctionDescription associated with this DispatcherHandlerMappingDetails.
     *
     * @return the HandlerFunctionDescription associated with this DispatcherHandlerMappingDetails
     */
    public HandlerFunctionDescription getHandlerFunction() {
		return this.handlerFunction;
	}

	/**
     * Sets the handler function for the DispatcherHandlerMappingDetails.
     * 
     * @param handlerFunction the HandlerFunctionDescription to be set
     */
    void setHandlerFunction(HandlerFunctionDescription handlerFunction) {
		this.handlerFunction = handlerFunction;
	}

	/**
     * Returns the RequestMappingConditionsDescription object that represents the request mapping conditions
     * for this DispatcherHandlerMappingDetails instance.
     *
     * @return the RequestMappingConditionsDescription object representing the request mapping conditions
     */
    public RequestMappingConditionsDescription getRequestMappingConditions() {
		return this.requestMappingConditions;
	}

	/**
     * Sets the request mapping conditions for this DispatcherHandlerMappingDetails.
     * 
     * @param requestMappingConditions the request mapping conditions to be set
     */
    void setRequestMappingConditions(RequestMappingConditionsDescription requestMappingConditions) {
		this.requestMappingConditions = requestMappingConditions;
	}

}
