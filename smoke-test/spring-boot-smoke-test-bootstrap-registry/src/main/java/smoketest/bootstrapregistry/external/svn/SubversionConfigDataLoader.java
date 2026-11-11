/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.bootstrapregistry.external.svn;

import java.io.IOException;
import java.util.Collections;

import org.springframework.boot.bootstrap.BootstrapContext;
import org.springframework.boot.bootstrap.BootstrapContextClosedEvent;
import org.springframework.boot.bootstrap.BootstrapRegistry;
import org.springframework.boot.bootstrap.BootstrapRegistry.InstanceSupplier;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;

/**
 * {@link ConfigDataLoader} for subversion.
 *
 * @author Phillip Webb
 */
class SubversionConfigDataLoader implements ConfigDataLoader<SubversionConfigDataResource> {

	private static final ApplicationListener<BootstrapContextClosedEvent> closeListener = SubversionConfigDataLoader::onBootstrapContextClosed;

	SubversionConfigDataLoader(BootstrapRegistry bootstrapRegistry) {
		bootstrapRegistry.registerIfAbsent(SubversionClient.class, this::createSubversionClient);
		bootstrapRegistry.addCloseListener(closeListener);
	}

	private SubversionClient createSubversionClient(BootstrapContext bootstrapContext) {
		return new SubversionClient(bootstrapContext.get(SubversionServerCertificate.class));
	}

	@Override
	public ConfigData load(ConfigDataLoaderContext context, SubversionConfigDataResource resource)
			throws IOException, ConfigDataLocationNotFoundException {
		context.getBootstrapContext()
			.registerIfAbsent(SubversionServerCertificate.class, InstanceSupplier.of(resource.getServerCertificate()));
		SubversionClient client = context.getBootstrapContext().get(SubversionClient.class);
		Assert.state(client != null, "'client' must not be null");
		String loaded = client.load(resource.getLocation());
		PropertySource<?> propertySource = new MapPropertySource("svn", Collections.singletonMap("svn", loaded));
		return new ConfigData(Collections.singleton(propertySource));
	}

	private static void onBootstrapContextClosed(BootstrapContextClosedEvent event) {
		SubversionClient subversionClient = event.getBootstrapContext().get(SubversionClient.class);
		Assert.state(subversionClient != null, "'subversionClient' must not be null");
		event.getApplicationContext().getBeanFactory().registerSingleton("subversionClient", subversionClient);
	}

}
