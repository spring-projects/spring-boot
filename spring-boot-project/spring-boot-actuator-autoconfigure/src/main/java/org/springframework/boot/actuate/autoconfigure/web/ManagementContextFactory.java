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

package org.springframework.boot.actuate.autoconfigure.web;

import org.springframework.boot.web.context.ConfigurableWebServerApplicationContext;
import org.springframework.context.ApplicationContext;

/**
 * Factory for creating a separate management context when the management web server is
 * running on a different port to the main application.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@FunctionalInterface
public interface ManagementContextFactory {

	/**
	 * Create the management application context.
	 * @param parent the parent context
	 * @param configurationClasses the configuration classes
	 * @return a configured application context
	 */
	ConfigurableWebServerApplicationContext createManagementContext(ApplicationContext parent,
			Class<?>... configurationClasses);

}
