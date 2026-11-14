/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.redis.autoconfigure;

import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Adapts {@link DataRedisProperties} to {@link DataRedisConnectionDetails}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Yanming Zhou
 * @author Phillip Webb
 */
class PropertiesDataRedisConnectionDetails implements DataRedisConnectionDetails {

	private final DataRedisProperties properties;

	private final @Nullable SslBundles sslBundles;

	PropertiesDataRedisConnectionDetails(DataRedisProperties properties, @Nullable SslBundles sslBundles) {
		this.properties = properties;
		this.sslBundles = sslBundles;
	}

	@Override
	public @Nullable String getUsername() {
		DataRedisUrl redisUrl = getRedisUrl();
		return (redisUrl != null) ? redisUrl.credentials().username() : this.properties.getUsername();
	}

	@Override
	public @Nullable String getPassword() {
		DataRedisUrl redisUrl = getRedisUrl();
		return (redisUrl != null) ? redisUrl.credentials().password() : this.properties.getPassword();
	}

	@Override
	public @Nullable SslBundle getSslBundle() {
		DataRedisUrl redisUrl = getRedisUrl();
		boolean urlUsesSsl = (redisUrl != null) && redisUrl.useSsl();
		if (!this.properties.getSsl().isEnabled() && !urlUsesSsl) {
			return null;
		}
		String bundleName = this.properties.getSsl().getBundle();
		if (StringUtils.hasLength(bundleName)) {
			Assert.notNull(this.sslBundles, "SSL bundle name has been set but no SSL bundles found in context");
			return this.sslBundles.getBundle(bundleName);
		}
		return SslBundle.systemDefault();
	}

	@Override
	public Standalone getStandalone() {
		DataRedisUrl redisUrl = getRedisUrl();
		if (redisUrl != null) {
			int port = redisUrl.uri().getPort();
			return Standalone.of(redisUrl.uri().getHost(), (port != -1) ? port : RedisNode.DEFAULT_PORT,
					redisUrl.database());
		}
		else {
			return Standalone.of(this.properties.getHost(), this.properties.getPort(), this.properties.getDatabase());
		}
	}

	@Override
	public @Nullable Sentinel getSentinel() {
		DataRedisProperties.Sentinel sentinel = this.properties.getSentinel();
		return (sentinel != null) ? new PropertiesSentinel(getStandalone().getDatabase(), sentinel) : null;
	}

	@Override
	public @Nullable Cluster getCluster() {
		DataRedisProperties.Cluster cluster = this.properties.getCluster();
		return (cluster != null) ? new PropertiesCluster(cluster) : null;
	}

	@Override
	public @Nullable MasterReplica getMasterReplica() {
		DataRedisProperties.Masterreplica masterreplica = this.properties.getMasterreplica();
		return (masterreplica != null) ? new PropertiesMasterReplica(getStandalone().getDatabase(), masterreplica)
				: null;
	}

	private @Nullable DataRedisUrl getRedisUrl() {
		return DataRedisUrl.of(this.properties.getUrl());
	}

	private static List<Node> asNodes(@Nullable List<String> nodes, int defaultPort) {
		if (nodes == null) {
			return Collections.emptyList();
		}
		return nodes.stream().map((node) -> asNode(node, defaultPort)).toList();
	}

	private static Node asNode(String node, int defaultPort) {
		int bracketIndex = node.lastIndexOf(']');
		int colonIndex = node.lastIndexOf(':');
		if (colonIndex == -1 || colonIndex < bracketIndex) {
			return new Node(node, defaultPort);
		}
		String host = node.substring(0, colonIndex);
		int port = Integer.parseInt(node.substring(colonIndex + 1));
		return new Node(host, port);
	}

	/**
	 * {@link Cluster} implementation backed by properties.
	 */
	private static class PropertiesCluster implements Cluster {

		private final List<Node> nodes;

		PropertiesCluster(DataRedisProperties.Cluster properties) {
			this.nodes = asNodes(properties.getNodes(), RedisNode.DEFAULT_PORT);
		}

		@Override
		public List<Node> getNodes() {
			return this.nodes;
		}

	}

	/**
	 * {@link MasterReplica} implementation backed by properties.
	 */
	private static class PropertiesMasterReplica implements MasterReplica {

		private final int database;

		private final List<Node> nodes;

		PropertiesMasterReplica(int database, DataRedisProperties.Masterreplica properties) {
			this.database = database;
			this.nodes = asNodes(properties.getNodes(), RedisNode.DEFAULT_PORT);
		}

		@Override
		public int getDatabase() {
			return this.database;
		}

		@Override
		public List<Node> getNodes() {
			return this.nodes;
		}

	}

	/**
	 * {@link Sentinel} implementation backed by properties.
	 */
	private static class PropertiesSentinel implements Sentinel {

		private final int database;

		private final DataRedisProperties.Sentinel properties;

		PropertiesSentinel(int database, DataRedisProperties.Sentinel properties) {
			this.database = database;
			this.properties = properties;
		}

		@Override
		public int getDatabase() {
			return this.database;
		}

		@Override
		public String getMaster() {
			String master = this.properties.getMaster();
			Assert.state(master != null, "'master' must not be null");
			return master;
		}

		@Override
		public List<Node> getNodes() {
			return asNodes(this.properties.getNodes(), RedisNode.DEFAULT_SENTINEL_PORT);
		}

		@Override
		public @Nullable String getUsername() {
			return this.properties.getUsername();
		}

		@Override
		public @Nullable String getPassword() {
			return this.properties.getPassword();
		}

	}

}
