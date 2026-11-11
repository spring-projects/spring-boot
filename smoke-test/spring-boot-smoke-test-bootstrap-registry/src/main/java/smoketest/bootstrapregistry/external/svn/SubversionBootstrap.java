/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.bootstrapregistry.external.svn;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.bootstrap.BootstrapContext;
import org.springframework.boot.bootstrap.BootstrapRegistryInitializer;

/**
 * Allows the user to register a {@link BootstrapRegistryInitializer} with a custom
 * {@link SubversionClient}.
 *
 * @author Phillip Webb
 */
public final class SubversionBootstrap {

	private SubversionBootstrap() {
	}

	/**
	 * Return a {@link BootstrapRegistryInitializer} for the given client factory.
	 * @param clientFactory the client factory
	 * @return a {@link BootstrapRegistryInitializer} instance
	 */
	public static BootstrapRegistryInitializer withCustomClient(
			Function<@Nullable SubversionServerCertificate, SubversionClient> clientFactory) {
		return (registry) -> registry.register(SubversionClient.class,
				(bootstrapContext) -> createSubversionClient(bootstrapContext, clientFactory));
	}

	private static SubversionClient createSubversionClient(BootstrapContext bootstrapContext,
			Function<@Nullable SubversionServerCertificate, SubversionClient> clientFactory) {
		return clientFactory.apply(bootstrapContext.get(SubversionServerCertificate.class));
	}

}
