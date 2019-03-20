/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.context.ApplicationContext;

/**
 * Provides access to the {@link ApplicationContext} being used to manage actuator
 * endpoints.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
public class ManagementContextResolver {

	private ApplicationContext applicationContext;

	ManagementContextResolver(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Return all {@link MvcEndpoints} from the management context.
	 * @return {@link MvcEndpoints} from the management context
	 */
	public MvcEndpoints getMvcEndpoints() {
		try {
			return getApplicationContext().getBean(MvcEndpoints.class);
		}
		catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Return the management {@link ApplicationContext}.
	 * @return the management {@link ApplicationContext}
	 */
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

}
