/*
 * Copyright 2012-2021 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.BootstrapRegistry.InstanceSupplier;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.DefaultBootstrapContext;
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

	private DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();

	@Mock
	private Binder binder;

	@Mock
	private ConfigDataLocationResolverContext context;

	@Mock
	private Profiles profiles;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	@Test
	void createWhenInjectingLogAndDeferredLogFactoryCreatesResolver() {
		ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.logFactory, this.bootstrapContext,
				this.binder, this.resourceLoader, Collections.singletonList(TestLogResolver.class.getName()));
		assertThat(resolvers.getResolvers()).hasSize(1);
		assertThat(resolvers.getResolvers().get(0)).isExactlyInstanceOf(TestLogResolver.class);
		TestLogResolver resolver = (TestLogResolver) resolvers.getResolvers().get(0);
		assertThat(resolver.getDeferredLogFactory()).isSameAs(this.logFactory);
		assertThat(resolver.getLog()).isNotNull();
	}

	@Test
	void createWhenInjectingBinderCreatesResolver() {
		ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.logFactory, this.bootstrapContext,
				this.binder, this.resourceLoader, Collections.singletonList(TestBoundResolver.class.getName()));
		assertThat(resolvers.getResolvers()).hasSize(1);
		assertThat(resolvers.getResolvers().get(0)).isExactlyInstanceOf(TestBoundResolver.class);
		assertThat(((TestBoundResolver) resolvers.getResolvers().get(0)).getBinder()).isSameAs(this.binder);
	}

	@Test
	void createWhenNotInjectingBinderCreatesResolver() {
		ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.logFactory, this.bootstrapContext,
				this.binder, this.resourceLoader, Collections.singletonList(TestResolver.class.getName()));
		assertThat(resolvers.getResolvers()).hasSize(1);
		assertThat(resolvers.getResolvers().get(0)).isExactlyInstanceOf(TestResolver.class);
	}

	@Test
	void createWhenResolverHasBootstrapParametersInjectsBootstrapContext() {
		new ConfigDataLocationResolvers(this.logFactory, this.bootstrapContext, this.binder, this.resourceLoader,
				Collections.singletonList(TestBootstrappingResolver.class.getName()));
		assertThat(this.bootstrapContext.get(String.class)).isEqualTo("boot");
	}

	@Test
	void createWhenNameIsNotConfigDataLocationResolverThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ConfigDataLocationResolvers(this.logFactory, this.bootstrapContext, this.binder,
						this.resourceLoader, Collections.singletonList(InputStream.class.getName())))
				.withMessageContaining("Unable to instantiate").havingCause().withMessageContaining("not assignable");
	}

	@Test
	void createOrdersResolvers() {
		List<String> names = new ArrayList<>();
		names.add(TestResolver.class.getName());
		names.add(LowestTestResolver.class.getName());
		names.add(HighestTestResolver.class.getName());
		ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.logFactory, this.bootstrapContext,
				this.binder, this.resourceLoader, names);
		assertThat(resolvers.getResolvers().get(0)).isExactlyInstanceOf(HighestTestResolver.class);
		assertThat(resolvers.getResolvers().get(1)).isExactlyInstanceOf(TestResolver.class);
		assertThat(resolvers.getResolvers().get(2)).isExactlyInstanceOf(LowestTestResolver.class);
	}

	@Test
	void resolveResolvesUsingFirstSupportedResolver() {
		ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.logFactory, this.bootstrapContext,
				this.binder, this.resourceLoader,
				Arrays.asList(LowestTestResolver.class.getName(), HighestTestResolver.class.getName()));
		ConfigDataLocation location = ConfigDataLocation.of("LowestTestResolver:test");
		List<ConfigDataResolutionResult> resolved = resolvers.resolve(this.context, location, null);
		assertThat(resolved).hasSize(1);
		TestConfigDataResource resource = (TestConfigDataResource) resolved.get(0).getResource();
		assertThat(resource.getResolver()).isInstanceOf(LowestTestResolver.class);
		assertThat(resource.getLocation()).isEqualTo(location);
		assertThat(resource.isProfileSpecific()).isFalse();
	}

	@Test
	void resolveWhenProfileMergesResolvedLocations() {
		ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.logFactory, this.bootstrapContext,
				this.binder, this.resourceLoader,
				Arrays.asList(LowestTestResolver.class.getName(), HighestTestResolver.class.getName()));
		ConfigDataLocation location = ConfigDataLocation.of("LowestTestResolver:test");
		List<ConfigDataResolutionResult> resolved = resolvers.resolve(this.context, location, this.profiles);
		assertThat(resolved).hasSize(2);
		TestConfigDataResource resource = (TestConfigDataResource) resolved.get(0).getResource();
		assertThat(resource.getResolver()).isInstanceOf(LowestTestResolver.class);
		assertThat(resource.getLocation()).isEqualTo(location);
		assertThat(resource.isProfileSpecific()).isFalse();
		TestConfigDataResource profileResource = (TestConfigDataResource) resolved.get(1).getResource();
		assertThat(profileResource.getResolver()).isInstanceOf(LowestTestResolver.class);
		assertThat(profileResource.getLocation()).isEqualTo(location);
		assertThat(profileResource.isProfileSpecific()).isTrue();
	}

	@Test
	void resolveWhenNoResolverThrowsException() {
		ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.logFactory, this.bootstrapContext,
				this.binder, this.resourceLoader,
				Arrays.asList(LowestTestResolver.class.getName(), HighestTestResolver.class.getName()));
		ConfigDataLocation location = ConfigDataLocation.of("Missing:test");
		assertThatExceptionOfType(UnsupportedConfigDataLocationException.class)
				.isThrownBy(() -> resolvers.resolve(this.context, location, null))
				.satisfies((ex) -> assertThat(ex.getLocation()).isEqualTo(location));
	}

	static class TestResolver implements ConfigDataLocationResolver<TestConfigDataResource> {

		@Override
		public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
			String name = getClass().getName();
			name = name.substring(name.lastIndexOf("$") + 1);
			return location.hasPrefix(name + ":");
		}

		@Override
		public List<TestConfigDataResource> resolve(ConfigDataLocationResolverContext context,
				ConfigDataLocation location)
				throws ConfigDataLocationNotFoundException, ConfigDataResourceNotFoundException {
			return Collections.singletonList(new TestConfigDataResource(this, location, false));
		}

		@Override
		public List<TestConfigDataResource> resolveProfileSpecific(ConfigDataLocationResolverContext context,
				ConfigDataLocation location, Profiles profiles) throws ConfigDataLocationNotFoundException {
			return Collections.singletonList(new TestConfigDataResource(this, location, true));
		}

	}

	static class TestLogResolver extends TestResolver {

		private final DeferredLogFactory deferredLogFactory;

		private final Log log;

		TestLogResolver(DeferredLogFactory deferredLogFactory, Log log) {
			this.deferredLogFactory = deferredLogFactory;
			this.log = log;
		}

		DeferredLogFactory getDeferredLogFactory() {
			return this.deferredLogFactory;
		}

		Log getLog() {
			return this.log;
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

	static class TestBootstrappingResolver extends TestResolver {

		TestBootstrappingResolver(ConfigurableBootstrapContext configurableBootstrapContext,
				BootstrapRegistry bootstrapRegistry, BootstrapContext bootstrapContext) {
			assertThat(configurableBootstrapContext).isNotNull();
			assertThat(bootstrapRegistry).isNotNull();
			assertThat(bootstrapContext).isNotNull();
			assertThat(configurableBootstrapContext).isEqualTo(bootstrapRegistry).isEqualTo(bootstrapContext);
			bootstrapRegistry.register(String.class, InstanceSupplier.of("boot"));
		}

	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	static class HighestTestResolver extends TestResolver {

	}

	@Order(Ordered.LOWEST_PRECEDENCE)
	static class LowestTestResolver extends TestResolver {

	}

	static class TestConfigDataResource extends ConfigDataResource {

		private final TestResolver resolver;

		private final ConfigDataLocation location;

		private final boolean profileSpecific;

		TestConfigDataResource(TestResolver resolver, ConfigDataLocation location, boolean profileSpecific) {
			this.resolver = resolver;
			this.location = location;
			this.profileSpecific = profileSpecific;
		}

		TestResolver getResolver() {
			return this.resolver;
		}

		ConfigDataLocation getLocation() {
			return this.location;
		}

		boolean isProfileSpecific() {
			return this.profileSpecific;
		}

	}

}
