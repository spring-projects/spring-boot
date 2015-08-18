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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.Assert;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.ArtifactStoreBuilder;
import de.flapdoodle.embed.mongo.config.DownloadConfigBuilder;
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

import static de.flapdoodle.embed.process.runtime.Network.localhostIsIPv6;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Embedded Mongo.
 *
 * @author Henryk Konsek
 * @author Andy Wilkinson
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

	@Bean(initMethod = "start", destroyMethod = "stop")
	@ConditionalOnMissingBean
	public MongodExecutable embeddedMongoServer(IMongodConfig mongodConfig,
			IRuntimeConfig runtimeConfig) throws IOException {
		return createEmbeddedMongoServer(mongodConfig, runtimeConfig);
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	@ConditionalOnMissingBean
	public MongodExecutable embeddedMongoServer(IMongodConfig mongodConfig)
			throws IOException {
		return createEmbeddedMongoServer(mongodConfig, null);
	}

	private MongodExecutable createEmbeddedMongoServer(IMongodConfig mongodConfig,
			IRuntimeConfig runtimeConfig) {
		if (getPort() == 0) {
			publishPortInfo(mongodConfig.net().getPort());
		}
		MongodStarter mongodStarter = runtimeConfig == null ? MongodStarter
				.getDefaultInstance() : MongodStarter.getInstance(runtimeConfig);
		return mongodStarter.prepare(mongodConfig);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnClass(Logger.class)
	public IRuntimeConfig embeddedMongoRuntimeConfig() {
		Logger logger = LoggerFactory.getLogger(getClass().getPackage().getName()
				+ ".EmbeddedMongo");

		ProcessOutput processOutput = new ProcessOutput(
				Processors.logTo(logger, Slf4jLevel.INFO),
				Processors.logTo(logger, Slf4jLevel.ERROR),
				Processors.named("[console>]", Processors.logTo(logger, Slf4jLevel.DEBUG)));

		return new RuntimeConfigBuilder()
				.defaultsWithLogger(Command.MongoD, logger)
				.processOutput(processOutput)
				.artifactStore(
						new ArtifactStoreBuilder().defaults(Command.MongoD).download(
								new DownloadConfigBuilder().defaultsForCommand(
										Command.MongoD).progressListener(
										new Slf4jProgressListener(logger)))).build();
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
			builder.net(new Net(getPort(), localhostIsIPv6()));
		}
		return builder.build();
	}

	private int getPort() {
		return this.properties.getPort() == null ? MongoProperties.DEFAULT_PORT
				: this.properties.getPort();
	}

	private void publishPortInfo(int port) {
		setPortProperty(this.context, port);
	}

	private void setPortProperty(ApplicationContext context, int port) {
		if (context instanceof ConfigurableApplicationContext) {
			ConfigurableEnvironment environment = ((ConfigurableApplicationContext) context)
					.getEnvironment();
			MutablePropertySources sources = environment.getPropertySources();
			Map<String, Object> map;
			if (!sources.contains("mongo.ports")) {
				map = new HashMap<String, Object>();
				MapPropertySource source = new MapPropertySource("mongo.ports", map);
				sources.addFirst(source);
			}
			else {
				@SuppressWarnings("unchecked")
				Map<String, Object> value = (Map<String, Object>) sources.get(
						"mongo.ports").getSource();
				map = value;
			}
			map.put("local.mongo.port", port);
		}
		if (this.context.getParent() != null) {
			setPortProperty(this.context.getParent(), port);
		}
	}

	/**
	 * Additional configuration to ensure that {@link MongoClient} beans depend on the
	 * {@code embeddedMongoServer} bean.
	 */
	@Configuration
	@ConditionalOnClass(MongoClient.class)
	protected static class EmbeddedMongoDependencyConfiguration extends
			MongoClientDependsOnBeanFactoryPostProcessor {

		public EmbeddedMongoDependencyConfiguration() {
			super("embeddedMongoServer");
		}

	}

	/**
	 * A workaround for the lack of a {@code toString} implementation on
	 * {@code GenericFeatureAwareVersion}.
	 */
	private static class ToStringFriendlyFeatureAwareVersion implements
			IFeatureAwareVersion {

		private final String version;

		private final Set<Feature> features;

		private ToStringFriendlyFeatureAwareVersion(String version, Set<Feature> features) {
			Assert.notNull(version, "version must not be null");
			this.version = version;
			this.features = features == null ? Collections.<Feature> emptySet()
					: features;
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
			if (getClass() != obj.getClass()) {
				return false;
			}
			ToStringFriendlyFeatureAwareVersion other = (ToStringFriendlyFeatureAwareVersion) obj;
			if (!this.features.equals(other.features)) {
				return false;
			}
			else if (!this.version.equals(other.version)) {
				return false;
			}
			return true;
		}
	}

}
