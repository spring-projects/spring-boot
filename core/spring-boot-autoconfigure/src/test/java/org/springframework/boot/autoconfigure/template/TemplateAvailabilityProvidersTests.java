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

package org.springframework.boot.autoconfigure.template;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link TemplateAvailabilityProviders}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class TemplateAvailabilityProvidersTests {

	private TemplateAvailabilityProviders providers;

	@Mock
	@SuppressWarnings("NullAway.Init")
	private TemplateAvailabilityProvider provider;

	private final String view = "view";

	private final ClassLoader classLoader = getClass().getClassLoader();

	private final MockEnvironment environment = new MockEnvironment();

	@Mock
	@SuppressWarnings("NullAway.Init")
	private ResourceLoader resourceLoader;

	@BeforeEach
	void setup() {
		this.providers = new TemplateAvailabilityProviders(Collections.singleton(this.provider));
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenApplicationContextIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new TemplateAvailabilityProviders((ApplicationContext) null))
			.withMessageContaining("'applicationContext' must not be null");
	}

	@Test
	@SuppressWarnings("rawtypes")
	@WithTestTemplateAvailabilityProvider
	void createWhenUsingApplicationContextShouldLoadProviders() {
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		given(applicationContext.getClassLoader()).willReturn(Thread.currentThread().getContextClassLoader());
		TemplateAvailabilityProviders providers = new TemplateAvailabilityProviders(applicationContext);
		assertThat(providers.getProviders()).extracting((provider) -> (Class) provider.getClass())
			.containsExactly(TestTemplateAvailabilityProvider.class);
		then(applicationContext).should().getClassLoader();
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenClassLoaderIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TemplateAvailabilityProviders((ClassLoader) null))
			.withMessageContaining("'classLoader' must not be null");
	}

	@Test
	@WithTestTemplateAvailabilityProvider
	void createWhenUsingClassLoaderShouldLoadProviders() {
		TemplateAvailabilityProviders providers = new TemplateAvailabilityProviders(
				Thread.currentThread().getContextClassLoader());
		assertThat(providers.getProviders()).isNotEmpty();
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenProvidersIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new TemplateAvailabilityProviders((Collection<TemplateAvailabilityProvider>) null))
			.withMessageContaining("'providers' must not be null");
	}

	@Test
	void createWhenUsingProvidersShouldUseProviders() {
		TemplateAvailabilityProviders providers = new TemplateAvailabilityProviders(
				Collections.singleton(this.provider));
		assertThat(providers.getProviders()).containsOnly(this.provider);
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void getProviderWhenApplicationContextIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.providers.getProvider(this.view, null))
			.withMessageContaining("'applicationContext' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void getProviderWhenViewIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.providers.getProvider(null, this.environment, this.classLoader, this.resourceLoader))
			.withMessageContaining("'view' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void getProviderWhenEnvironmentIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.providers.getProvider(this.view, null, this.classLoader, this.resourceLoader))
			.withMessageContaining("'environment' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void getProviderWhenClassLoaderIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.providers.getProvider(this.view, this.environment, null, this.resourceLoader))
			.withMessageContaining("'classLoader' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void getProviderWhenResourceLoaderIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.providers.getProvider(this.view, this.environment, this.classLoader, null))
			.withMessageContaining("'resourceLoader' must not be null");
	}

	@Test
	void getProviderWhenNoneMatchShouldReturnNull() {
		TemplateAvailabilityProvider found = this.providers.getProvider(this.view, this.environment, this.classLoader,
				this.resourceLoader);
		assertThat(found).isNull();
		then(this.provider).should()
			.isTemplateAvailable(this.view, this.environment, this.classLoader, this.resourceLoader);
	}

	@Test
	void getProviderWhenMatchShouldReturnProvider() {
		given(this.provider.isTemplateAvailable(this.view, this.environment, this.classLoader, this.resourceLoader))
			.willReturn(true);
		TemplateAvailabilityProvider found = this.providers.getProvider(this.view, this.environment, this.classLoader,
				this.resourceLoader);
		assertThat(found).isSameAs(this.provider);

	}

	@Test
	void getProviderShouldCacheMatchResult() {
		given(this.provider.isTemplateAvailable(this.view, this.environment, this.classLoader, this.resourceLoader))
			.willReturn(true);
		this.providers.getProvider(this.view, this.environment, this.classLoader, this.resourceLoader);
		this.providers.getProvider(this.view, this.environment, this.classLoader, this.resourceLoader);
		then(this.provider).should()
			.isTemplateAvailable(this.view, this.environment, this.classLoader, this.resourceLoader);
	}

	@Test
	void getProviderShouldCacheNoMatchResult() {
		this.providers.getProvider(this.view, this.environment, this.classLoader, this.resourceLoader);
		this.providers.getProvider(this.view, this.environment, this.classLoader, this.resourceLoader);
		then(this.provider).should()
			.isTemplateAvailable(this.view, this.environment, this.classLoader, this.resourceLoader);
	}

	@Test
	void getProviderWhenCacheDisabledShouldNotUseCache() {
		given(this.provider.isTemplateAvailable(this.view, this.environment, this.classLoader, this.resourceLoader))
			.willReturn(true);
		this.environment.setProperty("spring.template.provider.cache", "false");
		this.providers.getProvider(this.view, this.environment, this.classLoader, this.resourceLoader);
		this.providers.getProvider(this.view, this.environment, this.classLoader, this.resourceLoader);
		then(this.provider).should(times(2))
			.isTemplateAvailable(this.view, this.environment, this.classLoader, this.resourceLoader);
	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@WithResource(name = "META-INF/spring.factories",
			content = "org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider="
					+ "org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvidersTests$TestTemplateAvailabilityProvider")
	@interface WithTestTemplateAvailabilityProvider {

	}

	static class TestTemplateAvailabilityProvider implements TemplateAvailabilityProvider {

		@Override
		public boolean isTemplateAvailable(String view, Environment environment, ClassLoader classLoader,
				ResourceLoader resourceLoader) {
			return false;
		}

	}

}
