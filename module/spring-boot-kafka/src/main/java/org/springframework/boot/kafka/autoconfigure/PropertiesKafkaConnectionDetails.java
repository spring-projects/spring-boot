/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.kafka.autoconfigure;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Ssl;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Adapts {@link KafkaProperties} to {@link KafkaConnectionDetails}.
 * <p>
 * Configured SSL bundle names are adapted to a {@link LiveSslBundle} rather than being
 * captured, so that Kafka clients follow {@code reload-on-update} bundles.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class PropertiesKafkaConnectionDetails implements KafkaConnectionDetails {

	private static final Log logger = LogFactory.getLog(PropertiesKafkaConnectionDetails.class);

	private final KafkaProperties properties;

	private final @Nullable SslBundles sslBundles;

	private final Map<String, SslBundle> liveBundles = new ConcurrentHashMap<>();

	PropertiesKafkaConnectionDetails(KafkaProperties properties, @Nullable SslBundles sslBundles) {
		this.properties = properties;
		this.sslBundles = sslBundles;
	}

	@Override
	public List<String> getBootstrapServers() {
		return this.properties.getBootstrapServers();
	}

	@Override
	public Configuration getConsumer() {
		List<String> servers = this.properties.getConsumer().getBootstrapServers();
		SslBundle sslBundle = getBundle(this.properties.getConsumer().getSsl());
		String protocol = this.properties.getConsumer().getSecurity().getProtocol();
		return Configuration.of((servers != null) ? servers : getBootstrapServers(),
				(sslBundle != null) ? sslBundle : getSslBundle(),
				(StringUtils.hasLength(protocol)) ? protocol : getSecurityProtocol());
	}

	@Override
	public Configuration getProducer() {
		List<String> servers = this.properties.getProducer().getBootstrapServers();
		SslBundle sslBundle = getBundle(this.properties.getProducer().getSsl());
		String protocol = this.properties.getProducer().getSecurity().getProtocol();
		return Configuration.of((servers != null) ? servers : getBootstrapServers(),
				(sslBundle != null) ? sslBundle : getSslBundle(),
				(StringUtils.hasLength(protocol)) ? protocol : getSecurityProtocol());
	}

	@Override
	public Configuration getStreams() {
		List<String> servers = this.properties.getStreams().getBootstrapServers();
		SslBundle sslBundle = getBundle(this.properties.getStreams().getSsl());
		String protocol = this.properties.getStreams().getSecurity().getProtocol();
		return Configuration.of((servers != null) ? servers : getBootstrapServers(),
				(sslBundle != null) ? sslBundle : getSslBundle(),
				(StringUtils.hasLength(protocol)) ? protocol : getSecurityProtocol());
	}

	@Override
	public Configuration getAdmin() {
		SslBundle sslBundle = getBundle(this.properties.getAdmin().getSsl());
		String protocol = this.properties.getAdmin().getSecurity().getProtocol();
		return Configuration.of(getBootstrapServers(), (sslBundle != null) ? sslBundle : getSslBundle(),
				(StringUtils.hasLength(protocol)) ? protocol : getSecurityProtocol());
	}

	@Override
	public @Nullable SslBundle getSslBundle() {
		return getBundle(this.properties.getSsl());
	}

	@Override
	public @Nullable String getSecurityProtocol() {
		return this.properties.getSecurity().getProtocol();
	}

	private @Nullable SslBundle getBundle(Ssl ssl) {
		String name = ssl.getBundle();
		if (!StringUtils.hasLength(name)) {
			return null;
		}
		SslBundles sslBundles = this.sslBundles;
		Assert.notNull(sslBundles, "SSL bundle name has been set but no SSL bundles found in context");
		return this.liveBundles.computeIfAbsent(name, (bundleName) -> createLiveBundle(sslBundles, bundleName));
	}

	/**
	 * Creates the {@link LiveSslBundle} used for the given name. Cached per name so that
	 * repeated calls hand Kafka the same bundle, leaving
	 * {@link SslBundleSslEngineFactory#shouldBeRebuilt} unaffected, and so that only one
	 * update handler is registered per bundle.
	 * @param sslBundles the bundle registry
	 * @param name the bundle name
	 * @return a bundle that follows updates to the registered bundle
	 * @throws org.springframework.boot.ssl.NoSuchSslBundleException if the name is not
	 * registered
	 */
	private SslBundle createLiveBundle(SslBundles sslBundles, String name) {
		// Registering the handler also validates the name, so an unknown bundle still
		// fails here rather than on first connection.
		sslBundles.addBundleUpdateHandler(name, (updated) -> logger.debug(LogMessage.format(
				"SSL bundle '%s' has been updated, Kafka connections established from now on will use the updated SSL material",
				name)));
		return new LiveSslBundle(sslBundles, name);
	}

}
