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

package org.springframework.boot.web.servlet.context;

import org.springframework.aot.AotDetector;
import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * {@link ApplicationContextFactory} registered in {@code spring.factories} to support
 * {@link AnnotationConfigServletWebServerApplicationContext} and
 * {@link ServletWebServerApplicationContext}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ServletWebServerApplicationContextFactory implements ApplicationContextFactory {

	/**
     * Returns the environment type based on the web application type.
     * 
     * @param webApplicationType the type of web application
     * @return the environment type if the web application type is SERVLET, otherwise null
     */
    @Override
	public Class<? extends ConfigurableEnvironment> getEnvironmentType(WebApplicationType webApplicationType) {
		return (webApplicationType != WebApplicationType.SERVLET) ? null : ApplicationServletEnvironment.class;
	}

	/**
     * Creates a configurable environment based on the specified web application type.
     * 
     * @param webApplicationType the type of web application
     * @return a configurable environment if the web application type is SERVLET, otherwise null
     */
    @Override
	public ConfigurableEnvironment createEnvironment(WebApplicationType webApplicationType) {
		return (webApplicationType != WebApplicationType.SERVLET) ? null : new ApplicationServletEnvironment();
	}

	/**
     * Create a new ConfigurableApplicationContext based on the specified WebApplicationType.
     * 
     * @param webApplicationType the type of web application
     * @return the created ConfigurableApplicationContext, or null if the web application type is not SERVLET
     */
    @Override
	public ConfigurableApplicationContext create(WebApplicationType webApplicationType) {
		return (webApplicationType != WebApplicationType.SERVLET) ? null : createContext();
	}

	/**
     * Creates and returns a new ConfigurableApplicationContext based on the AotDetector's useGeneratedArtifacts() method.
     * If useGeneratedArtifacts() returns false, an AnnotationConfigServletWebServerApplicationContext is created and returned.
     * If useGeneratedArtifacts() returns true, a ServletWebServerApplicationContext is created and returned.
     *
     * @return a new ConfigurableApplicationContext based on the AotDetector's useGeneratedArtifacts() method
     */
    private ConfigurableApplicationContext createContext() {
		if (!AotDetector.useGeneratedArtifacts()) {
			return new AnnotationConfigServletWebServerApplicationContext();
		}
		return new ServletWebServerApplicationContext();
	}

}
