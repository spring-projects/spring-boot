/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.autoconfigure;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webflux.autoconfigure.WebFluxProperties.Forwardedheaders;
import org.springframework.boot.webflux.autoconfigure.WebFluxProperties.HeaderFormat;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ContextPathCompositeHandler;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link HttpHandler}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author Lasse Wulff
 * @since 4.0.0
 */
@AutoConfiguration(after = { WebFluxAutoConfiguration.class })
@ConditionalOnClass({ DispatcherHandler.class, HttpHandler.class })
@ConditionalOnWebApplication(type = Type.REACTIVE)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10)
public final class HttpHandlerAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(HttpHandler.class)
	static class AnnotationConfig {

		private final ApplicationContext applicationContext;

		AnnotationConfig(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Bean
		HttpHandler httpHandler(ObjectProvider<WebFluxProperties> propsProvider,
				ObjectProvider<WebHttpHandlerBuilderCustomizer> handlerBuilderCustomizers) {
			WebFluxProperties properties = propsProvider.getIfAvailable();
			WebHttpHandlerBuilder handlerBuilder = WebHttpHandlerBuilder.applicationContext(this.applicationContext);
			if (properties != null) {
				handlerBuilder.defaultHtmlEscape(properties.getDefaultHtmlEscape());
			}
			handlerBuilderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(handlerBuilder));
			HttpHandler httpHandler = handlerBuilder.build();
			if (properties != null && StringUtils.hasText(properties.getBasePath())) {
				Map<String, HttpHandler> handlersMap = Collections.singletonMap(properties.getBasePath(), httpHandler);
				return new ContextPathCompositeHandler(handlersMap);
			}
			return httpHandler;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(name = "server.forward-headers-strategy", havingValue = "framework")
	@EnableConfigurationProperties(WebFluxProperties.class)
	static class ForwardedHeaderTransformerConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ForwardedHeaderTransformer forwardedHeaderTransformer(WebFluxProperties webFluxProperties) {
			Forwardedheaders properties = webFluxProperties.getForwardedHeaders();
			ForwardedHeaderTransformer transformer = new ForwardedHeaderTransformer(
					properties.getHeaderFormat() == HeaderFormat.STANDARD);
			transformer.setUseForwardedPrefix(properties.isUseForwardedPrefix());
			return transformer;
		}

	}

}
