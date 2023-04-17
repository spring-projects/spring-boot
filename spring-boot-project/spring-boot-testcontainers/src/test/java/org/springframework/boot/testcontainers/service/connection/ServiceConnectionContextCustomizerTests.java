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

package org.springframework.boot.testcontainers.service.connection;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactories;
import org.springframework.boot.origin.Origin;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
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

	private String beanNameSuffix;

	private Origin origin;

	private JdbcDatabaseContainer<?> container;

	private MergedAnnotation<ServiceConnection> annotation;

	private ContainerConnectionSource<?, ?> source;

	private ConnectionDetailsFactories factories;

	@BeforeEach
	void setup() {
		this.beanNameSuffix = "MyBean";
		this.origin = mock(Origin.class);
		this.container = mock(PostgreSQLContainer.class);
		this.annotation = MergedAnnotation.of(ServiceConnection.class,
				Map.of("name", "myname", "type", new Class<?>[0]));
		this.source = new ContainerConnectionSource<>(this.beanNameSuffix, this.origin, this.container,
				this.annotation);
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
		JdbcConnectionDetails connectionDetails = new TestJdbcConnectionDetails();
		given(this.factories.getConnectionDetails(this.source))
			.willReturn(Map.of(JdbcConnectionDetails.class, connectionDetails));
		customizer.customizeContext(context, mergedConfig);
		ArgumentCaptor<BeanDefinition> beanDefinitionCaptor = ArgumentCaptor.forClass(BeanDefinition.class);
		then(beanFactory).should()
			.registerBeanDefinition(eq("testJdbcConnectionDetailsForMyBean"), beanDefinitionCaptor.capture());
		RootBeanDefinition beanDefinition = (RootBeanDefinition) beanDefinitionCaptor.getValue();
		assertThat(beanDefinition.getInstanceSupplier().get()).isSameAs(connectionDetails);
		assertThat(beanDefinition.getBeanClass()).isEqualTo(TestJdbcConnectionDetails.class);
	}

	@Test
	void customizeContextWhenFactoriesHasNoConnectionDetailsThrowsException() {
		ServiceConnectionContextCustomizer customizer = new ServiceConnectionContextCustomizer(List.of(this.source),
				this.factories);
		ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
		DefaultListableBeanFactory beanFactory = spy(new DefaultListableBeanFactory());
		given(context.getBeanFactory()).willReturn(beanFactory);
		MergedContextConfiguration mergedConfig = mock(MergedContextConfiguration.class);
		assertThatIllegalStateException().isThrownBy(() -> customizer.customizeContext(context, mergedConfig))
			.withMessageStartingWith("No connection details created for @ServiceConnection source");
	}

	/**
	 * Test {@link JdbcConnectionDetails}.
	 */
	static class TestJdbcConnectionDetails implements JdbcConnectionDetails {

		@Override
		public String getUsername() {
			return null;
		}

		@Override
		public String getPassword() {
			return null;
		}

		@Override
		public String getJdbcUrl() {
			return null;
		}

	}

}
