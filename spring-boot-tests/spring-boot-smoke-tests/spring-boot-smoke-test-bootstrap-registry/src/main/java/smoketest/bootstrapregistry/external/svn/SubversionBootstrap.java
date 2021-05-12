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

package smoketest.bootstrapregistry.external.svn;

import java.util.function.Function;

import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapRegistryInitializer;

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
			Function<SubversionServerCertificate, SubversionClient> clientFactory) {
		return (registry) -> registry.register(SubversionClient.class,
				(bootstrapContext) -> createSubversionClient(bootstrapContext, clientFactory));
	}

	private static SubversionClient createSubversionClient(BootstrapContext bootstrapContext,
			Function<SubversionServerCertificate, SubversionClient> clientFactory) {
		return clientFactory.apply(bootstrapContext.get(SubversionServerCertificate.class));
	}

}
