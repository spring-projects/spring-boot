/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.mongo.embedded;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.DownloadConfigBuilder;
import de.flapdoodle.embed.mongo.config.ExtractedArtifactStoreBuilder;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.Feature;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.io.Slf4jLevel;
import de.flapdoodle.embed.process.io.progress.Slf4jProgressListener;
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.embed.process.store.ArtifactStoreBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.mongo.MongoClientDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.data.mongo.ReactiveStreamsMongoClientDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.data.mongodb.core.MongoClientFactoryBean;
import org.springframework.data.mongodb.core.ReactiveMongoClientFactoryBean;
import org.springframework.util.Assert;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Embedded Mongo.
 *
 * @author Henryk Konsek
 * @author Andy Wilkinson
 * @author Yogesh Lonkar
 * @author Mark Paluch
 * @since 1.3.0
 */
@Configuration
@EnableConfigurationProperties({ MongoProperties.class, EmbeddedMongoProperties.class })
@AutoConfigureBefore(MongoAutoConfiguration.class)
@ConditionalOnClass({ Mongo.class, MongodStarter.class })
public class EmbeddedMongoAutoConfiguration {

	private static final byte[] IP4_LOOPBACK_ADDRESS = { 127, 0, 0, 1 };

	private static final byte[] IP6_LOOPBACK_ADDRESS = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 1 };

	private final MongoProperties properties;

	private final EmbeddedMongoProperties embeddedProperties;

	private final ApplicationContext context;

	private final IRuntimeConfig runtimeConfig;

	public EmbeddedMongoAutoConfiguration(MongoProperties properties,
			EmbeddedMongoProperties embeddedProperties, ApplicationContext context,
			IRuntimeConfig runtimeConfig) {
		this.properties = properties;
		this.embeddedProperties = embeddedProperties;
		this.context = context;
		this.runtimeConfig = runtimeConfig;
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	@ConditionalOnMissingBean
	public MongodExecutable embeddedMongoServer(IMongodConfig mongodConfig)
			throws IOException {
		Integer configuredPort = this.properties.getPort();
		if (configuredPort == null || configuredPort == 0) {
			setEmbeddedPort(mongodConfig.net().getPort());
		}
		MongodStarter mongodStarter = getMongodStarter(this.runtimeConfig);
		return mongodStarter.prepare(mongodConfig);
	}

	private MongodStarter getMongodStarter(IRuntimeConfig runtimeConfig) {
		if (runtimeConfig == null) {
			return MongodStarter.getDefaultInstance();
		}
		return MongodStarter.getInstance(runtimeConfig);
	}

	@Bean
	@ConditionalOnMissingBean
	public IMongodConfig embeddedMongoConfiguration() throws IOException {
		IFeatureAwareVersion featureAwareVersion = new ToStringFriendlyFeatureAwareVersion(
				this.embeddedProperties.getVersion(),
				this.embeddedProperties.getFeatures());
		MongodConfigBuilder builder = new MongodConfigBuilder()
				.version(featureAwareVersion);
		if (this.embeddedProperties.getStorage() != null) {
			builder.replication(
					new Storage(this.embeddedProperties.getStorage().getDatabaseDir(),
							this.embeddedProperties.getStorage().getReplSetName(),
							this.embeddedProperties.getStorage().getOplogSize() != null
									? this.embeddedProperties.getStorage().getOplogSize()
									: 0));
		}
		Integer configuredPort = this.properties.getPort();
		if (configuredPort != null && configuredPort > 0) {
			builder.net(new Net(getHost().getHostAddress(), configuredPort,
					Network.localhostIsIPv6()));
		}
		else {
			builder.net(new Net(getHost().getHostAddress(),
					Network.getFreeServerPort(getHost()), Network.localhostIsIPv6()));
		}
		return builder.build();
	}

	private InetAddress getHost() throws UnknownHostException {
		if (this.properties.getHost() == null) {
			return InetAddress.getByAddress(Network.localhostIsIPv6()
					? IP6_LOOPBACK_ADDRESS : IP4_LOOPBACK_ADDRESS);
		}
		return InetAddress.getByName(this.properties.getHost());
	}

	private void setEmbeddedPort(int port) {
		setPortProperty(this.context, port);
	}

	private void setPortProperty(ApplicationContext currentContext, int port) {
		if (currentContext instanceof ConfigurableApplicationContext) {
			MutablePropertySources sources = ((ConfigurableApplicationContext) currentContext)
					.getEnvironment().getPropertySources();
			getMongoPorts(sources).put("local.mongo.port", port);
		}
		if (currentContext.getParent() != null) {
			setPortProperty(currentContext.getParent(), port);
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getMongoPorts(MutablePropertySources sources) {
		PropertySource<?> propertySource = sources.get("mongo.ports");
		if (propertySource == null) {
			propertySource = new MapPropertySource("mongo.ports", new HashMap<>());
			sources.addFirst(propertySource);
		}
		return (Map<String, Object>) propertySource.getSource();
	}

	@Configuration
	@ConditionalOnClass(Logger.class)
	@ConditionalOnMissingBean(IRuntimeConfig.class)
	static class RuntimeConfigConfiguration {

		@Bean
		public IRuntimeConfig embeddedMongoRuntimeConfig() {
			Logger logger = LoggerFactory
					.getLogger(getClass().getPackage().getName() + ".EmbeddedMongo");
			ProcessOutput processOutput = new ProcessOutput(
					Processors.logTo(logger, Slf4jLevel.INFO),
					Processors.logTo(logger, Slf4jLevel.ERROR), Processors.named(
							"[console>]", Processors.logTo(logger, Slf4jLevel.DEBUG)));
			return new RuntimeConfigBuilder().defaultsWithLogger(Command.MongoD, logger)
					.processOutput(processOutput).artifactStore(getArtifactStore(logger))
					.build();
		}

		private ArtifactStoreBuilder getArtifactStore(Logger logger) {
			return new ExtractedArtifactStoreBuilder().defaults(Command.MongoD)
					.download(new DownloadConfigBuilder()
							.defaultsForCommand(Command.MongoD)
							.progressListener(new Slf4jProgressListener(logger)).build());
		}

	}

	/**
	 * Additional configuration to ensure that {@link MongoClient} beans depend on the
	 * {@code embeddedMongoServer} bean.
	 */
	@Configuration
	@ConditionalOnClass({ MongoClient.class, MongoClientFactoryBean.class })
	protected static class EmbeddedMongoDependencyConfiguration
			extends MongoClientDependsOnBeanFactoryPostProcessor {

		public EmbeddedMongoDependencyConfiguration() {
			super("embeddedMongoServer");
		}

	}

	/**
	 * Additional configuration to ensure that {@link MongoClient} beans depend on the
	 * {@code embeddedMongoServer} bean.
	 */
	@Configuration
	@ConditionalOnClass({ com.mongodb.reactivestreams.client.MongoClient.class,
			ReactiveMongoClientFactoryBean.class })
	protected static class EmbeddedReactiveMongoDependencyConfiguration
			extends ReactiveStreamsMongoClientDependsOnBeanFactoryPostProcessor {

		public EmbeddedReactiveMongoDependencyConfiguration() {
			super("embeddedMongoServer");
		}

	}

	/**
	 * A workaround for the lack of a {@code toString} implementation on
	 * {@code GenericFeatureAwareVersion}.
	 */
	private final static class ToStringFriendlyFeatureAwareVersion
			implements IFeatureAwareVersion {

		private final String version;

		private final Set<Feature> features;

		private ToStringFriendlyFeatureAwareVersion(String version,
				Set<Feature> features) {
			Assert.notNull(version, "version must not be null");
			this.version = version;
			this.features = (features == null ? Collections.<Feature>emptySet()
					: features);
		}

		@Override
		public String asInDownloadPath() {
			return this.version;
		}

		@Override
		public boolean enabled(Feature feature) {
			return this.features.contains(feature);
		}

		@Override
		public String toString() {
			return this.version;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + this.features.hashCode();
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
				equals = equals && this.features.equals(other.features);
				equals = equals && this.version.equals(other.version);
				return equals;
			}
			return super.equals(obj);
		}

	}

}
