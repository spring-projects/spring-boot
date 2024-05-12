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

package org.springframework.boot.autoconfigure.flyway;

import java.util.Collection;

import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.resource.LoadableResource;
import org.junit.jupiter.api.Test;


import org.springframework.boot.testsupport.classpath.ClassPathOverrides;

import static org.assertj.core.api.Assertions.assertThat;

@ClassPathOverrides("org.flywaydb:flyway-core:9.22.3")
class Flyway9NativeImageResourceProviderCustomizerTests {
	private final NativeImageResourceProviderCustomizer customizer = new NativeImageResourceProviderCustomizer();

	@Test
	void nativeImageResourceProviderShouldFindMigrations() {
		FluentConfiguration configuration = new FluentConfiguration();
		this.customizer.customize(configuration);
		ResourceProvider resourceProvider = configuration.getResourceProvider();
		Collection<LoadableResource> migrations = resourceProvider.getResources("V", new String[] { ".sql" });
		LoadableResource migration = resourceProvider.getResource("V1__init.sql");
		assertThat(migrations).containsExactly(migration);
	}
}
