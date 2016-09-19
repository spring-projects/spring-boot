/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.template;

import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link TemplateAvailabilityProviders}.
 *
 * @author Phillip Webb
 */
public class TemplateAvailabilityProvidersTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private TemplateAvailabilityProviders providers;

	@Mock
	private TemplateAvailabilityProvider provider;

	private String view = "view";

	private ClassLoader classLoader = getClass().getClassLoader();

	private MockEnvironment environment = new MockEnvironment();

	@Mock
	private ResourceLoader resourceLoader;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.providers = new TemplateAvailabilityProviders(
				Collections.singleton(this.provider));
	}

	@Test
	public void createWhenApplicationContextIsNullShouldThrowException()
			throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ClassLoader must not be null");
		new TemplateAvailabilityProviders((ApplicationContext) null);
	}

	@Test
	public void createWhenUsingApplicationContextShouldLoadProviders() throws Exception {
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		given(applicationContext.getClassLoader()).willReturn(this.classLoader);
		TemplateAvailabilityProviders providers = new TemplateAvailabilityProviders(
				applicationContext);
		assertThat(providers.getProviders()).isNotEmpty();
		verify(applicationContext).getClassLoader();
	}

	@Test
	public void createWhenClassLoaderIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ClassLoader must not be null");
		new TemplateAvailabilityProviders((ClassLoader) null);
	}

	@Test
	public void createWhenUsingClassLoaderShouldLoadProviders() throws Exception {
		TemplateAvailabilityProviders providers = new TemplateAvailabilityProviders(
				this.classLoader);
		assertThat(providers.getProviders()).isNotEmpty();
	}

	@Test
	public void createWhenProvidersIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Providers must not be null");
		new TemplateAvailabilityProviders(
				(Collection<TemplateAvailabilityProvider>) null);
	}

	@Test
	public void createWhenUsingProvidersShouldUseProviders() throws Exception {
		TemplateAvailabilityProviders providers = new TemplateAvailabilityProviders(
				Collections.singleton(this.provider));
		assertThat(providers.getProviders()).containsOnly(this.provider);
	}

	@Test
	public void getProviderWhenApplicationContextIsNullShouldThrowException()
			throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ApplicationContext must not be null");
		this.providers.getProvider(this.view, null);
	}

	@Test
	public void getProviderWhenViewIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("View must not be null");
		this.providers.getProvider(null, this.environment, this.classLoader,
				this.resourceLoader);
	}

	@Test
	public void getProviderWhenEnvironmentIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Environment must not be null");
		this.providers.getProvider(this.view, null, this.classLoader,
				this.resourceLoader);
	}

	@Test
	public void getProviderWhenClassLoaderIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ClassLoader must not be null");
		this.providers.getProvider(this.view, this.environment, null,
				this.resourceLoader);
	}

	@Test
	public void getProviderWhenResourceLoaderIsNullShouldThrowException()
			throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ResourceLoader must not be null");
		this.providers.getProvider(this.view, this.environment, this.classLoader, null);
	}

	@Test
	public void getProviderWhenNoneMatchShouldReturnNull() throws Exception {
		TemplateAvailabilityProvider found = this.providers.getProvider(this.view,
				this.environment, this.classLoader, this.resourceLoader);
		assertThat(found).isNull();
		verify(this.provider).isTemplateAvailable(this.view, this.environment,
				this.classLoader, this.resourceLoader);
	}

	@Test
	public void getProviderWhenMatchShouldReturnProvider() throws Exception {
		given(this.provider.isTemplateAvailable(this.view, this.environment,
				this.classLoader, this.resourceLoader)).willReturn(true);
		TemplateAvailabilityProvider found = this.providers.getProvider(this.view,
				this.environment, this.classLoader, this.resourceLoader);
		assertThat(found).isSameAs(this.provider);

	}

	@Test
	public void getProviderShouldCacheMatchResult() throws Exception {
		given(this.provider.isTemplateAvailable(this.view, this.environment,
				this.classLoader, this.resourceLoader)).willReturn(true);
		this.providers.getProvider(this.view, this.environment, this.classLoader,
				this.resourceLoader);
		this.providers.getProvider(this.view, this.environment, this.classLoader,
				this.resourceLoader);
		verify(this.provider, times(1)).isTemplateAvailable(this.view, this.environment,
				this.classLoader, this.resourceLoader);
	}

	@Test
	public void getProviderShouldCacheNoMatchResult() throws Exception {
		this.providers.getProvider(this.view, this.environment, this.classLoader,
				this.resourceLoader);
		this.providers.getProvider(this.view, this.environment, this.classLoader,
				this.resourceLoader);
		verify(this.provider, times(1)).isTemplateAvailable(this.view, this.environment,
				this.classLoader, this.resourceLoader);
	}

	@Test
	public void getProviderWhenCacheDisabledShouldNotUseCache() throws Exception {
		given(this.provider.isTemplateAvailable(this.view, this.environment,
				this.classLoader, this.resourceLoader)).willReturn(true);
		this.environment.setProperty("spring.template.provider.cache", "false");
		this.providers.getProvider(this.view, this.environment, this.classLoader,
				this.resourceLoader);
		this.providers.getProvider(this.view, this.environment, this.classLoader,
				this.resourceLoader);
		verify(this.provider, times(2)).isTemplateAvailable(this.view, this.environment,
				this.classLoader, this.resourceLoader);
	}

}
