/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.server.servlet.undertow;

import io.undertow.Undertow;
import io.undertow.websockets.jsr.Bootstrap;
import jakarta.servlet.ServletRequest;
import org.xnio.SslClientAuthMode;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.server.servlet.ServletWebServerConfiguration;
import org.springframework.boot.autoconfigure.web.server.undertow.UndertowWebServerConfiguration;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.boot.web.server.servlet.undertow.UndertowDeploymentInfoCustomizer;
import org.springframework.boot.web.server.servlet.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.undertow.UndertowBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for an Undertow-based servlet web
 * server.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ ServletRequest.class, Undertow.class, SslClientAuthMode.class })
@ConditionalOnWebApplication(type = Type.SERVLET)
@Import({ UndertowWebServerConfiguration.class, ServletWebServerConfiguration.class })
public class UndertowServletWebServerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT)
	UndertowServletWebServerFactory undertowServletWebServerFactory(
			ObjectProvider<UndertowDeploymentInfoCustomizer> deploymentInfoCustomizers,
			ObjectProvider<UndertowBuilderCustomizer> builderCustomizers) {
		UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory();
		factory.getDeploymentInfoCustomizers().addAll(deploymentInfoCustomizers.orderedStream().toList());
		factory.getBuilderCustomizers().addAll(builderCustomizers.orderedStream().toList());
		return factory;
	}

	@Bean
	UndertowServletWebServerFactoryCustomizer undertowServletWebServerFactoryCustomizer(
			ServerProperties serverProperties) {
		return new UndertowServletWebServerFactoryCustomizer(serverProperties);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Bootstrap.class)
	static class UndertowWebSocketConfiguration {

		@Bean
		@ConditionalOnMissingBean(name = "websocketServletWebServerCustomizer")
		WebSocketUndertowServletWebServerFactoryCustomizer websocketServletWebServerCustomizer() {
			return new WebSocketUndertowServletWebServerFactoryCustomizer();
		}

	}

}
