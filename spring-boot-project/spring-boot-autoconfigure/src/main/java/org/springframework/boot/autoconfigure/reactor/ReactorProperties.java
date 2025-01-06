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

package org.springframework.boot.autoconfigure.reactor;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Reactor.
 *
 * @author Brian Clozel
 * @since 3.2.0
 */
@ConfigurationProperties(prefix = "spring.reactor")
public class ReactorProperties {

	/**
	 * Context Propagation support mode for Reactor operators.
	 */
	private ContextPropagationMode contextPropagation = ContextPropagationMode.LIMITED;

	public ContextPropagationMode getContextPropagation() {
		return this.contextPropagation;
	}

	public void setContextPropagation(ContextPropagationMode contextPropagation) {
		this.contextPropagation = contextPropagation;
	}

	public enum ContextPropagationMode {

		/**
		 * Context Propagation is applied to all Reactor operators.
		 */
		AUTO,

		/**
		 * Context Propagation is only applied to "tap" and "handle" Reactor operators.
		 */
		LIMITED

	}

}
