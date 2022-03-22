/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.web.server.context;

import org.springframework.boot.WebApplicationType;
import org.springframework.context.ApplicationContextException;
import org.springframework.lang.NonNull;

/**
 * Throws exception when web server factory bean is missing.
 *
 * @author Guirong Hu
 * @since 2.7.0
 */
@SuppressWarnings("serial")
public class MissingWebServerFactoryBeanException extends ApplicationContextException {

	private final WebApplicationType webApplicationType;

	/**
	 * Create a new {@code MissingWebServerFactoryBeanException} with the given web
	 * application context class and the given web server factory class and the given type
	 * of web application.
	 * @param webApplicationContextClass the web application context class
	 * @param webServerFactoryClass the web server factory class
	 * @param webApplicationType the type of web application
	 */
	public MissingWebServerFactoryBeanException(@NonNull Class<?> webApplicationContextClass,
			@NonNull Class<?> webServerFactoryClass, @NonNull WebApplicationType webApplicationType) {
		super(String.format("Unable to start %s due to missing %s bean.", webApplicationContextClass.getSimpleName(),
				webServerFactoryClass.getSimpleName()));
		this.webApplicationType = webApplicationType;
	}

	/**
	 * Returns the type of web application that is being run.
	 * @return the type of web application
	 * @since 2.7.0
	 */
	public WebApplicationType getWebApplicationType() {
		return this.webApplicationType;
	}

}
