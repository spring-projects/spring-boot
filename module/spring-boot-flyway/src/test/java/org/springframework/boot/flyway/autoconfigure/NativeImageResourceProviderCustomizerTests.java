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

package org.springframework.boot.flyway.autoconfigure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.resource.LoadableResource;
import org.flywaydb.core.internal.resource.NoopResourceProvider;
import org.flywaydb.core.internal.scanner.Scanner;
import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.ForkedClassPath;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.testsupport.classpath.resources.WithResources;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NativeImageResourceProviderCustomizer}.
 *
 * @author Moritz Halbritter
 * @author Dongliang Xie
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
	@ForkedClassPath
	@WithResource(name = "db/migration/nested/V2__users.sql", content = "select 1;")
	void nativeImageResourceProviderShouldReadNestedMigrations() throws IOException {
		System.setProperty("org.graalvm.nativeimage.imagecode", "true");
		try {
			@SuppressWarnings("unchecked")
			Scanner<Object> scanner = mock(Scanner.class);
			given(scanner.getResources("V", ".sql")).willReturn(Collections.emptyList());
			Location location = Location.fromPath("classpath:", "db/migration");
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			ResourceProvider resourceProvider = new NativeImageResourceProvider(scanner, classLoader, List.of(location),
					StandardCharsets.UTF_8, true);
			Collection<LoadableResource> migrations = resourceProvider.getResources("V", new String[] { ".sql" });
			assertThat(migrations).hasSize(1);
			LoadableResource migration = migrations.iterator().next();
			assertThat(FileCopyUtils.copyToString(migration.read())).isEqualTo("select 1;");
		}
		finally {
			System.clearProperty("org.graalvm.nativeimage.imagecode");
		}
	}

	@Test
	void shouldBackOffOnCustomResourceProvider() {
		FluentConfiguration configuration = new FluentConfiguration();
		configuration.resourceProvider(NoopResourceProvider.INSTANCE);
		this.customizer.customize(configuration);
		assertThat(configuration.getResourceProvider()).isEqualTo(NoopResourceProvider.INSTANCE);
	}

}
