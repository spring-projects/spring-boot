/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.testcontainers.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.testcontainers.lifecycle.BeforeTestcontainerUsedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link TestcontainersPropertySource}.
 *
 * @author Phillip Webb
 * @deprecated since 3.4.0 for removal in 3.6.0
 */
@SuppressWarnings("removal")
@Deprecated(since = "3.4.0", forRemoval = true)
class TestcontainersPropertySourceTests {

	private MockEnvironment environment = new MockEnvironment()
		.withProperty("spring.testcontainers.dynamic-property-registry-injection", "allow");

	private GenericApplicationContext context = new GenericApplicationContext();

	TestcontainersPropertySourceTests() {
		((DefaultListableBeanFactory) this.context.getBeanFactory()).setAllowBeanDefinitionOverriding(false);
		this.context.setEnvironment(this.environment);
	}

	@Test
	void getPropertyWhenHasValueSupplierReturnsSuppliedValue() {
		DynamicPropertyRegistry registry = TestcontainersPropertySource.attach(this.environment);
		registry.add("test", () -> "spring");
		assertThat(this.environment.getProperty("test")).isEqualTo("spring");
	}

	@Test
	void getPropertyWhenHasNoValueSupplierReturnsNull() {
		DynamicPropertyRegistry registry = TestcontainersPropertySource.attach(this.environment);
		registry.add("test", () -> "spring");
		assertThat(this.environment.getProperty("missing")).isNull();
	}

	@Test
	void containsPropertyWhenHasPropertyReturnsTrue() {
		DynamicPropertyRegistry registry = TestcontainersPropertySource.attach(this.environment);
		registry.add("test", () -> null);
		assertThat(this.environment.containsProperty("test")).isTrue();
	}

	@Test
	void containsPropertyWhenHasNoPropertyReturnsFalse() {
		DynamicPropertyRegistry registry = TestcontainersPropertySource.attach(this.environment);
		registry.add("test", () -> null);
		assertThat(this.environment.containsProperty("missing")).isFalse();
	}

	@Test
	void getPropertyNamesReturnsNames() {
		DynamicPropertyRegistry registry = TestcontainersPropertySource.attach(this.environment);
		registry.add("test", () -> null);
		registry.add("other", () -> null);
		EnumerablePropertySource<?> propertySource = (EnumerablePropertySource<?>) this.environment.getPropertySources()
			.get(TestcontainersPropertySource.NAME);
		assertThat(propertySource.getPropertyNames()).containsExactly("test", "other");
	}

	@Test
	@SuppressWarnings("unchecked")
	void getSourceReturnsImmutableSource() {
		TestcontainersPropertySource.attach(this.environment);
		PropertySource<?> propertySource = this.environment.getPropertySources().get(TestcontainersPropertySource.NAME);
		Map<String, Object> map = (Map<String, Object>) propertySource.getSource();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(map::clear);
	}

	@Test
	void attachToEnvironmentWhenNotAttachedAttaches() {
		TestcontainersPropertySource.attach(this.environment);
		PropertySource<?> propertySource = this.environment.getPropertySources().get(TestcontainersPropertySource.NAME);
		assertThat(propertySource).isNotNull();
	}

	@Test
	void attachToEnvironmentWhenAlreadyAttachedReturnsExisting() {
		DynamicPropertyRegistry r1 = TestcontainersPropertySource.attach(this.environment);
		PropertySource<?> p1 = this.environment.getPropertySources().get(TestcontainersPropertySource.NAME);
		DynamicPropertyRegistry r2 = TestcontainersPropertySource.attach(this.environment);
		PropertySource<?> p2 = this.environment.getPropertySources().get(TestcontainersPropertySource.NAME);
		assertThat(r1).isSameAs(r2);
		assertThat(p1).isSameAs(p2);
	}

	@Test
	void attachToEnvironmentAndContextWhenNotAttachedAttaches() {
		TestcontainersPropertySource.attach(this.environment, this.context);
		PropertySource<?> propertySource = this.environment.getPropertySources().get(TestcontainersPropertySource.NAME);
		assertThat(propertySource).isNotNull();
		assertThat(this.context.containsBean(
				org.springframework.boot.testcontainers.properties.TestcontainersPropertySource.EventPublisherRegistrar.NAME));
	}

	@Test
	void attachToEnvironmentAndContextWhenAlreadyAttachedReturnsExisting() {
		DynamicPropertyRegistry r1 = TestcontainersPropertySource.attach(this.environment, this.context);
		PropertySource<?> p1 = this.environment.getPropertySources().get(TestcontainersPropertySource.NAME);
		DynamicPropertyRegistry r2 = TestcontainersPropertySource.attach(this.environment, this.context);
		PropertySource<?> p2 = this.environment.getPropertySources().get(TestcontainersPropertySource.NAME);
		assertThat(r1).isSameAs(r2);
		assertThat(p1).isSameAs(p2);
	}

	@Test
	void getPropertyPublishesEvent() {
		try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {
			ConfigurableEnvironment environment = applicationContext.getEnvironment();
			environment.getPropertySources()
				.addLast(new MapPropertySource("test",
						Map.of("spring.testcontainers.dynamic-property-registry-injection", "allow")));
			List<ApplicationEvent> events = new ArrayList<>();
			applicationContext.addApplicationListener(events::add);
			DynamicPropertyRegistry registry = TestcontainersPropertySource.attach(environment,
					(BeanDefinitionRegistry) applicationContext.getBeanFactory());
			applicationContext.refresh();
			registry.add("test", () -> "spring");
			assertThat(environment.containsProperty("test")).isTrue();
			assertThat(events.isEmpty());
			assertThat(environment.getProperty("test")).isEqualTo("spring");
			assertThat(events.stream().filter(BeforeTestcontainerUsedEvent.class::isInstance)).hasSize(1);
		}
	}

}
