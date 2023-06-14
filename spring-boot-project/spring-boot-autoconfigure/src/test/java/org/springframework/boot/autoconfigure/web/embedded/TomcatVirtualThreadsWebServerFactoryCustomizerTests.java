/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.embedded;

import java.util.function.Consumer;

import org.apache.tomcat.util.threads.VirtualThreadExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TomcatVirtualThreadsWebServerFactoryCustomizer}.
 *
 * @author Moritz Halbritter
 */
class TomcatVirtualThreadsWebServerFactoryCustomizerTests {

	private final TomcatVirtualThreadsWebServerFactoryCustomizer customizer = new TomcatVirtualThreadsWebServerFactoryCustomizer();

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void shouldSetVirtualThreadExecutor() {
		withWebServer((webServer) -> assertThat(webServer.getTomcat().getConnector().getProtocolHandler().getExecutor())
			.isInstanceOf(VirtualThreadExecutor.class));
	}

	private TomcatWebServer getWebServer() {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
		this.customizer.customize(factory);
		return (TomcatWebServer) factory.getWebServer();
	}

	private void withWebServer(Consumer<TomcatWebServer> callback) {
		TomcatWebServer webServer = getWebServer();
		webServer.start();
		try {
			callback.accept(webServer);
		}
		finally {
			webServer.stop();
		}
	}

}
