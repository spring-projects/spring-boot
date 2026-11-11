/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SystemEnvironmentConfigDataLocationResolver}.
 *
 * @author Moritz Halbritter
 */
class SystemEnvironmentConfigDataLocationResolverTests {

	private SystemEnvironmentConfigDataLocationResolver resolver;

	private Map<String, String> environment;

	private ConfigDataLocationResolverContext context;

	@BeforeEach
	void setUp() {
		this.context = mock(ConfigDataLocationResolverContext.class);
		this.environment = new HashMap<>();
		this.resolver = new SystemEnvironmentConfigDataLocationResolver(
				List.of(new PropertiesPropertySourceLoader(), new YamlPropertySourceLoader()), this.environment::get);
	}

	@Test
	void isResolvable() {
		assertThat(this.resolver.isResolvable(this.context, ConfigDataLocation.of("env:VAR1"))).isTrue();
		assertThat(this.resolver.isResolvable(this.context, ConfigDataLocation.of("dummy:VAR1"))).isFalse();
		assertThat(this.resolver.isResolvable(this.context, ConfigDataLocation.of("VAR1"))).isFalse();
	}

	@Test
	void resolveResolves() {
		this.environment.put("VAR1", "VALUE1");
		ConfigDataLocation location = ConfigDataLocation.of("env:VAR1");
		List<SystemEnvironmentConfigDataResource> resolved = this.resolver.resolve(this.context, location);
		assertThat(resolved).hasSize(1);
		SystemEnvironmentConfigDataResource resource = resolved.get(0);
		assertThat(resource.getVariableName()).isEqualTo("VAR1");
		assertThat(resource.getLoader()).isInstanceOf(PropertiesPropertySourceLoader.class);
	}

	@Test
	void resolveWhenHasNoVariableThrowsException() {
		assertThatExceptionOfType(ConfigDataLocationNotFoundException.class)
			.isThrownBy(() -> this.resolver.resolve(this.context, ConfigDataLocation.of("env:VAR1")))
			.withMessage("Environment variable 'VAR1' is not set");
	}

	@Test
	void resolveWhenOptionalAndHasVariableResolves() {
		this.environment.put("VAR1", "VALUE1");
		ConfigDataLocation location = ConfigDataLocation.of("optional:env:VAR1");
		List<SystemEnvironmentConfigDataResource> resolved = this.resolver.resolve(this.context, location);
		assertThat(resolved).hasSize(1);
		SystemEnvironmentConfigDataResource resource = resolved.get(0);
		assertThat(resource.getVariableName()).isEqualTo("VAR1");
		assertThat(resource.getLoader()).isInstanceOf(PropertiesPropertySourceLoader.class);
	}

	@Test
	void resolveWhenOptionalAndHasNoVariableResolvesEmpty() {
		ConfigDataLocation location = ConfigDataLocation.of("optional:env:VAR1");
		List<SystemEnvironmentConfigDataResource> resolved = this.resolver.resolve(this.context, location);
		assertThat(resolved).isEmpty();
	}

	@Test
	void resolveWhenHasPropertiesExtensionHintResolves() {
		this.environment.put("VAR1", "VALUE1");
		ConfigDataLocation location = ConfigDataLocation.of("env:VAR1[.properties]");
		List<SystemEnvironmentConfigDataResource> resolved = this.resolver.resolve(this.context, location);
		assertThat(resolved).hasSize(1);
		SystemEnvironmentConfigDataResource resource = resolved.get(0);
		assertThat(resource.getVariableName()).isEqualTo("VAR1");
		assertThat(resource.getLoader()).isInstanceOf(PropertiesPropertySourceLoader.class);
	}

	@Test
	void resolveWhenHasYamlExtensionHintResolves() {
		this.environment.put("VAR1", "VALUE1");
		ConfigDataLocation location = ConfigDataLocation.of("env:VAR1[.yaml]");
		List<SystemEnvironmentConfigDataResource> resolved = this.resolver.resolve(this.context, location);
		assertThat(resolved).hasSize(1);
		SystemEnvironmentConfigDataResource resource = resolved.get(0);
		assertThat(resource.getVariableName()).isEqualTo("VAR1");
		assertThat(resource.getLoader()).isInstanceOf(YamlPropertySourceLoader.class);
	}

	@Test
	void resolveWhenHasUnknownExtensionHintThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> this.resolver.resolve(this.context, ConfigDataLocation.of("env:VAR1[.dummy]")))
			.withMessage("File extension 'dummy' is not known to any PropertySourceLoader");
	}

}
