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

package org.springframework.boot.autoconfigure.web.server.reactive.tomcat;

import org.springframework.boot.autoconfigure.web.server.tomcat.TomcatServerProperties;
import org.springframework.boot.tomcat.reactive.TomcatReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

/**
 * {@link WebServerFactoryCustomizer} to apply {@link TomcatServerProperties} to Tomcat
 * reactive web servers.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class TomcatReactiveWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<TomcatReactiveWebServerFactory> {

	private final TomcatServerProperties tomcatProperties;

	public TomcatReactiveWebServerFactoryCustomizer(TomcatServerProperties tomcatProperties) {
		this.tomcatProperties = tomcatProperties;
	}

	@Override
	public void customize(TomcatReactiveWebServerFactory factory) {
		factory.setDisableMBeanRegistry(!this.tomcatProperties.getMbeanregistry().isEnabled());
	}

}
