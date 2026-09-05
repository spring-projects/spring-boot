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

import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ForwardHeadersCustomizer}.
 *
 * @author Brian Clozel
 */
class ForwardHeadersCustomizerTests {

	@Test
	void defaultConstructorUsesXForwardedHeaders() {
		ForwardedRequestCustomizer customizer = customize(new ForwardHeadersCustomizer());
		assertThat(customizer.getForwardedHeader()).isNull();
		assertThat(customizer.getForwardedForHeader()).isNotNull();
	}

	@Test
	void useXForwardedTrueEnablesXForwardedAndDisablesForwarded() {
		ForwardedRequestCustomizer customizer = customize(new ForwardHeadersCustomizer(true));
		assertThat(customizer.getForwardedHeader()).isNull();
		assertThat(customizer.getForwardedForHeader()).isNotNull();
	}

	@Test
	void useXForwardedFalseEnablesForwardedAndDisablesXForwarded() {
		ForwardedRequestCustomizer customizer = customize(new ForwardHeadersCustomizer(false));
		assertThat(customizer.getForwardedHeader()).isNotNull();
		assertThat(customizer.getForwardedForHeader()).isNull();
	}

	private ForwardedRequestCustomizer customize(ForwardHeadersCustomizer customizer) {
		Server server = new Server(0);
		customizer.customize(server);
		HttpConfiguration httpConfiguration = ((HttpConfiguration.ConnectionFactory) server.getConnectors()[0]
			.getConnectionFactories()
			.stream()
			.filter(HttpConfiguration.ConnectionFactory.class::isInstance)
			.findFirst()
			.orElseThrow()).getHttpConfiguration();
		return httpConfiguration.getCustomizer(ForwardedRequestCustomizer.class);
	}

}
