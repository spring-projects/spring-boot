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
package org.springframework.boot.autoconfigure.web.reactive;

import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;

/**
 * Interface to register key components of the {@link WebFluxAutoConfiguration} in place
 * of the default ones provided by Spring WebFlux.
 * <p>
 * All custom instances are later processed by Boot and Spring WebFlux configurations. A
 * single instance of this component should be registered, otherwise making it impossible
 * to choose from redundant WebFlux components.
 *
 * @author Artsiom Yudovin
 * @since 2.1.0
 * @see org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration.EnableWebFluxConfiguration
 */
public interface WebFluxRegistrations {

	/**
	 * Return the custom {@link RequestMappingHandlerMapping} that should be used and
	 * processed by the WebFlux configuration.
	 * @return the custom {@link RequestMappingHandlerMapping} instance
	 */
	default RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
		return null;
	}

	/**
	 * Return the custom {@link RequestMappingHandlerAdapter} that should be used and
	 * processed by the WebFlux configuration.
	 * @return the custom {@link RequestMappingHandlerAdapter} instance
	 */
	default RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
		return null;
	}

}
