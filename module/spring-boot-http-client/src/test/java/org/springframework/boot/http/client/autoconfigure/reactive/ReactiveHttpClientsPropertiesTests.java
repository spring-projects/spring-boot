/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client.autoconfigure.reactive;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.http.client.autoconfigure.reactive.ReactiveHttpClientsProperties.Connector;
import org.springframework.boot.http.client.reactive.HttpComponentsClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.JdkClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.JettyClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.ReactorClientHttpConnectorBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveHttpClientsProperties}.
 *
 * @author Phillip Webb
 */
class ReactiveHttpClientsPropertiesTests {

	@Nested
	class ConnectorTests {

		@Test
		void reactorBuilder() {
			assertThat(Connector.REACTOR.builder()).isInstanceOf(ReactorClientHttpConnectorBuilder.class);
		}

		@Test
		void jettyBuilder() {
			assertThat(Connector.JETTY.builder()).isInstanceOf(JettyClientHttpConnectorBuilder.class);
		}

		@Test
		void httpComponentsBuilder() {
			assertThat(Connector.HTTP_COMPONENTS.builder())
				.isInstanceOf(HttpComponentsClientHttpConnectorBuilder.class);
		}

		@Test
		void jdkBuilder() {
			assertThat(Connector.JDK.builder()).isInstanceOf(JdkClientHttpConnectorBuilder.class);
		}

	}

}
