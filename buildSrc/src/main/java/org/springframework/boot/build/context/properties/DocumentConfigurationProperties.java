/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.build.context.properties;

import java.io.File;
import java.io.IOException;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import org.springframework.boot.build.context.properties.Snippet.Config;

/**
 * {@link Task} used to document auto-configuration classes.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
public class DocumentConfigurationProperties extends DefaultTask {

	private FileCollection configurationPropertyMetadata;

	private File outputDir;

	/**
	 * Returns the configuration property metadata.
	 * @return The configuration property metadata.
	 */
	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public FileCollection getConfigurationPropertyMetadata() {
		return this.configurationPropertyMetadata;
	}

	/**
	 * Sets the configuration property metadata.
	 * @param configurationPropertyMetadata the file collection containing the
	 * configuration property metadata
	 */
	public void setConfigurationPropertyMetadata(FileCollection configurationPropertyMetadata) {
		this.configurationPropertyMetadata = configurationPropertyMetadata;
	}

	/**
	 * Returns the output directory for the document configuration properties.
	 * @return the output directory
	 */
	@OutputDirectory
	public File getOutputDir() {
		return this.outputDir;
	}

	/**
	 * Sets the output directory for the document configuration properties.
	 * @param outputDir the output directory to be set
	 */
	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

	/**
	 * Generates documentation for the configuration properties.
	 * @throws IOException if an I/O error occurs while writing the documentation
	 */
	@TaskAction
	void documentConfigurationProperties() throws IOException {
		Snippets snippets = new Snippets(this.configurationPropertyMetadata);
		snippets.add("application-properties.core", "Core Properties", this::corePrefixes);
		snippets.add("application-properties.cache", "Cache Properties", this::cachePrefixes);
		snippets.add("application-properties.mail", "Mail Properties", this::mailPrefixes);
		snippets.add("application-properties.json", "JSON Properties", this::jsonPrefixes);
		snippets.add("application-properties.data", "Data Properties", this::dataPrefixes);
		snippets.add("application-properties.transaction", "Transaction Properties", this::transactionPrefixes);
		snippets.add("application-properties.data-migration", "Data Migration Properties", this::dataMigrationPrefixes);
		snippets.add("application-properties.integration", "Integration Properties", this::integrationPrefixes);
		snippets.add("application-properties.web", "Web Properties", this::webPrefixes);
		snippets.add("application-properties.templating", "Templating Properties", this::templatePrefixes);
		snippets.add("application-properties.server", "Server Properties", this::serverPrefixes);
		snippets.add("application-properties.security", "Security Properties", this::securityPrefixes);
		snippets.add("application-properties.rsocket", "RSocket Properties", this::rsocketPrefixes);
		snippets.add("application-properties.actuator", "Actuator Properties", this::actuatorPrefixes);
		snippets.add("application-properties.devtools", "Devtools Properties", this::devtoolsPrefixes);
		snippets.add("application-properties.docker-compose", "Docker Compose Properties", this::dockerComposePrefixes);
		snippets.add("application-properties.testcontainers", "Testcontainers Properties",
				this::testcontainersPrefixes);
		snippets.add("application-properties.testing", "Testing Properties", this::testingPrefixes);
		snippets.writeTo(this.outputDir.toPath());
	}

	/**
	 * Sets the core prefixes for the configuration.
	 * @param config the configuration object
	 */
	private void corePrefixes(Config config) {
		config.accept("debug");
		config.accept("trace");
		config.accept("logging");
		config.accept("spring.aop");
		config.accept("spring.application");
		config.accept("spring.autoconfigure");
		config.accept("spring.banner");
		config.accept("spring.beaninfo");
		config.accept("spring.codec");
		config.accept("spring.config");
		config.accept("spring.info");
		config.accept("spring.jmx");
		config.accept("spring.lifecycle");
		config.accept("spring.main");
		config.accept("spring.messages");
		config.accept("spring.pid");
		config.accept("spring.profiles");
		config.accept("spring.quartz");
		config.accept("spring.reactor");
		config.accept("spring.ssl");
		config.accept("spring.task");
		config.accept("spring.threads");
		config.accept("spring.mandatory-file-encoding");
		config.accept("info");
		config.accept("spring.output.ansi.enabled");
	}

	/**
	 * Caches the prefixes of the given configuration.
	 * @param config the configuration to cache prefixes for
	 */
	private void cachePrefixes(Config config) {
		config.accept("spring.cache");
	}

	/**
	 * This method accepts the prefixes "spring.mail" and "spring.sendgrid" from the given
	 * Config object.
	 * @param config the Config object containing the prefixes to be accepted
	 */
	private void mailPrefixes(Config config) {
		config.accept("spring.mail");
		config.accept("spring.sendgrid");
	}

	/**
	 * Sets the JSON prefixes for the configuration.
	 * @param config the configuration object
	 */
	private void jsonPrefixes(Config config) {
		config.accept("spring.jackson");
		config.accept("spring.gson");
	}

	/**
	 * Sets the data prefixes for the configuration.
	 * @param config the configuration object
	 */
	private void dataPrefixes(Config config) {
		config.accept("spring.couchbase");
		config.accept("spring.cassandra");
		config.accept("spring.elasticsearch");
		config.accept("spring.h2");
		config.accept("spring.influx");
		config.accept("spring.ldap");
		config.accept("spring.mongodb");
		config.accept("spring.neo4j");
		config.accept("spring.dao");
		config.accept("spring.data");
		config.accept("spring.datasource");
		config.accept("spring.jooq");
		config.accept("spring.jdbc");
		config.accept("spring.jpa");
		config.accept("spring.r2dbc");
		config.accept("spring.datasource.oracleucp",
				"Oracle UCP specific settings bound to an instance of Oracle UCP's PoolDataSource");
		config.accept("spring.datasource.dbcp2",
				"Commons DBCP2 specific settings bound to an instance of DBCP2's BasicDataSource");
		config.accept("spring.datasource.tomcat",
				"Tomcat datasource specific settings bound to an instance of Tomcat JDBC's DataSource");
		config.accept("spring.datasource.hikari",
				"Hikari specific settings bound to an instance of Hikari's HikariDataSource");

	}

	/**
	 * Sets the transaction prefixes for the configuration properties.
	 * @param prefix the configuration prefix to accept
	 */
	private void transactionPrefixes(Config prefix) {
		prefix.accept("spring.jta");
		prefix.accept("spring.transaction");
	}

	/**
	 * Migrates the data prefixes for the specified configuration.
	 * @param prefix the configuration prefix to migrate
	 */
	private void dataMigrationPrefixes(Config prefix) {
		prefix.accept("spring.flyway");
		prefix.accept("spring.liquibase");
		prefix.accept("spring.sql.init");
	}

	/**
	 * Sets the integration prefixes for the given configuration prefix.
	 * @param prefix the configuration prefix to set the integration prefixes for
	 */
	private void integrationPrefixes(Config prefix) {
		prefix.accept("spring.activemq");
		prefix.accept("spring.artemis");
		prefix.accept("spring.batch");
		prefix.accept("spring.integration");
		prefix.accept("spring.jms");
		prefix.accept("spring.kafka");
		prefix.accept("spring.pulsar");
		prefix.accept("spring.rabbitmq");
		prefix.accept("spring.hazelcast");
		prefix.accept("spring.webservices");
	}

	/**
	 * Sets the web prefixes for the configuration.
	 * @param prefix the configuration prefix to accept
	 */
	private void webPrefixes(Config prefix) {
		prefix.accept("spring.graphql");
		prefix.accept("spring.hateoas");
		prefix.accept("spring.http");
		prefix.accept("spring.jersey");
		prefix.accept("spring.mvc");
		prefix.accept("spring.netty");
		prefix.accept("spring.resources");
		prefix.accept("spring.servlet");
		prefix.accept("spring.session");
		prefix.accept("spring.web");
		prefix.accept("spring.webflux");
	}

	/**
	 * Sets the template prefixes for the specified configuration.
	 * @param prefix the configuration to set the template prefixes for
	 */
	private void templatePrefixes(Config prefix) {
		prefix.accept("spring.freemarker");
		prefix.accept("spring.groovy");
		prefix.accept("spring.mustache");
		prefix.accept("spring.thymeleaf");
		prefix.accept("spring.groovy.template.configuration", "See GroovyMarkupConfigurer");
	}

	/**
	 * Sets the server prefixes for the configuration.
	 * @param prefix the configuration prefix to be set
	 */
	private void serverPrefixes(Config prefix) {
		prefix.accept("server");
	}

	/**
	 * Sets the security prefixes for the configuration properties.
	 * @param prefix the configuration prefix to be set
	 */
	private void securityPrefixes(Config prefix) {
		prefix.accept("spring.security");
	}

	/**
	 * Sets the RSocket prefixes for the DocumentConfigurationProperties.
	 * @param prefix the Config object containing the RSocket prefixes
	 */
	private void rsocketPrefixes(Config prefix) {
		prefix.accept("spring.rsocket");
	}

	/**
	 * Sets the actuator prefixes for the DocumentConfigurationProperties.
	 * @param prefix the Config object containing the actuator prefixes
	 */
	private void actuatorPrefixes(Config prefix) {
		prefix.accept("management");
		prefix.accept("micrometer");
	}

	/**
	 * Sets the prefixes for Docker Compose configuration properties.
	 * @param prefix the configuration prefix to be set
	 */
	private void dockerComposePrefixes(Config prefix) {
		prefix.accept("spring.docker.compose");
	}

	/**
	 * Sets the devtools prefixes for the DocumentConfigurationProperties.
	 * @param prefix the Config object containing the devtools prefixes
	 */
	private void devtoolsPrefixes(Config prefix) {
		prefix.accept("spring.devtools");
	}

	/**
	 * This method is used to test prefixes for the given configuration properties.
	 * @param prefix The configuration prefix to be tested.
	 */
	private void testingPrefixes(Config prefix) {
		prefix.accept("spring.test.");
	}

	/**
	 * Sets the prefixes for the testcontainers configuration properties.
	 * @param prefix the configuration prefix to be set (e.g. "spring.testcontainers.")
	 */
	private void testcontainersPrefixes(Config prefix) {
		prefix.accept("spring.testcontainers.");
	}

}
