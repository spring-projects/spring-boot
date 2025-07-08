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

import org.springframework.web.servlet.function.HandlerFunction;

/**
 * Description of a {@link HandlerFunction}.
 *
 * @author Xiong Tang
 * @since 4.0.0
 */
public class HandlerFunctionDescription {

	private final String className;

	HandlerFunctionDescription(HandlerFunction<?> handlerFunction) {
		this.className = getHandlerFunctionClassName(handlerFunction);
	}

	private static String getHandlerFunctionClassName(HandlerFunction<?> handlerFunction) {
		Class<?> functionClass = handlerFunction.getClass();
		String canonicalName = functionClass.getCanonicalName();
		return (canonicalName != null) ? canonicalName : functionClass.getName();
	}

	public String getClassName() {
		return this.className;
	}

}
