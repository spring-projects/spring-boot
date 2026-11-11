/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.autoconfigure;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.r2dbc.ConnectionFactoryBuilder;
import org.springframework.boot.r2dbc.ConnectionFactoryDecorator;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link R2dbcProxyAutoConfiguration}.
 *
 * @author Tadaya Tsuyukubo
 * @author Moritz Halbritter
 */
class R2dbcProxyAutoConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(R2dbcProxyAutoConfiguration.class));

	@Test
	void shouldSupplyConnectionFactoryDecorator() {
		this.runner.run((context) -> assertThat(context).hasSingleBean(ConnectionFactoryDecorator.class));
	}

	@Test
	void shouldNotSupplyBeansIfR2dbcSpiIsNotOnClasspath() {
		this.runner.withClassLoader(new FilteredClassLoader("io.r2dbc.spi"))
			.run((context) -> assertThat(context).doesNotHaveBean(ConnectionFactoryDecorator.class));
	}

	@Test
	void shouldNotSupplyBeansIfR2dbcProxyIsNotOnClasspath() {
		this.runner.withClassLoader(new FilteredClassLoader("io.r2dbc.proxy"))
			.run((context) -> assertThat(context).doesNotHaveBean(ConnectionFactoryDecorator.class));
	}

	@Test
	void shouldApplyCustomizers() {
		this.runner.withUserConfiguration(ProxyConnectionFactoryCustomizerConfig.class).run((context) -> {
			ConnectionFactoryDecorator decorator = context.getBean(ConnectionFactoryDecorator.class);
			ConnectionFactory connectionFactory = ConnectionFactoryBuilder
				.withUrl("r2dbc:h2:mem:///" + UUID.randomUUID())
				.build();
			decorator.decorate(connectionFactory);
			assertThat(context.getBean(ProxyConnectionFactoryCustomizerConfig.class).called).containsExactly("first",
					"second");
		});
	}

	@Configuration(proxyBeanMethods = false)
	private static final class ProxyConnectionFactoryCustomizerConfig {

		private final List<String> called = new ArrayList<>();

		@Bean
		@Order(1)
		ProxyConnectionFactoryCustomizer first() {
			return (builder) -> this.called.add("first");
		}

		@Bean
		@Order(2)
		ProxyConnectionFactoryCustomizer second() {
			return (builder) -> this.called.add("second");
		}

	}

}
