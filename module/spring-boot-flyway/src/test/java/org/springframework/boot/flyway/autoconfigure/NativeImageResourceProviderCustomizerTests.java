/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.flyway.autoconfigure;

import java.util.Collection;

import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.resource.LoadableResource;
import org.flywaydb.core.internal.resource.NoopResourceProvider;
import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.testsupport.classpath.resources.WithResources;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NativeImageResourceProviderCustomizer}.
 *
 * @author Moritz Halbritter
 */
class NativeImageResourceProviderCustomizerTests {

	private final NativeImageResourceProviderCustomizer customizer = new NativeImageResourceProviderCustomizer();

	@Test
	void shouldInstallNativeImageResourceProvider() {
		FluentConfiguration configuration = new FluentConfiguration();
		assertThat(configuration.getResourceProvider()).isNull();
		this.customizer.customize(configuration);
		assertThat(configuration.getResourceProvider()).isInstanceOf(NativeImageResourceProvider.class);
	}

	@Test
	@WithResource(name = "db/migration/V1__init.sql")
	void nativeImageResourceProviderShouldFindMigrations() {
		FluentConfiguration configuration = new FluentConfiguration();
		this.customizer.customize(configuration);
		ResourceProvider resourceProvider = configuration.getResourceProvider();
		Collection<LoadableResource> migrations = resourceProvider.getResources("V", new String[] { ".sql" });
		LoadableResource migration = resourceProvider.getResource("V1__init.sql");
		assertThat(migrations).containsExactly(migration);
	}

	@Test
	@WithResources({ @WithResource(name = "db/migration/V1__init.sql"),
			@WithResource(name = "db/migration/nested/V2__users.sql") })
	void nativeImageResourceProviderShouldFindNestedMigrations() {
		FluentConfiguration configuration = new FluentConfiguration();
		this.customizer.customize(configuration);
		ResourceProvider resourceProvider = configuration.getResourceProvider();
		Collection<LoadableResource> migrations = resourceProvider.getResources("V", new String[] { ".sql" });
		LoadableResource v1 = resourceProvider.getResource("V1__init.sql");
		LoadableResource v2 = resourceProvider.getResource("nested/V2__users.sql");
		assertThat(migrations).containsExactlyInAnyOrder(v1, v2);
	}

	@Test
	void shouldBackOffOnCustomResourceProvider() {
		FluentConfiguration configuration = new FluentConfiguration();
		configuration.resourceProvider(NoopResourceProvider.INSTANCE);
		this.customizer.customize(configuration);
		assertThat(configuration.getResourceProvider()).isEqualTo(NoopResourceProvider.INSTANCE);
	}

}
