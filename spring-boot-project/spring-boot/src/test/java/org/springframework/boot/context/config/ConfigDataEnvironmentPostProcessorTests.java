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

package org.springframework.boot.context.config;

import java.util.Collections;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.TestApplicationEnvironment;
import org.springframework.boot.context.config.ConfigData.Options;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link ConfigDataEnvironmentPostProcessor}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Nguyen Bao Sach
 */
class ConfigDataEnvironmentPostProcessorTests {

	private final TestApplicationEnvironment environment = new TestApplicationEnvironment();

	private final SpringApplication application = new SpringApplication();

	private ConfigDataEnvironment configDataEnvironment;

	private ConfigDataEnvironmentPostProcessor postProcessor;

	@Test
	void postProcessEnvironmentWhenNoLoaderCreatesDefaultLoaderInstance() {
		setupMocksAndSpies();
		willReturn(this.configDataEnvironment).given(this.postProcessor).getConfigDataEnvironment(any(), any(), any());
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		then(this.postProcessor).should()
			.getConfigDataEnvironment(any(),
					assertArg((resourceLoader) -> assertThat(resourceLoader).isInstanceOf(DefaultResourceLoader.class)),
					any());
		then(this.configDataEnvironment).should().processAndApply();
	}

	@Test
	void postProcessEnvironmentWhenCustomLoaderUsesSpecifiedLoaderInstance() {
		setupMocksAndSpies();
		ResourceLoader resourceLoader = mock(ResourceLoader.class);
		this.application.setResourceLoader(resourceLoader);
		willReturn(this.configDataEnvironment).given(this.postProcessor).getConfigDataEnvironment(any(), any(), any());
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		then(this.postProcessor).should()
			.getConfigDataEnvironment(any(),
					assertArg((resourceLoaderB) -> assertThat(resourceLoaderB).isSameAs(resourceLoader)), any());
		then(this.configDataEnvironment).should().processAndApply();
	}

	@Test
	void postProcessEnvironmentWhenHasAdditionalProfilesOnSpringApplicationUsesAdditionalProfiles() {
		setupMocksAndSpies();
		this.application.setAdditionalProfiles("dev");
		willReturn(this.configDataEnvironment).given(this.postProcessor).getConfigDataEnvironment(any(), any(), any());
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		then(this.postProcessor).should()
			.getConfigDataEnvironment(any(), any(),
					assertArg((additionalProperties) -> assertThat(additionalProperties).containsExactly("dev")));
		then(this.configDataEnvironment).should().processAndApply();
	}

	@Test
	void postProcessEnvironmentWhenNoActiveProfiles() {
		setupMocksAndSpies();
		willReturn(this.configDataEnvironment).given(this.postProcessor).getConfigDataEnvironment(any(), any(), any());
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		then(this.postProcessor).should().getConfigDataEnvironment(any(), any(ResourceLoader.class), any());
		then(this.configDataEnvironment).should().processAndApply();
		assertThat(this.environment.getActiveProfiles()).isEmpty();
	}

	@Test
	@WithResource(name = "application.properties", content = "property=value")
	@WithResource(name = "application-dev.properties", content = "property=dev-value")
	void applyToAppliesPostProcessing() {
		int before = this.environment.getPropertySources().size();
		TestConfigDataEnvironmentUpdateListener listener = new TestConfigDataEnvironmentUpdateListener();
		ConfigDataEnvironmentPostProcessor.applyTo(this.environment, null, null, Collections.singleton("dev"),
				listener);
		assertThat(this.environment.getPropertySources()).hasSizeGreaterThan(before);
		assertThat(this.environment.getActiveProfiles()).containsExactly("dev");
		assertThat(listener.getAddedPropertySources()).isNotEmpty();
		assertThat(listener.getProfiles().getActive()).containsExactly("dev");
		assertThat(listener.getAddedPropertySources().stream().anyMatch((added) -> hasDevProfile(added.getResource())))
			.isTrue();
	}

	@Test
	@WithResource(name = "application.properties", content = """
			spring.profiles.active=dev
			property=value
			#---
			spring.config.activate.on-profile=dev
			property=dev-value1
			""")
	@WithResource(name = "application-dev.properties", content = "property=dev-value2")
	void applyToCanOverrideConfigDataOptions() {
		ConfigDataEnvironmentUpdateListener listener = new ConfigDataEnvironmentUpdateListener() {

			@Override
			public Options onConfigDataOptions(ConfigData configData, PropertySource<?> propertySource,
					Options options) {
				return options.with(ConfigData.Option.IGNORE_PROFILES);
			}

		};
		ConfigDataEnvironmentPostProcessor.applyTo(this.environment, null, null, Collections.emptyList(), listener);
		assertThat(this.environment.getProperty("property")).isEqualTo("value");
		assertThat(this.environment.getActiveProfiles()).isEmpty();
	}

	private void setupMocksAndSpies() {
		this.configDataEnvironment = mock(ConfigDataEnvironment.class);
		this.postProcessor = spy(new ConfigDataEnvironmentPostProcessor(Supplier::get, new DefaultBootstrapContext()));
	}

	private boolean hasDevProfile(ConfigDataResource resource) {
		return (resource instanceof StandardConfigDataResource standardResource)
				&& "dev".equals(standardResource.getProfile());
	}

}
