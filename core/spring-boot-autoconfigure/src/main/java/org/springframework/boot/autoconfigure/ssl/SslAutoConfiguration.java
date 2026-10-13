/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.ssl;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.io.ApplicationResourceLoader;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundleRegistry;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for SSL.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
@AutoConfiguration
@EnableConfigurationProperties(SslProperties.class)
public final class SslAutoConfiguration {

	private final ResourceLoader resourceLoader;

	private final SslProperties sslProperties;

	SslAutoConfiguration(ResourceLoader resourceLoader, SslProperties sslProperties) {
		this.resourceLoader = ApplicationResourceLoader.get(resourceLoader, true);
		this.sslProperties = sslProperties;
	}

	@Bean
	FileWatcher fileWatcher() {
		return new FileWatcher(this.sslProperties.getBundle().getWatch().getFile().getQuietPeriod());
	}

	@Bean
	SslPropertiesBundleRegistrar sslPropertiesSslBundleRegistrar(FileWatcher fileWatcher) {
		return new SslPropertiesBundleRegistrar(this.sslProperties, fileWatcher, this.resourceLoader);
	}

	@Bean
	@ConditionalOnMissingBean({ SslBundleRegistry.class, SslBundles.class })
	DefaultSslBundleRegistry sslBundleRegistry(ObjectProvider<SslBundleRegistrar> sslBundleRegistrars) {
		DefaultSslBundleRegistry registry = new DefaultSslBundleRegistry();
		sslBundleRegistrars.orderedStream().forEach((registrar) -> registrar.registerBundles(registry));
		return registry;
	}

}
