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

package org.springframework.boot.docker.compose.service.connection;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactory;
import org.springframework.boot.docker.compose.core.DockerComposeFile;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.io.ApplicationResourceLoader;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;
import org.springframework.boot.ssl.pem.PemSslStore;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for {@link ConnectionDetailsFactory} implementations that provide
 * {@link ConnectionDetails} from a {@link DockerComposeConnectionSource}.
 *
 * @param <D> the connection details type
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public abstract class DockerComposeConnectionDetailsFactory<D extends ConnectionDetails>
		implements ConnectionDetailsFactory<DockerComposeConnectionSource, D> {

	private final Predicate<DockerComposeConnectionSource> predicate;

	private final String[] requiredClassNames;

	/**
	 * Create a new {@link DockerComposeConnectionDetailsFactory} instance.
	 * @param connectionName the required connection name
	 * @param requiredClassNames the names of classes that must be present
	 */
	protected DockerComposeConnectionDetailsFactory(String connectionName, String... requiredClassNames) {
		this(new ConnectionNamePredicate(connectionName), requiredClassNames);
	}

	/**
	 * Create a new {@link DockerComposeConnectionDetailsFactory} instance.
	 * @param connectionNames the required connection name
	 * @param requiredClassNames the names of classes that must be present
	 * @since 3.2.0
	 */
	protected DockerComposeConnectionDetailsFactory(String[] connectionNames, String... requiredClassNames) {
		this(new ConnectionNamePredicate(connectionNames), requiredClassNames);
	}

	/**
	 * Create a new {@link DockerComposeConnectionDetailsFactory} instance.
	 * @param predicate a predicate used to check when a service is accepted
	 * @param requiredClassNames the names of classes that must be present
	 */
	protected DockerComposeConnectionDetailsFactory(Predicate<DockerComposeConnectionSource> predicate,
			String... requiredClassNames) {
		this.predicate = predicate;
		this.requiredClassNames = requiredClassNames;
	}

	@Override
	public final D getConnectionDetails(DockerComposeConnectionSource source) {
		return (!accept(source)) ? null : getDockerComposeConnectionDetails(source);
	}

	private boolean accept(DockerComposeConnectionSource source) {
		return hasRequiredClasses() && this.predicate.test(source);
	}

	private boolean hasRequiredClasses() {
		return ObjectUtils.isEmpty(this.requiredClassNames) || Arrays.stream(this.requiredClassNames)
			.allMatch((requiredClassName) -> ClassUtils.isPresent(requiredClassName, null));
	}

	/**
	 * Get the {@link ConnectionDetails} from the given {@link RunningService}
	 * {@code source}. May return {@code null} if no connection can be created. Result
	 * types should consider extending {@link DockerComposeConnectionDetails}.
	 * @param source the source
	 * @return the service connection or {@code null}.
	 */
	protected abstract D getDockerComposeConnectionDetails(DockerComposeConnectionSource source);

	/**
	 * Convenient base class for {@link ConnectionDetails} results that are backed by a
	 * {@link RunningService}.
	 */
	protected static class DockerComposeConnectionDetails implements ConnectionDetails, OriginProvider {

		private final Origin origin;

		private volatile SslBundle sslBundle;

		/**
		 * Create a new {@link DockerComposeConnectionDetails} instance.
		 * @param runningService the source {@link RunningService}
		 */
		protected DockerComposeConnectionDetails(RunningService runningService) {
			Assert.notNull(runningService, "'runningService' must not be null");
			this.origin = Origin.from(runningService);
		}

		@Override
		public Origin getOrigin() {
			return this.origin;
		}

		protected SslBundle getSslBundle(RunningService service) {
			if (this.sslBundle != null) {
				return this.sslBundle;
			}
			SslBundle jksSslBundle = getJksSslBundle(service);
			SslBundle pemSslBundle = getPemSslBundle(service);
			if (jksSslBundle == null && pemSslBundle == null) {
				return null;
			}
			if (jksSslBundle != null && pemSslBundle != null) {
				throw new IllegalStateException("Mutually exclusive JKS and PEM ssl bundles have been configured");
			}
			SslBundle sslBundle = (jksSslBundle != null) ? jksSslBundle : pemSslBundle;
			this.sslBundle = sslBundle;
			return sslBundle;
		}

		private SslBundle getJksSslBundle(RunningService service) {
			JksSslStoreDetails keyStoreDetails = getJksSslStoreDetails(service, "keystore");
			JksSslStoreDetails trustStoreDetails = getJksSslStoreDetails(service, "truststore");
			if (keyStoreDetails == null && trustStoreDetails == null) {
				return null;
			}
			SslBundleKey key = SslBundleKey.of(service.labels().get("org.springframework.boot.sslbundle.jks.key.alias"),
					service.labels().get("org.springframework.boot.sslbundle.jks.key.password"));
			SslOptions options = createSslOptions(
					service.labels().get("org.springframework.boot.sslbundle.jks.options.ciphers"),
					service.labels().get("org.springframework.boot.sslbundle.jks.options.enabled-protocols"));
			String protocol = service.labels().get("org.springframework.boot.sslbundle.jks.protocol");
			Path workingDirectory = getWorkingDirectory(service);
			return SslBundle.of(
					new JksSslStoreBundle(keyStoreDetails, trustStoreDetails, getResourceLoader(workingDirectory)), key,
					options, protocol);
		}

		private ResourceLoader getResourceLoader(Path workingDirectory) {
			ClassLoader classLoader = ApplicationResourceLoader.get().getClassLoader();
			return ApplicationResourceLoader.get(classLoader,
					SpringFactoriesLoader.forDefaultResourceLocation(classLoader), workingDirectory);
		}

		private JksSslStoreDetails getJksSslStoreDetails(RunningService service, String storeType) {
			String type = service.labels().get("org.springframework.boot.sslbundle.jks.%s.type".formatted(storeType));
			String provider = service.labels()
				.get("org.springframework.boot.sslbundle.jks.%s.provider".formatted(storeType));
			String location = service.labels()
				.get("org.springframework.boot.sslbundle.jks.%s.location".formatted(storeType));
			String password = service.labels()
				.get("org.springframework.boot.sslbundle.jks.%s.password".formatted(storeType));
			if (location == null) {
				return null;
			}
			return new JksSslStoreDetails(type, provider, location, password);
		}

		private Path getWorkingDirectory(RunningService runningService) {
			DockerComposeFile composeFile = runningService.composeFile();
			if (composeFile == null || CollectionUtils.isEmpty(composeFile.getFiles())) {
				return Path.of(".");
			}
			return composeFile.getFiles().get(0).toPath().getParent();
		}

		private SslOptions createSslOptions(String ciphers, String enabledProtocols) {
			Set<String> ciphersSet = null;
			if (StringUtils.hasLength(ciphers)) {
				ciphersSet = StringUtils.commaDelimitedListToSet(ciphers);
			}
			Set<String> enabledProtocolsSet = null;
			if (StringUtils.hasLength(enabledProtocols)) {
				enabledProtocolsSet = StringUtils.commaDelimitedListToSet(enabledProtocols);
			}
			return SslOptions.of(ciphersSet, enabledProtocolsSet);
		}

		private SslBundle getPemSslBundle(RunningService service) {
			PemSslStoreDetails keyStoreDetails = getPemSslStoreDetails(service, "keystore");
			PemSslStoreDetails trustStoreDetails = getPemSslStoreDetails(service, "truststore");
			if (keyStoreDetails == null && trustStoreDetails == null) {
				return null;
			}
			SslBundleKey key = SslBundleKey.of(service.labels().get("org.springframework.boot.sslbundle.pem.key.alias"),
					service.labels().get("org.springframework.boot.sslbundle.pem.key.password"));
			SslOptions options = createSslOptions(
					service.labels().get("org.springframework.boot.sslbundle.pem.options.ciphers"),
					service.labels().get("org.springframework.boot.sslbundle.pem.options.enabled-protocols"));
			String protocol = service.labels().get("org.springframework.boot.sslbundle.pem.protocol");
			Path workingDirectory = getWorkingDirectory(service);
			ResourceLoader resourceLoader = getResourceLoader(workingDirectory);
			return SslBundle.of(new PemSslStoreBundle(PemSslStore.load(keyStoreDetails, resourceLoader),
					PemSslStore.load(trustStoreDetails, resourceLoader)), key, options, protocol);
		}

		private PemSslStoreDetails getPemSslStoreDetails(RunningService service, String storeType) {
			String type = service.labels().get("org.springframework.boot.sslbundle.pem.%s.type".formatted(storeType));
			String certificate = service.labels()
				.get("org.springframework.boot.sslbundle.pem.%s.certificate".formatted(storeType));
			String privateKey = service.labels()
				.get("org.springframework.boot.sslbundle.pem.%s.private-key".formatted(storeType));
			String privateKeyPassword = service.labels()
				.get("org.springframework.boot.sslbundle.pem.%s.private-key-password".formatted(storeType));
			if (certificate == null && privateKey == null) {
				return null;
			}
			return new PemSslStoreDetails(type, certificate, privateKey, privateKeyPassword);
		}

	}

}
