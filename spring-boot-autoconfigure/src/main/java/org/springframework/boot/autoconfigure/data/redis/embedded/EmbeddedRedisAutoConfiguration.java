/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.data.redis.embedded;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.distribution.IVersion;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.io.Slf4jLevel;
import de.flapdoodle.embed.process.io.progress.Slf4jProgressListener;
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.embed.process.store.ArtifactStoreBuilder;
import de.flapdoodle.embed.redis.Command;
import de.flapdoodle.embed.redis.RedisDExecutable;
import de.flapdoodle.embed.redis.RedisDStarter;
import de.flapdoodle.embed.redis.config.AbstractRedisConfig;
import de.flapdoodle.embed.redis.config.DownloadConfigBuilder;
import de.flapdoodle.embed.redis.config.ExtractedArtifactStoreBuilder;
import de.flapdoodle.embed.redis.config.RedisDConfig;
import de.flapdoodle.embed.redis.config.RuntimeConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Embedded Redis.
 *
 * @author Alexey Zhokhov
 * @since 1.4.0
 */
@Configuration
@ConditionalOnClass({ Jedis.class, RedisDStarter.class })
@EnableConfigurationProperties({ RedisProperties.class, EmbeddedRedisProperties.class })
@AutoConfigureBefore(RedisAutoConfiguration.class)
public class EmbeddedRedisAutoConfiguration {

	private static final byte[] IP4_LOOPBACK_ADDRESS = { 127, 0, 0, 1 };

	private static final byte[] IP6_LOOPBACK_ADDRESS = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 1 };

	private final RedisProperties properties;

	private final EmbeddedRedisProperties embeddedProperties;

	private final ApplicationContext context;

	private final IRuntimeConfig runtimeConfig;

	public EmbeddedRedisAutoConfiguration(RedisProperties properties,
			EmbeddedRedisProperties embeddedProperties, ApplicationContext context,
			IRuntimeConfig runtimeConfig) {
		this.properties = properties;
		this.embeddedProperties = embeddedProperties;
		this.context = context;
		this.runtimeConfig = runtimeConfig;
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	@ConditionalOnMissingBean
	public RedisDExecutable embeddedRedisServer(RedisDConfig redisdConfig)
			throws IOException {
		if (getPort() == 0) {
			publishPortInfo(redisdConfig.net().getPort());
		}
		RedisDStarter redisdStarter = getRedisdStarter(this.runtimeConfig);
		return redisdStarter.prepare(redisdConfig);
	}

	private RedisDStarter getRedisdStarter(IRuntimeConfig runtimeConfig) {
		if (runtimeConfig == null) {
			return RedisDStarter.getDefaultInstance();
		}
		return RedisDStarter.getInstance(runtimeConfig);
	}

	@Bean
	@ConditionalOnMissingBean
	public RedisDConfig embeddedRedisConfiguration() throws IOException {
		IVersion featureAwareVersion = new ToStringFriendlyFeatureAwareVersion(
				this.embeddedProperties.getVersion());

		AbstractRedisConfig.Net port = null;
		if (getPort() > 0) {
			port = new AbstractRedisConfig.Net(getPort());
		}
		else {
			// get free server port
			port = new AbstractRedisConfig.Net();
		}

		AbstractRedisConfig.Storage storage = null;
		if (this.embeddedProperties.getStorage() != null) {
			storage = new AbstractRedisConfig.Storage(
					this.embeddedProperties.getStorage().getDatabaseDir(), null, null);
		}
		else {
			storage = new AbstractRedisConfig.Storage();
		}

		return new RedisDConfig(featureAwareVersion, port, storage,
				new AbstractRedisConfig.Timeout());
	}

	private int getPort() {
		if (this.properties.getPort() == null) {
			return RedisProperties.DEFAULT_PORT;
		}
		return this.properties.getPort();
	}

	private InetAddress getHost() throws UnknownHostException {
		if (this.properties.getHost() == null) {
			return InetAddress.getByAddress(Network.localhostIsIPv6()
					? IP6_LOOPBACK_ADDRESS : IP4_LOOPBACK_ADDRESS);
		}
		return InetAddress.getByName(this.properties.getHost());
	}

	private void publishPortInfo(int port) {
		setPortProperty(this.context, port);
	}

	private void setPortProperty(ApplicationContext currentContext, int port) {
		if (currentContext instanceof ConfigurableApplicationContext) {
			MutablePropertySources sources = ((ConfigurableApplicationContext) currentContext)
					.getEnvironment().getPropertySources();
			getRedisPorts(sources).put("local.redis.port", port);
		}
		if (currentContext.getParent() != null) {
			setPortProperty(currentContext.getParent(), port);
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getRedisPorts(MutablePropertySources sources) {
		PropertySource<?> propertySource = sources.get("redis.ports");
		if (propertySource == null) {
			propertySource = new MapPropertySource("redis.ports",
					new HashMap<String, Object>());
			sources.addFirst(propertySource);
		}
		return (Map<String, Object>) propertySource.getSource();
	}

	@Configuration
	@ConditionalOnClass(Logger.class)
	@ConditionalOnMissingBean(IRuntimeConfig.class)
	static class RuntimeConfigConfiguration {

		@Bean
		public IRuntimeConfig embeddedRedisRuntimeConfig() {
			Logger logger = LoggerFactory
					.getLogger(getClass().getPackage().getName() + ".EmbeddedRedis");
			ProcessOutput processOutput = new ProcessOutput(
					Processors.logTo(logger, Slf4jLevel.INFO),
					Processors.logTo(logger, Slf4jLevel.ERROR), Processors.named(
							"[console>]", Processors.logTo(logger, Slf4jLevel.DEBUG)));
			return new RuntimeConfigBuilder().defaultsWithLogger(Command.RedisD, logger)
					.processOutput(processOutput).artifactStore(getArtifactStore(logger))
					.build();
		}

		private ArtifactStoreBuilder getArtifactStore(Logger logger) {
			return new ExtractedArtifactStoreBuilder().defaults(Command.RedisD)
					.download(new DownloadConfigBuilder()
							.defaultsForCommand(Command.RedisD)
							.progressListener(new Slf4jProgressListener(logger)).build());
		}

	}

	/**
	 * A workaround for the lack of a {@code toString} implementation on
	 * {@code GenericFeatureAwareVersion}.
	 */
	private final static class ToStringFriendlyFeatureAwareVersion implements IVersion {

		private final String version;

		private ToStringFriendlyFeatureAwareVersion(String version) {
			Assert.notNull(version, "version must not be null");
			this.version = version;
		}

		@Override
		public String asInDownloadPath() {
			return this.version;
		}

		@Override
		public String toString() {
			return this.version;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + this.version.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() == obj.getClass()) {
				ToStringFriendlyFeatureAwareVersion other = (ToStringFriendlyFeatureAwareVersion) obj;
				boolean equals = true;
				equals &= this.version.equals(other.version);
				return equals;
			}
			return super.equals(obj);
		}

	}

}
