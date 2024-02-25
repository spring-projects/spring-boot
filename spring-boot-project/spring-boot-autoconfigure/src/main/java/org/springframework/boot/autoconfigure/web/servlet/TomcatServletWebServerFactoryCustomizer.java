/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;
import org.springframework.util.ObjectUtils;

/**
 * {@link WebServerFactoryCustomizer} to apply {@link ServerProperties} to Tomcat web
 * servers.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 2.0.0
 */
public class TomcatServletWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<TomcatServletWebServerFactory>, Ordered {

	private final ServerProperties serverProperties;

	/**
	 * Constructs a new TomcatServletWebServerFactoryCustomizer with the specified
	 * ServerProperties.
	 * @param serverProperties the ServerProperties to be used by the customizer
	 */
	public TomcatServletWebServerFactoryCustomizer(ServerProperties serverProperties) {
		this.serverProperties = serverProperties;
	}

	/**
	 * Returns the order value for this customizer.
	 *
	 * The order value determines the order in which the customizer is applied. A lower
	 * value means higher priority.
	 * @return the order value for this customizer
	 */
	@Override
	public int getOrder() {
		return 0;
	}

	/**
	 * Customize the TomcatServletWebServerFactory with the provided configuration
	 * properties.
	 * @param factory the TomcatServletWebServerFactory to customize
	 */
	@Override
	public void customize(TomcatServletWebServerFactory factory) {
		ServerProperties.Tomcat tomcatProperties = this.serverProperties.getTomcat();
		if (!ObjectUtils.isEmpty(tomcatProperties.getAdditionalTldSkipPatterns())) {
			factory.getTldSkipPatterns().addAll(tomcatProperties.getAdditionalTldSkipPatterns());
		}
		if (tomcatProperties.getRedirectContextRoot() != null) {
			customizeRedirectContextRoot(factory, tomcatProperties.getRedirectContextRoot());
		}
		customizeUseRelativeRedirects(factory, tomcatProperties.isUseRelativeRedirects());
		factory.setDisableMBeanRegistry(!tomcatProperties.getMbeanregistry().isEnabled());
	}

	/**
	 * Customizes the redirect context root setting for the Tomcat web server factory.
	 * @param factory the configurable Tomcat web server factory
	 * @param redirectContextRoot true to enable redirecting the context root, false
	 * otherwise
	 */
	private void customizeRedirectContextRoot(ConfigurableTomcatWebServerFactory factory, boolean redirectContextRoot) {
		factory.addContextCustomizers((context) -> context.setMapperContextRootRedirectEnabled(redirectContextRoot));
	}

	/**
	 * Sets the flag to customize the use of relative redirects in the Tomcat web server
	 * factory.
	 * @param factory the ConfigurableTomcatWebServerFactory to customize
	 * @param useRelativeRedirects the flag indicating whether to use relative redirects
	 */
	private void customizeUseRelativeRedirects(ConfigurableTomcatWebServerFactory factory,
			boolean useRelativeRedirects) {
		factory.addContextCustomizers((context) -> context.setUseRelativeRedirects(useRelativeRedirects));
	}

}
