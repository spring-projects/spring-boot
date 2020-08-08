/*
 * Copyright 2012-2020 the original author or authors.
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ConfigDataLocationResolvers}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
@ExtendWith(MockitoExtension.class)
class ConfigDataLocationResolversTests {

	private DeferredLogFactory logFactory = Supplier::get;

	@Mock
	private Binder binder;

	@Mock
	private ConfigDataLocationResolverContext context;

	@Mock
	private Profiles profiles;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	@Test
	void createWhenInjectingBinderCreatesResolver() {
		ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.logFactory, this.binder,
				this.resourceLoader, Collections.singletonList(TestBoundResolver.class.getName()));
		assertThat(resolvers.getResolvers()).hasSize(1);
		assertThat(resolvers.getResolvers().get(0)).isExactlyInstanceOf(TestBoundResolver.class);
		assertThat(((TestBoundResolver) resolvers.getResolvers().get(0)).getBinder()).isSameAs(this.binder);
	}

	@Test
	void createWhenNotInjectingBinderCreatesResolver() {
		ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.logFactory, this.binder,
				this.resourceLoader, Collections.singletonList(TestResolver.class.getName()));
		assertThat(resolvers.getResolvers()).hasSize(1);
		assertThat(resolvers.getResolvers().get(0)).isExactlyInstanceOf(TestResolver.class);
	}

	@Test
	void createWhenNameIsNotConfigDataLocationResolverThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ConfigDataLocationResolvers(this.logFactory, this.binder, this.resourceLoader,
						Collections.singletonList(InputStream.class.getName())))
				.withMessageContaining("Unable to instantiate").havingCause().withMessageContaining("not assignable");
	}

	@Test
	void createOrdersResolvers() {
		List<String> names = new ArrayList<>();
		names.add(TestResolver.class.getName());
		names.add(LowestTestResolver.class.getName());
		names.add(HighestTestResolver.class.getName());
		ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.logFactory, this.binder,
				this.resourceLoader, names);
		assertThat(resolvers.getResolvers().get(0)).isExactlyInstanceOf(HighestTestResolver.class);
		assertThat(resolvers.getResolvers().get(1)).isExactlyInstanceOf(TestResolver.class);
		assertThat(resolvers.getResolvers().get(2)).isExactlyInstanceOf(LowestTestResolver.class);
	}

	@Test
	void resolveAllResolvesUsingFirstSupportedResolver() {
		ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.logFactory, this.binder,
				this.resourceLoader,
				Arrays.asList(LowestTestResolver.class.getName(), HighestTestResolver.class.getName()));
		List<ConfigDataLocation> resolved = resolvers.resolveAll(this.context,
				Collections.singletonList("LowestTestResolver:test"), null);
		assertThat(resolved).hasSize(1);
		TestConfigDataLocation location = (TestConfigDataLocation) resolved.get(0);
		assertThat(location.getResolver()).isInstanceOf(LowestTestResolver.class);
		assertThat(location.getLocation()).isEqualTo("LowestTestResolver:test");
		assertThat(location.isProfileSpecific()).isFalse();
	}

	@Test
	void resolveAllWhenProfileMergesResolvedLocations() {
		ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.logFactory, this.binder,
				this.resourceLoader,
				Arrays.asList(LowestTestResolver.class.getName(), HighestTestResolver.class.getName()));
		List<ConfigDataLocation> resolved = resolvers.resolveAll(this.context,
				Collections.singletonList("LowestTestResolver:test"), this.profiles);
		assertThat(resolved).hasSize(2);
		TestConfigDataLocation location = (TestConfigDataLocation) resolved.get(0);
		assertThat(location.getResolver()).isInstanceOf(LowestTestResolver.class);
		assertThat(location.getLocation()).isEqualTo("LowestTestResolver:test");
		assertThat(location.isProfileSpecific()).isFalse();
		TestConfigDataLocation profileLocation = (TestConfigDataLocation) resolved.get(1);
		assertThat(profileLocation.getResolver()).isInstanceOf(LowestTestResolver.class);
		assertThat(profileLocation.getLocation()).isEqualTo("LowestTestResolver:test");
		assertThat(profileLocation.isProfileSpecific()).isTrue();
	}

	@Test
	void resolveWhenNoResolverThrowsException() {
		ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.logFactory, this.binder,
				this.resourceLoader,
				Arrays.asList(LowestTestResolver.class.getName(), HighestTestResolver.class.getName()));
		assertThatExceptionOfType(UnsupportedConfigDataLocationException.class)
				.isThrownBy(() -> resolvers.resolveAll(this.context, Collections.singletonList("Missing:test"), null))
				.satisfies((ex) -> assertThat(ex.getLocation()).isEqualTo("Missing:test"));
	}

	static class TestResolver implements ConfigDataLocationResolver<TestConfigDataLocation> {

		@Override
		public boolean isResolvable(ConfigDataLocationResolverContext context, String location) {
			String name = getClass().getName();
			name = name.substring(name.lastIndexOf("$") + 1);
			return location.startsWith(name + ":");
		}

		@Override
		public List<TestConfigDataLocation> resolve(ConfigDataLocationResolverContext context, String location) {
			return Collections.singletonList(new TestConfigDataLocation(this, location, false));
		}

		@Override
		public List<TestConfigDataLocation> resolveProfileSpecific(ConfigDataLocationResolverContext context,
				String location, Profiles profiles) {
			return Collections.singletonList(new TestConfigDataLocation(this, location, true));
		}

	}

	static class TestBoundResolver extends TestResolver {

		private final Binder binder;

		TestBoundResolver(Binder binder) {
			this.binder = binder;
		}

		Binder getBinder() {
			return this.binder;
		}

	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	static class HighestTestResolver extends TestResolver {

	}

	@Order(Ordered.LOWEST_PRECEDENCE)
	static class LowestTestResolver extends TestResolver {

	}

	static class TestConfigDataLocation extends ConfigDataLocation {

		private final TestResolver resolver;

		private final String location;

		private final boolean profileSpecific;

		TestConfigDataLocation(TestResolver resolver, String location, boolean profileSpecific) {
			this.resolver = resolver;
			this.location = location;
			this.profileSpecific = profileSpecific;
		}

		TestResolver getResolver() {
			return this.resolver;
		}

		String getLocation() {
			return this.location;
		}

		boolean isProfileSpecific() {
			return this.profileSpecific;
		}

	}

}
