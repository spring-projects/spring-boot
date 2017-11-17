/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.reactive;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.reactive.server.ConfigurableReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;

/**
 * Default {@link WebServerFactoryCustomizer} for reactive servers.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class DefaultReactiveWebServerCustomizer implements
		WebServerFactoryCustomizer<ConfigurableReactiveWebServerFactory>, Ordered {

	private final ServerProperties serverProperties;

	public DefaultReactiveWebServerCustomizer(ServerProperties serverProperties) {
		this.serverProperties = serverProperties;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void customize(ConfigurableReactiveWebServerFactory server) {
		if (this.serverProperties.getPort() != null) {
			server.setPort(this.serverProperties.getPort());
		}
		if (this.serverProperties.getAddress() != null) {
			server.setAddress(this.serverProperties.getAddress());
		}
		if (this.serverProperties.getSsl() != null) {
			server.setSsl(this.serverProperties.getSsl());
		}
		if (this.serverProperties.getCompression() != null) {
			server.setCompression(this.serverProperties.getCompression());
		}
		if (this.serverProperties.getHttp2() != null) {
			server.setHttp2(this.serverProperties.getHttp2());
		}
	}

}
