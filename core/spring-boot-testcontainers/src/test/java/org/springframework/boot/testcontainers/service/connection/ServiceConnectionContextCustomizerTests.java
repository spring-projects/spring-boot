/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.service.connection;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactories;
import org.springframework.boot.origin.Origin;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link ServiceConnectionContextCustomizer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ServiceConnectionContextCustomizerTests {

	private Origin origin;

	private PostgreSQLContainer container;

	private MergedAnnotation<ServiceConnection> annotation;

	private ContainerConnectionSource<?> source;

	private ConnectionDetailsFactories factories;

	@BeforeEach
	void setup() {
		this.origin = mock(Origin.class);
		this.container = mock(PostgreSQLContainer.class);
		this.annotation = MergedAnnotation.of(ServiceConnection.class,
				Map.of("name", "myname", "type", new Class<?>[0]));
		this.source = new ContainerConnectionSource<>("test", this.origin, PostgreSQLContainer.class,
				this.container.getDockerImageName(), this.annotation, () -> this.container, null, null);
		this.factories = mock(ConnectionDetailsFactories.class);
	}

	@Test
	void customizeContextRegistersServiceConnections() {
		ServiceConnectionContextCustomizer customizer = new ServiceConnectionContextCustomizer(List.of(this.source),
				this.factories);
		ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
		DefaultListableBeanFactory beanFactory = spy(new DefaultListableBeanFactory());
		given(context.getBeanFactory()).willReturn(beanFactory);
		MergedContextConfiguration mergedConfig = mock(MergedContextConfiguration.class);
		DatabaseConnectionDetails connectionDetails = new TestConnectionDetails();
		given(this.factories.getConnectionDetails(this.source, true))
			.willReturn(Map.of(DatabaseConnectionDetails.class, connectionDetails));
		customizer.customizeContext(context, mergedConfig);
		then(beanFactory).should()
			.registerBeanDefinition(eq("testConnectionDetailsForTest"),
					ArgumentMatchers.<RootBeanDefinition>assertArg((beanDefinition) -> {
						Supplier<?> instanceSupplier = beanDefinition.getInstanceSupplier();
						assertThat(instanceSupplier).isNotNull();
						assertThat(instanceSupplier.get()).isSameAs(connectionDetails);
						assertThat(beanDefinition.getBeanClass()).isEqualTo(TestConnectionDetails.class);
					}));
	}

	@Test
	void equalsAndHashCode() {
		PostgreSQLContainer container1 = mock(PostgreSQLContainer.class);
		PostgreSQLContainer container2 = mock(PostgreSQLContainer.class);
		MergedAnnotation<ServiceConnection> annotation1 = MergedAnnotation.of(ServiceConnection.class,
				Map.of("name", "", "type", new Class<?>[0]));
		MergedAnnotation<ServiceConnection> annotation2 = MergedAnnotation.of(ServiceConnection.class,
				Map.of("name", "", "type", new Class<?>[0]));
		MergedAnnotation<ServiceConnection> annotation3 = MergedAnnotation.of(ServiceConnection.class,
				Map.of("name", "", "type", new Class<?>[] { DatabaseConnectionDetails.class }));
		// Connection Names
		ServiceConnectionContextCustomizer n1 = new ServiceConnectionContextCustomizer(
				List.of(new ContainerConnectionSource<>("test", this.origin, PostgreSQLContainer.class, "name",
						annotation1, () -> container1, null, null)));
		ServiceConnectionContextCustomizer n2 = new ServiceConnectionContextCustomizer(
				List.of(new ContainerConnectionSource<>("test", this.origin, PostgreSQLContainer.class, "name",
						annotation1, () -> container1, null, null)));
		ServiceConnectionContextCustomizer n3 = new ServiceConnectionContextCustomizer(
				List.of(new ContainerConnectionSource<>("test", this.origin, PostgreSQLContainer.class, "namex",
						annotation1, () -> container1, null, null)));
		assertThat(n1.hashCode()).isEqualTo(n2.hashCode()).isNotEqualTo(n3.hashCode());
		assertThat(n1).isEqualTo(n2).isNotEqualTo(n3);
		// Connection Details Types
		ServiceConnectionContextCustomizer t1 = new ServiceConnectionContextCustomizer(
				List.of(new ContainerConnectionSource<>("test", this.origin, PostgreSQLContainer.class, "name",
						annotation1, () -> container1, null, null)));
		ServiceConnectionContextCustomizer t2 = new ServiceConnectionContextCustomizer(
				List.of(new ContainerConnectionSource<>("test", this.origin, PostgreSQLContainer.class, "name",
						annotation2, () -> container1, null, null)));
		ServiceConnectionContextCustomizer t3 = new ServiceConnectionContextCustomizer(
				List.of(new ContainerConnectionSource<>("test", this.origin, PostgreSQLContainer.class, "name",
						annotation3, () -> container1, null, null)));
		assertThat(t1.hashCode()).isEqualTo(t2.hashCode()).isNotEqualTo(t3.hashCode());
		assertThat(t1).isEqualTo(t2).isNotEqualTo(t3);
		// Container
		ServiceConnectionContextCustomizer c1 = new ServiceConnectionContextCustomizer(
				List.of(new ContainerConnectionSource<>("test", this.origin, PostgreSQLContainer.class, "name",
						annotation1, () -> container1, null, null)));
		ServiceConnectionContextCustomizer c2 = new ServiceConnectionContextCustomizer(
				List.of(new ContainerConnectionSource<>("test", this.origin, PostgreSQLContainer.class, "name",
						annotation1, () -> container1, null, null)));
		ServiceConnectionContextCustomizer c3 = new ServiceConnectionContextCustomizer(
				List.of(new ContainerConnectionSource<>("test", this.origin, PostgreSQLContainer.class, "name",
						annotation1, () -> container2, null, null)));
		assertThat(c1.hashCode()).isEqualTo(c2.hashCode()).isNotEqualTo(c3.hashCode());
		assertThat(c1).isEqualTo(c2).isNotEqualTo(c3);
	}

	static class TestConnectionDetails implements DatabaseConnectionDetails {

		@Override
		public String getJdbcUrl() {
			return "";
		}

	}

}
