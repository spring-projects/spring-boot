/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Interface to register key components of the {@link WebMvcConfigurationSupport} in place
 * of the default ones provided by Spring MVC.
 * <p>
 * All custom instances are later processed by Boot and Spring MVC configurations. A
 * single instance of this component should be registered, otherwise making it impossible
 * to choose from redundant MVC components.
 *
 * @author Brian Clozel
 * @since 2.0.0
 * @see org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration.EnableWebMvcConfiguration
 */
public interface WebMvcRegistrations {

	/**
	 * Return the custom {@link RequestMappingHandlerMapping} that should be used and
	 * processed by the MVC configuration.
	 * @return the custom {@link RequestMappingHandlerMapping} instance
	 */
	default RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
		return null;
	}

	/**
	 * Return the custom {@link RequestMappingHandlerAdapter} that should be used and
	 * processed by the MVC configuration.
	 * @return the custom {@link RequestMappingHandlerAdapter} instance
	 */
	default RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
		return null;
	}

	/**
	 * Return the custom {@link ExceptionHandlerExceptionResolver} that should be used and
	 * processed by the MVC configuration.
	 * @return the custom {@link ExceptionHandlerExceptionResolver} instance
	 */
	default ExceptionHandlerExceptionResolver getExceptionHandlerExceptionResolver() {
		return null;
	}

}
