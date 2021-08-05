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

package org.springframework.boot.autoconfigure.mongo.embedded;

import java.io.File;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Collectors;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.Feature;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.RuntimeConfig;
import de.flapdoodle.embed.process.config.store.DownloadConfig;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link EmbeddedMongoAutoConfiguration}.
 *
 * @author Henryk Konsek
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Issam El-atif
 * @author Chris Bono
 */
class EmbeddedMongoAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void noVersion() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.data.mongodb.port=0").applyTo(this.context);
		this.context.register(MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
				EmbeddedMongoAutoConfiguration.class);
		assertThatThrownBy(() -> this.context.refresh()).hasRootCauseExactlyInstanceOf(IllegalStateException.class)
				.hasRootCauseMessage("Set the spring.mongodb.embedded.version property or define your own MongodConfig "
						+ "bean to use embedded MongoDB");
	}

	@Test
	void customVersion() {
		String version = Version.V3_4_15.asInDownloadPath();
		assertVersionConfiguration(version, version);
	}

	@Test
	void customUnknownVersion() {
		assertVersionConfiguration("3.4.1", "3.4.1");
	}

	@Test
	void customFeatures() {
		EnumSet<Feature> features = EnumSet.of(Feature.TEXT_SEARCH, Feature.SYNC_DELAY, Feature.ONLY_WITH_SSL,
				Feature.NO_HTTP_INTERFACE_ARG);
		if (isWindows()) {
			features.add(Feature.ONLY_WINDOWS_2008_SERVER);
		}
		loadWithValidVersion("spring.mongodb.embedded.features="
				+ features.stream().map(Feature::name).collect(Collectors.joining(", ")));
		assertThat(this.context.getBean(EmbeddedMongoProperties.class).getFeatures())
				.containsExactlyElementsOf(features);
	}

	@Test
	void useRandomPortByDefault() {
		loadWithValidVersion();
		assertThat(this.context.getBeansOfType(MongoClient.class)).hasSize(1);
		MongoClient client = this.context.getBean(MongoClient.class);
		Integer mongoPort = Integer.valueOf(this.context.getEnvironment().getProperty("local.mongo.port"));
		assertThat(getPort(client)).isEqualTo(mongoPort);
	}

	@Test
	void specifyPortToZeroAllocateRandomPort() {
		loadWithValidVersion("spring.data.mongodb.port=0");
		assertThat(this.context.getBeansOfType(MongoClient.class)).hasSize(1);
		MongoClient client = this.context.getBean(MongoClient.class);
		Integer mongoPort = Integer.valueOf(this.context.getEnvironment().getProperty("local.mongo.port"));
		assertThat(getPort(client)).isEqualTo(mongoPort);
	}

	@Test
	void randomlyAllocatedPortIsAvailableWhenCreatingMongoClient() {
		loadWithValidVersion(MongoClientConfiguration.class);
		MongoClient client = this.context.getBean(MongoClient.class);
		Integer mongoPort = Integer.valueOf(this.context.getEnvironment().getProperty("local.mongo.port"));
		assertThat(getPort(client)).isEqualTo(mongoPort);
	}

	@Test
	void portIsAvailableInParentContext() {
		try (ConfigurableApplicationContext parent = new AnnotationConfigApplicationContext()) {
			TestPropertyValues.of("spring.mongodb.embedded.version=3.5.5").applyTo(parent);
			parent.refresh();
			this.context = new AnnotationConfigApplicationContext();
			this.context.setParent(parent);
			this.context.register(EmbeddedMongoAutoConfiguration.class, MongoClientConfiguration.class);
			this.context.refresh();
			assertThat(parent.getEnvironment().getProperty("local.mongo.port")).isNotNull();
		}
	}

	@Test
	void defaultStorageConfiguration() {
		loadWithValidVersion(MongoClientConfiguration.class);
		Storage replication = this.context.getBean(MongodConfig.class).replication();
		assertThat(replication.getOplogSize()).isEqualTo(0);
		assertThat(replication.getDatabaseDir()).isNull();
		assertThat(replication.getReplSetName()).isNull();
	}

	@Test
	void mongoWritesToCustomDatabaseDir(@TempDir Path temp) {
		File customDatabaseDir = new File(temp.toFile(), "custom-database-dir");
		FileSystemUtils.deleteRecursively(customDatabaseDir);
		loadWithValidVersion("spring.mongodb.embedded.storage.databaseDir=" + customDatabaseDir.getPath());
		assertThat(customDatabaseDir).isDirectory();
		assertThat(customDatabaseDir.listFiles()).isNotEmpty();
	}

	@Test
	void customOpLogSizeIsAppliedToConfiguration() {
		loadWithValidVersion("spring.mongodb.embedded.storage.oplogSize=1024KB");
		assertThat(this.context.getBean(MongodConfig.class).replication().getOplogSize()).isEqualTo(1);
	}

	@Test
	void customOpLogSizeUsesMegabytesPerDefault() {
		loadWithValidVersion("spring.mongodb.embedded.storage.oplogSize=10");
		assertThat(this.context.getBean(MongodConfig.class).replication().getOplogSize()).isEqualTo(10);
	}

	@Test
	void customReplicaSetNameIsAppliedToConfiguration() {
		loadWithValidVersion("spring.mongodb.embedded.storage.replSetName=testing");
		assertThat(this.context.getBean(MongodConfig.class).replication().getReplSetName()).isEqualTo("testing");
	}

	@Test
	void customizeDownloadConfiguration() {
		loadWithValidVersion(DownloadConfigBuilderCustomizerConfiguration.class);
		RuntimeConfig runtimeConfig = this.context.getBean(RuntimeConfig.class);
		DownloadConfig downloadConfig = (DownloadConfig) new DirectFieldAccessor(runtimeConfig.artifactStore())
				.getPropertyValue("downloadConfig");
		assertThat(downloadConfig.getUserAgent()).isEqualTo("Test User Agent");
	}

	@Test
	void shutdownHookIsNotRegistered() {
		loadWithValidVersion();
		assertThat(this.context.getBean(MongodExecutable.class).isRegisteredJobKiller()).isFalse();
	}

	@Test
	void customMongoServerConfiguration() {
		loadWithValidVersion(CustomMongoConfiguration.class);
		Map<String, MongoClient> mongoClients = this.context.getBeansOfType(MongoClient.class);
		assertThat(mongoClients).isNotEmpty();
		for (String mongoClientBeanName : mongoClients.keySet()) {
			BeanDefinition beanDefinition = this.context.getBeanFactory().getBeanDefinition(mongoClientBeanName);
			assertThat(beanDefinition.getDependsOn()).contains("customMongoServer");
		}
	}

	private void assertVersionConfiguration(String configuredVersion, String expectedVersion) {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.data.mongodb.port=0").applyTo(this.context);
		if (configuredVersion != null) {
			TestPropertyValues.of("spring.mongodb.embedded.version=" + configuredVersion).applyTo(this.context);
		}
		this.context.register(MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
				EmbeddedMongoAutoConfiguration.class);
		this.context.refresh();
		MongoTemplate mongo = this.context.getBean(MongoTemplate.class);
		Document buildInfo = mongo.executeCommand("{ buildInfo: 1 }");

		assertThat(buildInfo.getString("version")).isEqualTo(expectedVersion);
	}

	private void loadWithValidVersion(String... environment) {
		loadWithValidVersion(null, environment);
	}

	private void loadWithValidVersion(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		if (config != null) {
			ctx.register(config);
		}
		TestPropertyValues.of("spring.mongodb.embedded.version=3.5.5").applyTo(ctx);
		TestPropertyValues.of(environment).applyTo(ctx);
		ctx.register(EmbeddedMongoAutoConfiguration.class, MongoAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	private boolean isWindows() {
		return File.separatorChar == '\\';
	}

	private int getPort(MongoClient client) {
		return client.getClusterDescription().getClusterSettings().getHosts().get(0).getPort();
	}

	@Configuration(proxyBeanMethods = false)
	static class MongoClientConfiguration {

		@Bean
		MongoClient mongoClient(@Value("${local.mongo.port}") int port) {
			return MongoClients.create("mongodb://localhost:" + port);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DownloadConfigBuilderCustomizerConfiguration {

		@Bean
		DownloadConfigBuilderCustomizer testDownloadConfigBuilderCustomizer() {
			return (downloadConfigBuilder) -> downloadConfigBuilder.userAgent("Test User Agent");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomMongoConfiguration {

		@Bean(initMethod = "start", destroyMethod = "stop")
		MongodExecutable customMongoServer(RuntimeConfig runtimeConfig, MongodConfig mongodConfig) {
			MongodStarter mongodStarter = MongodStarter.getInstance(runtimeConfig);
			return mongodStarter.prepare(mongodConfig);
		}

	}

}
