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

package org.springframework.boot.web.context;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.web.server.WebServerFactory;

/**
 * Exception thrown when there is no {@link WebServerFactory} bean of the required type
 * defined in a {@link WebServerApplicationContext}.
 *
 * @author Guirong Hu
 * @author Andy Wilkinson
 * @since 2.7.0
 */
public class MissingWebServerFactoryBeanException extends NoSuchBeanDefinitionException {

	private final WebApplicationType webApplicationType;

	/**
	 * Create a new {@code MissingWebServerFactoryBeanException}.
	 * @param webServerApplicationContextClass the class of the
	 * WebServerApplicationContext that required the WebServerFactory
	 * @param webServerFactoryClass the class of the WebServerFactory that was missing
	 * @param webApplicationType the type of the web application
	 */
	public MissingWebServerFactoryBeanException(
			Class<? extends WebServerApplicationContext> webServerApplicationContextClass,
			Class<? extends WebServerFactory> webServerFactoryClass, WebApplicationType webApplicationType) {
		super(webServerFactoryClass, String.format("Unable to start %s due to missing %s bean",
				webServerApplicationContextClass.getSimpleName(), webServerFactoryClass.getSimpleName()));
		this.webApplicationType = webApplicationType;
	}

	/**
	 * Returns the type of web application for which a {@link WebServerFactory} bean was
	 * missing.
	 * @return the type of web application
	 */
	public WebApplicationType getWebApplicationType() {
		return this.webApplicationType;
	}

}
