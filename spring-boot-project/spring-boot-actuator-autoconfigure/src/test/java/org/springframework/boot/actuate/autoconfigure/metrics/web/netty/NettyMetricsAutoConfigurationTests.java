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

package org.springframework.boot.actuate.autoconfigure.metrics.web.netty;

import java.util.stream.Collectors;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NettyMetricsAutoConfiguration}
 *
 * @author Brian Clozel
 */
class NettyMetricsAutoConfigurationTests {

	@Test
	void autoConfiguresTcpMetricsWithReactorNettyServer() {
		MeterRegistry registry = Metrics.globalRegistry;
		assertThat(registry.find("reactor.netty.tcp.server.data.received").summary()).isNull();
		new ReactiveWebApplicationContextRunner(AnnotationConfigReactiveWebServerApplicationContext::new)
				.withConfiguration(AutoConfigurations.of(NettyMetricsAutoConfiguration.class,
						ReactiveWebServerFactoryAutoConfiguration.class))
				.withUserConfiguration(ReactiveWebServerConfiguration.class).run((context) -> {
					AnnotationConfigReactiveWebServerApplicationContext serverContext = context
							.getSourceApplicationContext(AnnotationConfigReactiveWebServerApplicationContext.class);
					context.publishEvent(new ApplicationStartedEvent(new SpringApplication(), null,
							context.getSourceApplicationContext()));
					WebClient.create("http://localhost:" + serverContext.getWebServer().getPort()).get().retrieve()
							.toBodilessEntity().block();
					assertThat(registry.find("reactor.netty.tcp.server.data.received").summary()).isNotNull();
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class ReactiveWebServerConfiguration {

		@Bean
		NettyReactiveWebServerFactory nettyFactory(ObjectProvider<NettyServerCustomizer> customizers) {
			NettyReactiveWebServerFactory serverFactory = new NettyReactiveWebServerFactory(0);
			serverFactory.setServerCustomizers(customizers.orderedStream().collect(Collectors.toList()));
			return serverFactory;
		}

		@Bean
		HttpHandler httpHandler() {
			return (req, res) -> res.setComplete();
		}

	}

}
