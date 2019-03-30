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

package org.springframework.boot.actuate.web.mappings;

import org.springframework.asm.Type;
import org.springframework.web.method.HandlerMethod;

/**
 * A description of a {@link HandlerMethod}.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class HandlerMethodDescription {

	private final String className;

	private final String name;

	private final String descriptor;

	public HandlerMethodDescription(HandlerMethod handlerMethod) {
		this.name = handlerMethod.getMethod().getName();
		this.className = handlerMethod.getMethod().getDeclaringClass().getCanonicalName();
		this.descriptor = Type.getMethodDescriptor(handlerMethod.getMethod());
	}

	public String getName() {
		return this.name;
	}

	public String getDescriptor() {
		return this.descriptor;
	}

	public String getClassName() {
		return this.className;
	}

}
