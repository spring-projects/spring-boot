/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.reactive.error;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to render errors through a WebFlux
 * {@link org.springframework.web.server.WebExceptionHandler}.
 *
 * @author Brian Clozel
 * @author Scott Frederick
 * @since 2.0.0
 */
@AutoConfiguration(before = WebFluxAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(WebFluxConfigurer.class)
@EnableConfigurationProperties({ ServerProperties.class, WebProperties.class })
public class ErrorWebFluxAutoConfiguration {

	private final ServerProperties serverProperties;

	/**
     * Constructs a new ErrorWebFluxAutoConfiguration with the specified ServerProperties.
     *
     * @param serverProperties the ServerProperties to be used by the ErrorWebFluxAutoConfiguration
     */
    public ErrorWebFluxAutoConfiguration(ServerProperties serverProperties) {
		this.serverProperties = serverProperties;
	}

	/**
     * Create a bean of type {@link ErrorWebExceptionHandler} if no other bean of the same type is present in the application context.
     * The bean is conditionally created based on the absence of a bean of type {@link ErrorWebExceptionHandler} and is ordered with a priority of -1.
     * 
     * @param errorAttributes the error attributes to be used by the exception handler
     * @param webProperties the web properties to be used by the exception handler
     * @param viewResolvers the view resolvers to be used by the exception handler
     * @param serverCodecConfigurer the server codec configurer to be used by the exception handler
     * @param applicationContext the application context to be used by the exception handler
     * @return the created {@link ErrorWebExceptionHandler} bean
     */
    @Bean
	@ConditionalOnMissingBean(value = ErrorWebExceptionHandler.class, search = SearchStrategy.CURRENT)
	@Order(-1)
	public ErrorWebExceptionHandler errorWebExceptionHandler(ErrorAttributes errorAttributes,
			WebProperties webProperties, ObjectProvider<ViewResolver> viewResolvers,
			ServerCodecConfigurer serverCodecConfigurer, ApplicationContext applicationContext) {
		DefaultErrorWebExceptionHandler exceptionHandler = new DefaultErrorWebExceptionHandler(errorAttributes,
				webProperties.getResources(), this.serverProperties.getError(), applicationContext);
		exceptionHandler.setViewResolvers(viewResolvers.orderedStream().toList());
		exceptionHandler.setMessageWriters(serverCodecConfigurer.getWriters());
		exceptionHandler.setMessageReaders(serverCodecConfigurer.getReaders());
		return exceptionHandler;
	}

	/**
     * Creates a new instance of {@link DefaultErrorAttributes} if no other bean of type {@link ErrorAttributes} is present in the application context.
     * 
     * @return the {@link DefaultErrorAttributes} instance
     */
    @Bean
	@ConditionalOnMissingBean(value = ErrorAttributes.class, search = SearchStrategy.CURRENT)
	public DefaultErrorAttributes errorAttributes() {
		return new DefaultErrorAttributes();
	}

}
