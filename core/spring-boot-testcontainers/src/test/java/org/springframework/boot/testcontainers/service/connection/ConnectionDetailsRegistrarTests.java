/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.service.connection;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactories;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactoryNotFoundException;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsNotFoundException;
import org.springframework.boot.origin.Origin;
import org.springframework.core.annotation.MergedAnnotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConnectionDetailsRegistrar}.
 *
 * @author Phillip Webb
 */
class ConnectionDetailsRegistrarTests {

	private Origin origin;

	private PostgreSQLContainer container;

	private MergedAnnotation<ServiceConnection> annotation;

	private ContainerConnectionSource<?> source;

	private ConnectionDetailsFactories factories;

	@BeforeEach
	void setup() {
		this.origin = mock(Origin.class);
		this.container = mock(PostgreSQLContainer.class);
		this.annotation = MergedAnnotation.of(ServiceConnection.class, Map.of("name", "", "type", new Class<?>[0]));
		this.source = new ContainerConnectionSource<>("test", this.origin, PostgreSQLContainer.class, null,
				this.annotation, () -> this.container, null, null);
		this.factories = mock(ConnectionDetailsFactories.class);
	}

	@Test
	void registerBeanDefinitionsWhenConnectionDetailsFactoryNotFoundAndNoConnectionNameThrowsExceptionWithBetterMessage() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		ConnectionDetailsRegistrar registrar = new ConnectionDetailsRegistrar(beanFactory, this.factories);
		given(this.factories.getConnectionDetails(this.source, true))
			.willThrow(new ConnectionDetailsFactoryNotFoundException("fail"));
		assertThatExceptionOfType(ConnectionDetailsFactoryNotFoundException.class)
			.isThrownBy(() -> registrar.registerBeanDefinitions(beanFactory, this.source))
			.withMessage("fail. You may need to add a 'name' to your @ServiceConnection annotation");
	}

	@Test
	void registerBeanDefinitionsWhenConnectionDetailsNotFoundExceptionAndNoConnectionNameThrowsExceptionWithBetterMessage() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		ConnectionDetailsRegistrar registrar = new ConnectionDetailsRegistrar(beanFactory, this.factories);
		given(this.factories.getConnectionDetails(this.source, true))
			.willThrow(new ConnectionDetailsNotFoundException("fail"));
		assertThatExceptionOfType(ConnectionDetailsNotFoundException.class)
			.isThrownBy(() -> registrar.registerBeanDefinitions(beanFactory, this.source))
			.withMessage("fail. You may need to add a 'name' to your @ServiceConnection annotation");
	}

	@Test
	void registerBeanDefinitionsWhenExistingBeanSkipsRegistration() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("testbean", new RootBeanDefinition(CustomTestConnectionDetails.class));
		ConnectionDetailsRegistrar registrar = new ConnectionDetailsRegistrar(beanFactory, this.factories);
		given(this.factories.getConnectionDetails(this.source, true))
			.willReturn(Map.of(TestConnectionDetails.class, new TestConnectionDetails()));
		registrar.registerBeanDefinitions(beanFactory, this.source);
		assertThat(beanFactory.getBean(TestConnectionDetails.class)).isInstanceOf(CustomTestConnectionDetails.class);
	}

	@Test
	void registerBeanDefinitionsRegistersDefinition() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		ConnectionDetailsRegistrar registrar = new ConnectionDetailsRegistrar(beanFactory, this.factories);
		given(this.factories.getConnectionDetails(this.source, true))
			.willReturn(Map.of(TestConnectionDetails.class, new TestConnectionDetails()));
		registrar.registerBeanDefinitions(beanFactory, this.source);
		assertThat(beanFactory.getBean(TestConnectionDetails.class)).isNotNull();
	}

	static class TestConnectionDetails implements ConnectionDetails {

	}

	static class CustomTestConnectionDetails extends TestConnectionDetails {

	}

}
