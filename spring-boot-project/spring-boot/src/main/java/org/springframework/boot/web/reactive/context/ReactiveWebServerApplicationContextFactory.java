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

package org.springframework.boot.web.reactive.context;

import org.springframework.aot.AotDetector;
import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * {@link ApplicationContextFactory} registered in {@code spring.factories} to support
 * {@link AnnotationConfigReactiveWebServerApplicationContext} and
 * {@link ReactiveWebServerApplicationContext}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ReactiveWebServerApplicationContextFactory implements ApplicationContextFactory {

	/**
     * Returns the environment type based on the web application type.
     * 
     * @param webApplicationType the type of web application
     * @return the environment type if the web application type is reactive, otherwise null
     */
    @Override
	public Class<? extends ConfigurableEnvironment> getEnvironmentType(WebApplicationType webApplicationType) {
		return (webApplicationType != WebApplicationType.REACTIVE) ? null : ApplicationReactiveWebEnvironment.class;
	}

	/**
     * Creates a configurable environment based on the specified web application type.
     * 
     * @param webApplicationType the type of web application
     * @return the created environment, or null if the web application type is not reactive
     */
    @Override
	public ConfigurableEnvironment createEnvironment(WebApplicationType webApplicationType) {
		return (webApplicationType != WebApplicationType.REACTIVE) ? null : new ApplicationReactiveWebEnvironment();
	}

	/**
     * Creates a new ConfigurableApplicationContext based on the specified WebApplicationType.
     * 
     * @param webApplicationType the type of web application
     * @return the created ConfigurableApplicationContext, or null if the web application type is not reactive
     */
    @Override
	public ConfigurableApplicationContext create(WebApplicationType webApplicationType) {
		return (webApplicationType != WebApplicationType.REACTIVE) ? null : createContext();
	}

	/**
     * Creates a new ConfigurableApplicationContext based on the current environment.
     * If the AotDetector does not use generated artifacts, an AnnotationConfigReactiveWebServerApplicationContext is created.
     * Otherwise, a ReactiveWebServerApplicationContext is created.
     *
     * @return the newly created ConfigurableApplicationContext
     */
    private ConfigurableApplicationContext createContext() {
		if (!AotDetector.useGeneratedArtifacts()) {
			return new AnnotationConfigReactiveWebServerApplicationContext();
		}
		return new ReactiveWebServerApplicationContext();
	}

}
