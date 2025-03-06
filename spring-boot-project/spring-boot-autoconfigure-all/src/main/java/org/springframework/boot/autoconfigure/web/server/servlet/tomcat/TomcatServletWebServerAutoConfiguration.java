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

package org.springframework.boot.autoconfigure.web.server.servlet.tomcat;

import jakarta.servlet.ServletRequest;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.UpgradeProtocol;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.server.servlet.ForwardedHeaderFilterCustomizer;
import org.springframework.boot.autoconfigure.web.server.servlet.ServletWebServerConfiguration;
import org.springframework.boot.autoconfigure.web.server.tomcat.TomcatServerProperties;
import org.springframework.boot.autoconfigure.web.server.tomcat.TomcatWebServerConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.tomcat.TomcatContextCustomizer;
import org.springframework.boot.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a Tomcat-based servlet web
 * server.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ ServletRequest.class, Tomcat.class, UpgradeProtocol.class, TomcatServletWebServerFactory.class })
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(TomcatServerProperties.class)
@Import({ ServletWebServerConfiguration.class, TomcatWebServerConfiguration.class })
public class TomcatServletWebServerAutoConfiguration {

	private final TomcatServerProperties tomcatProperties;

	public TomcatServletWebServerAutoConfiguration(TomcatServerProperties tomcatProperties) {
		this.tomcatProperties = tomcatProperties;
	}

	@Bean
	@ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT)
	TomcatServletWebServerFactory tomcatServletWebServerFactory(
			ObjectProvider<TomcatConnectorCustomizer> connectorCustomizers,
			ObjectProvider<TomcatContextCustomizer> contextCustomizers,
			ObjectProvider<TomcatProtocolHandlerCustomizer<?>> protocolHandlerCustomizers) {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
		factory.getConnectorCustomizers().addAll(connectorCustomizers.orderedStream().toList());
		factory.getContextCustomizers().addAll(contextCustomizers.orderedStream().toList());
		factory.getProtocolHandlerCustomizers().addAll(protocolHandlerCustomizers.orderedStream().toList());
		return factory;
	}

	@Bean
	TomcatServletWebServerFactoryCustomizer tomcatServletWebServerFactoryCustomizer(
			TomcatServerProperties tomcatProperties) {
		return new TomcatServletWebServerFactoryCustomizer(tomcatProperties);
	}

	@Bean
	@ConditionalOnProperty(name = "server.forward-headers-strategy", havingValue = "framework")
	ForwardedHeaderFilterCustomizer tomcatForwardedHeaderFilterCustomizer(ServerProperties serverProperties) {
		return (filter) -> filter.setRelativeRedirects(this.tomcatProperties.isUseRelativeRedirects());
	}

}
