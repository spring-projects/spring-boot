/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.autoconfigure.error;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.boot.webflux.error.DefaultErrorAttributes;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
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
 * @since 4.0.0
 */
@AutoConfiguration(before = WebFluxAutoConfiguration.class)
@ConditionalOnWebApplication(type = Type.REACTIVE)
@ConditionalOnClass(WebFluxConfigurer.class)
@EnableConfigurationProperties(WebProperties.class)
public final class ErrorWebFluxAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(search = SearchStrategy.CURRENT)
	@Order(-1)
	ErrorWebExceptionHandler errorWebExceptionHandler(ErrorAttributes errorAttributes, WebProperties webProperties,
			ObjectProvider<ViewResolver> viewResolvers, ServerCodecConfigurer serverCodecConfigurer,
			ApplicationContext applicationContext) {
		DefaultErrorWebExceptionHandler exceptionHandler = new DefaultErrorWebExceptionHandler(errorAttributes,
				webProperties.getResources(), webProperties.getError(), applicationContext);
		exceptionHandler.setViewResolvers(viewResolvers.orderedStream().toList());
		exceptionHandler.setMessageWriters(serverCodecConfigurer.getWriters());
		exceptionHandler.setMessageReaders(serverCodecConfigurer.getReaders());
		return exceptionHandler;
	}

	@Bean
	@ConditionalOnMissingBean(value = ErrorAttributes.class, search = SearchStrategy.CURRENT)
	DefaultErrorAttributes errorAttributes() {
		return new DefaultErrorAttributes();
	}

}
