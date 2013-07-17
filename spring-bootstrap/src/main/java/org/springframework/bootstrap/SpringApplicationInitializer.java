/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.bootstrap;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * Strategy interface that can be used to initialize a {@link SpringApplication} before it
 * is runs. A {@link SpringApplicationInitializer} can optionally implement
 * {@link EnvironmentAware} if it needs to access or configure the underling application
 * {@link Environment}.
 * 
 * @author Phillip Webb
 */
public interface SpringApplicationInitializer {

	/**
	 * Initialize the application
	 * @param springApplication the spring application.
	 */
	void initialize(SpringApplication springApplication);

}
