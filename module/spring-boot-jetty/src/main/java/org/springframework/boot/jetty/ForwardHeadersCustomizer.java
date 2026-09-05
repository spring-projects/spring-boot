/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jetty;

import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;

/**
 * {@link JettyServerCustomizer} to add {@link ForwardedRequestCustomizer}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class ForwardHeadersCustomizer implements JettyServerCustomizer {

	private final boolean useXForwarded;

	/**
	 * Create a new customizer instance.
	 * @param useXForwarded "X-Forwarded-*" headers are used if {@code true}, otherwise
	 * standard "Forwarded" are used instead.
	 * @since 4.2.0
	 */
	public ForwardHeadersCustomizer(boolean useXForwarded) {
		this.useXForwarded = useXForwarded;
	}

	/**
	 * Create a new customizer instance with "X-Forwarded-*" support.
	 */
	public ForwardHeadersCustomizer() {
		this(true);
	}

	@Override
	public void customize(Server server) {
		ForwardedRequestCustomizer customizer = new ForwardedRequestCustomizer();
		if (this.useXForwarded) {
			// disable "Forwarded" support
			customizer.setForwardedHeader(null);
		}
		else {
			// disable "X-Forwarded-*" support
			customizer.setForwardedOnly(true);
		}
		for (Connector connector : server.getConnectors()) {
			for (ConnectionFactory connectionFactory : connector.getConnectionFactories()) {
				if (connectionFactory instanceof HttpConfiguration.ConnectionFactory jettyConnectionFactory) {
					jettyConnectionFactory.getHttpConfiguration().addCustomizer(customizer);
				}
			}
		}
	}

}
