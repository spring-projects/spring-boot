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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.boot.BootstrapContextClosedEvent;
import org.springframework.boot.BootstrapRegistry.InstanceSupplier;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.MapPropertySource;

/**
 * Test classes used with
 * {@link ConfigDataEnvironmentPostProcessorBootstrapContextIntegrationTests} to show how
 * a bootstrap registry can be used. This example will create helper instances during
 * result and load. It also shows how the helper can ultimately be registered as a bean.
 *
 * @author Phillip Webb
 */
class TestConfigDataBootstrap {

	static class LocationResolver implements ConfigDataLocationResolver<Resource> {

		@Override
		public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
			context.getBootstrapContext().get(Binder.class); // gh-24559
			return location.hasPrefix("testbootstrap:");
		}

		@Override
		public List<Resource> resolve(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
			context.getBootstrapContext().registerIfAbsent(ResolverHelper.class,
					InstanceSupplier.from(() -> new ResolverHelper(location)));
			ResolverHelper helper = context.getBootstrapContext().get(ResolverHelper.class);
			return Collections.singletonList(new Resource(helper));
		}

	}

	static class Loader implements ConfigDataLoader<Resource> {

		@Override
		public ConfigData load(ConfigDataLoaderContext context, Resource location) throws IOException {
			context.getBootstrapContext().registerIfAbsent(LoaderHelper.class,
					(bootstrapContext) -> new LoaderHelper(location, () -> bootstrapContext.get(Binder.class)));
			LoaderHelper helper = context.getBootstrapContext().get(LoaderHelper.class);
			context.getBootstrapContext().addCloseListener(helper);
			return new ConfigData(
					Collections.singleton(new MapPropertySource("loaded", Collections.singletonMap("test", "test"))));
		}

	}

	static class Resource extends ConfigDataResource {

		private final ResolverHelper resolverHelper;

		Resource(ResolverHelper resolverHelper) {
			this.resolverHelper = resolverHelper;
		}

		@Override
		public String toString() {
			return "test";
		}

		ResolverHelper getResolverHelper() {
			return this.resolverHelper;
		}

	}

	static class ResolverHelper {

		private final ConfigDataLocation location;

		ResolverHelper(ConfigDataLocation location) {
			this.location = location;
		}

		ConfigDataLocation getLocation() {
			return this.location;
		}

	}

	static class LoaderHelper implements ApplicationListener<BootstrapContextClosedEvent> {

		private final Resource location;

		private final Supplier<Binder> binder;

		LoaderHelper(Resource location, Supplier<Binder> binder) {
			this.location = location;
			this.binder = binder;
		}

		Resource getLocation() {
			return this.location;
		}

		String getBound() {
			return this.binder.get().bind("myprop", String.class).orElse(null);
		}

		String getProfileBound() {
			return this.binder.get().bind("myprofileprop", String.class).orElse(null);
		}

		@Override
		public void onApplicationEvent(BootstrapContextClosedEvent event) {
			event.getApplicationContext().getBeanFactory().registerSingleton("loaderHelper", this);
		}

	}

}
