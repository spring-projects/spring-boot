/*
 * Copyright 2012-2015 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.mongo.MongoClientDependsOnBeanFactoryPostProcessor;
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
import org.springframework.util.Assert;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Embedded Mongo.
 *
 * @author Henryk Konsek
 * @author Andy Wilkinson
 * @since 1.3.0
 */
@Configuration
@EnableConfigurationProperties({ MongoProperties.class, EmbeddedMongoProperties.class })
@AutoConfigureBefore(MongoAutoConfiguration.class)
@ConditionalOnClass({ Mongo.class, MongodStarter.class })
public class EmbeddedMongoAutoConfiguration {

	@Autowired
	private MongoProperties properties;

	@Autowired
	private EmbeddedMongoProperties embeddedProperties;

	@Autowired
	private ApplicationContext context;

	@Autowired(required = false)
	private IRuntimeConfig runtimeConfig;

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnClass(Logger.class)
	public IRuntimeConfig embeddedMongoRuntimeConfig() {
		Logger logger = LoggerFactory
				.getLogger(getClass().getPackage().getName() + ".EmbeddedMongo");
		ProcessOutput processOutput = new ProcessOutput(
				Processors.logTo(logger, Slf4jLevel.INFO),
				Processors.logTo(logger, Slf4jLevel.ERROR), Processors.named("[console>]",
						Processors.logTo(logger, Slf4jLevel.DEBUG)));
		return new RuntimeConfigBuilder().defaultsWithLogger(Command.MongoD, logger)
				.processOutput(processOutput).artifactStore(getArtifactStore(logger))
				.build();
	}

	private ArtifactStoreBuilder getArtifactStore(Logger logger) {
		return new ExtractedArtifactStoreBuilder().defaults(Command.MongoD)
				.download(new DownloadConfigBuilder().defaultsForCommand(Command.MongoD)
						.progressListener(new Slf4jProgressListener(logger)));
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	@ConditionalOnMissingBean
	public MongodExecutable embeddedMongoServer(IMongodConfig mongodConfig)
			throws IOException {
		if (getPort() == 0) {
			publishPortInfo(mongodConfig.net().getPort());
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
		if (getPort() > 0) {
			builder.net(new Net(getPort(), Network.localhostIsIPv6()));
		}
		return builder.build();
	}

	private int getPort() {
		if (this.properties.getPort() == null) {
			return MongoProperties.DEFAULT_PORT;
		}
		return this.properties.getPort();
	}

	private void publishPortInfo(int port) {
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
			propertySource = new MapPropertySource("mongo.ports",
					new HashMap<String, Object>());
			sources.addFirst(propertySource);
		}
		return (Map<String, Object>) propertySource.getSource();
	}

	/**
	 * Additional configuration to ensure that {@link MongoClient} beans depend on the
	 * {@code embeddedMongoServer} bean.
	 */
	@Configuration
	@ConditionalOnClass(MongoClient.class)
	protected static class EmbeddedMongoDependencyConfiguration
			extends MongoClientDependsOnBeanFactoryPostProcessor {

		public EmbeddedMongoDependencyConfiguration() {
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
				equals &= this.features.equals(other.features);
				equals &= this.version.equals(other.version);
				return equals;
			}
			return super.equals(obj);
		}

	}

}
