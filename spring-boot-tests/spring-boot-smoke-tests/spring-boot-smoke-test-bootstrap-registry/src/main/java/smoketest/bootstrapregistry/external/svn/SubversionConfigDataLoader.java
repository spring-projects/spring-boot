/*
 * Copyright 2012-2023 the original author or authors.
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

package smoketest.bootstrapregistry.external.svn;

import java.io.IOException;
import java.util.Collections;

import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapContextClosedEvent;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.BootstrapRegistry.InstanceSupplier;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * {@link ConfigDataLoader} for subversion.
 *
 * @author Phillip Webb
 */
class SubversionConfigDataLoader implements ConfigDataLoader<SubversionConfigDataResource> {

	private static final ApplicationListener<BootstrapContextClosedEvent> closeListener = SubversionConfigDataLoader::onBootstrapContextClosed;

	/**
	 * Constructs a new SubversionConfigDataLoader with the specified BootstrapRegistry.
	 * @param bootstrapRegistry the BootstrapRegistry to be used for registration and
	 * close listener addition
	 */
	SubversionConfigDataLoader(BootstrapRegistry bootstrapRegistry) {
		bootstrapRegistry.registerIfAbsent(SubversionClient.class, this::createSubversionClient);
		bootstrapRegistry.addCloseListener(closeListener);
	}

	/**
	 * Creates a SubversionClient object using the provided BootstrapContext.
	 * @param bootstrapContext the BootstrapContext containing the necessary information
	 * for creating the SubversionClient
	 * @return a SubversionClient object
	 */
	private SubversionClient createSubversionClient(BootstrapContext bootstrapContext) {
		return new SubversionClient(bootstrapContext.get(SubversionServerCertificate.class));
	}

	/**
	 * Loads the configuration data from a SubversionConfigDataResource.
	 * @param context the ConfigDataLoaderContext
	 * @param resource the SubversionConfigDataResource to load from
	 * @return the loaded ConfigData
	 * @throws IOException if an I/O error occurs while loading the data
	 * @throws ConfigDataLocationNotFoundException if the location of the resource is not
	 * found
	 */
	@Override
	public ConfigData load(ConfigDataLoaderContext context, SubversionConfigDataResource resource)
			throws IOException, ConfigDataLocationNotFoundException {
		context.getBootstrapContext()
			.registerIfAbsent(SubversionServerCertificate.class, InstanceSupplier.of(resource.getServerCertificate()));
		SubversionClient client = context.getBootstrapContext().get(SubversionClient.class);
		String loaded = client.load(resource.getLocation());
		PropertySource<?> propertySource = new MapPropertySource("svn", Collections.singletonMap("svn", loaded));
		return new ConfigData(Collections.singleton(propertySource));
	}

	/**
	 * This method is called when the bootstrap context is closed. It registers the
	 * SubversionClient bean in the application context.
	 * @param event the BootstrapContextClosedEvent containing the application context and
	 * bootstrap context
	 */
	private static void onBootstrapContextClosed(BootstrapContextClosedEvent event) {
		event.getApplicationContext()
			.getBeanFactory()
			.registerSingleton("subversionClient", event.getBootstrapContext().get(SubversionClient.class));
	}

}
