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

package org.springframework.boot.autoconfigure.ssl;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundleRegistry;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for SSL.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
@AutoConfiguration
@EnableConfigurationProperties(SslProperties.class)
public class SslAutoConfiguration {

	private final SslProperties sslProperties;

	/**
	 * Constructs a new instance of SslAutoConfiguration with the specified SslProperties.
	 * @param sslProperties the SSL properties to be used for SSL configuration
	 */
	SslAutoConfiguration(SslProperties sslProperties) {
		this.sslProperties = sslProperties;
	}

	/**
	 * Creates a new instance of FileWatcher with the specified quiet period.
	 * @return the created FileWatcher instance
	 */
	@Bean
	FileWatcher fileWatcher() {
		return new FileWatcher(this.sslProperties.getBundle().getWatch().getFile().getQuietPeriod());
	}

	/**
	 * Creates a new instance of {@link SslPropertiesBundleRegistrar} with the provided
	 * {@link SslProperties} and {@link FileWatcher}.
	 * @param sslProperties the {@link SslProperties} to be used for SSL configuration
	 * @param fileWatcher the {@link FileWatcher} to be used for monitoring SSL properties
	 * file changes
	 * @return a new instance of {@link SslPropertiesBundleRegistrar}
	 */
	@Bean
	SslPropertiesBundleRegistrar sslPropertiesSslBundleRegistrar(FileWatcher fileWatcher) {
		return new SslPropertiesBundleRegistrar(this.sslProperties, fileWatcher);
	}

	/**
	 * Creates a default {@link SslBundleRegistry} bean if no other beans of type
	 * {@link SslBundleRegistry} or {@link SslBundles} are present.
	 *
	 * This method uses an {@link ObjectProvider} to obtain all available
	 * {@link SslBundleRegistrar} beans and registers their bundles with the
	 * {@link DefaultSslBundleRegistry}.
	 * @param sslBundleRegistrars the {@link ObjectProvider} of {@link SslBundleRegistrar}
	 * beans
	 * @return the created {@link DefaultSslBundleRegistry} bean
	 */
	@Bean
	@ConditionalOnMissingBean({ SslBundleRegistry.class, SslBundles.class })
	DefaultSslBundleRegistry sslBundleRegistry(ObjectProvider<SslBundleRegistrar> sslBundleRegistrars) {
		DefaultSslBundleRegistry registry = new DefaultSslBundleRegistry();
		sslBundleRegistrars.orderedStream().forEach((registrar) -> registrar.registerBundles(registry));
		return registry;
	}

}
