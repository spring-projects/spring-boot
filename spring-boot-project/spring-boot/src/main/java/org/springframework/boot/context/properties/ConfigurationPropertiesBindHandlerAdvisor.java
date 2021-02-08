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

package org.springframework.boot.context.properties;

import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindHandler;

/**
 * Allows additional functionality to be applied to the {@link BindHandler} used by the
 * {@link ConfigurationPropertiesBindingPostProcessor}.
 *
 * @author Phillip Webb
 * @since 2.1.0
 * @see AbstractBindHandler
 */
@FunctionalInterface
public interface ConfigurationPropertiesBindHandlerAdvisor {

	/**
	 * Apply additional functionality to the source bind handler.
	 * @param bindHandler the source bind handler
	 * @return a replacement bind handler that delegates to the source and provides
	 * additional functionality
	 */
	BindHandler apply(BindHandler bindHandler);

}
