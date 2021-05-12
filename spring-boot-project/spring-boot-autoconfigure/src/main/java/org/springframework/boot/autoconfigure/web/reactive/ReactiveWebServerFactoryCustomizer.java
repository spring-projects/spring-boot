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

package org.springframework.boot.autoconfigure.web.reactive;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.reactive.server.ConfigurableReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;

/**
 * {@link WebServerFactoryCustomizer} to apply {@link ServerProperties} to reactive
 * servers.
 *
 * @author Brian Clozel
 * @author Yunkun Huang
 * @since 2.0.0
 */
public class ReactiveWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<ConfigurableReactiveWebServerFactory>, Ordered {

	private final ServerProperties serverProperties;

	public ReactiveWebServerFactoryCustomizer(ServerProperties serverProperties) {
		this.serverProperties = serverProperties;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void customize(ConfigurableReactiveWebServerFactory factory) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(this.serverProperties::getPort).to(factory::setPort);
		map.from(this.serverProperties::getAddress).to(factory::setAddress);
		map.from(this.serverProperties::getSsl).to(factory::setSsl);
		map.from(this.serverProperties::getCompression).to(factory::setCompression);
		map.from(this.serverProperties::getHttp2).to(factory::setHttp2);
		map.from(this.serverProperties.getShutdown()).to(factory::setShutdown);
	}

}
