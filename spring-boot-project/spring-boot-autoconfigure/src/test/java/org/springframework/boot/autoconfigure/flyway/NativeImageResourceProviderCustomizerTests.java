/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.flyway;

import java.util.Collection;

import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.resource.LoadableResource;
import org.flywaydb.core.internal.resource.NoopResourceProvider;
import org.junit.jupiter.api.Test;

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
	void nativeImageResourceProviderShouldFindMigrations() {
		FluentConfiguration configuration = new FluentConfiguration();
		this.customizer.customize(configuration);
		ResourceProvider resourceProvider = configuration.getResourceProvider();
		Collection<LoadableResource> migrations = resourceProvider.getResources("V", new String[] { ".sql" });
		LoadableResource migration = resourceProvider.getResource("V1__init.sql");
		assertThat(migrations).containsExactly(migration);
	}

	@Test
	void shouldBackOffOnCustomResourceProvider() {
		FluentConfiguration configuration = new FluentConfiguration();
		configuration.resourceProvider(NoopResourceProvider.INSTANCE);
		this.customizer.customize(configuration);
		assertThat(configuration.getResourceProvider()).isEqualTo(NoopResourceProvider.INSTANCE);
	}

}
