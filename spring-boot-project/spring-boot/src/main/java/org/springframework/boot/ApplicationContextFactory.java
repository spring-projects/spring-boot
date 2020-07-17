/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Strategy interface for creating the {@link ConfigurableApplicationContext} used by a
 * {@link SpringApplication}. Created contexts should be returned in their default form,
 * with the {@code SpringApplication} responsible for configuring and refreshing the
 * context.
 *
 * @author Andy Wilkinson
 * @since 2.4.0
 */
@FunctionalInterface
public interface ApplicationContextFactory {

	/**
	 * Creates the {@link ConfigurableApplicationContext application context} for a
	 * {@link SpringApplication}, respecting the given {@code webApplicationType}.
	 * @param webApplicationType the web application type
	 * @return the newly created application context
	 */
	ConfigurableApplicationContext create(WebApplicationType webApplicationType);

	/**
	 * Creates an {@code ApplicationContextFactory} that will create contexts by
	 * instantiating the given {@code contextClass} via its primary constructor.
	 * @param contextClass the context class
	 * @return the factory that will instantiate the context class
	 * @see BeanUtils#instantiateClass(Class)
	 */
	static ApplicationContextFactory forContextClass(Class<? extends ConfigurableApplicationContext> contextClass) {
		return (webApplicationType) -> BeanUtils.instantiateClass(contextClass);
	}

}
