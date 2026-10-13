/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client.autoconfigure.imperative;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.JdkClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.JettyClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ReactorClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.SimpleClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.autoconfigure.imperative.ImperativeHttpClientsProperties.Factory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ImperativeHttpClientsProperties}.
 *
 * @author Phillip Webb
 */
class ImperativeHttpClientsPropertiesTests {

	@Nested
	class FactoryTests {

		@Test
		void httpComponentsBuilder() {
			assertThat(Factory.HTTP_COMPONENTS.builder())
				.isInstanceOf(HttpComponentsClientHttpRequestFactoryBuilder.class);
		}

		@Test
		void jettyBuilder() {
			assertThat(Factory.JETTY.builder()).isInstanceOf(JettyClientHttpRequestFactoryBuilder.class);
		}

		@Test
		void reactorBuilder() {
			assertThat(Factory.REACTOR.builder()).isInstanceOf(ReactorClientHttpRequestFactoryBuilder.class);
		}

		@Test
		void jdkBuilder() {
			assertThat(Factory.JDK.builder()).isInstanceOf(JdkClientHttpRequestFactoryBuilder.class);
		}

		@Test
		void simpleBuilder() {
			assertThat(Factory.SIMPLE.builder()).isInstanceOf(SimpleClientHttpRequestFactoryBuilder.class);
		}

	}

}
