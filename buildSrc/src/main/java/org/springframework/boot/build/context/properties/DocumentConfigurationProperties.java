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

	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public FileCollection getConfigurationPropertyMetadata() {
		return this.configurationPropertyMetadata;
	}

	public void setConfigurationPropertyMetadata(FileCollection configurationPropertyMetadata) {
		this.configurationPropertyMetadata = configurationPropertyMetadata;
	}

	@OutputDirectory
	public File getOutputDir() {
		return this.outputDir;
	}

	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

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
		snippets.add("application-properties.docker-compose", "Docker Compose Properties", this::dockerComposePrefixes);
		snippets.add("application-properties.devtools", "Devtools Properties", this::devtoolsPrefixes);
		snippets.add("application-properties.testing", "Testing Properties", this::testingPrefixes);
		snippets.writeTo(this.outputDir.toPath());
	}

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

	private void cachePrefixes(Config config) {
		config.accept("spring.cache");
	}

	private void mailPrefixes(Config config) {
		config.accept("spring.mail");
		config.accept("spring.sendgrid");
	}

	private void jsonPrefixes(Config config) {
		config.accept("spring.jackson");
		config.accept("spring.gson");
	}

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

	private void transactionPrefixes(Config prefix) {
		prefix.accept("spring.jta");
		prefix.accept("spring.transaction");
	}

	private void dataMigrationPrefixes(Config prefix) {
		prefix.accept("spring.flyway");
		prefix.accept("spring.liquibase");
		prefix.accept("spring.sql.init");
	}

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

	private void templatePrefixes(Config prefix) {
		prefix.accept("spring.freemarker");
		prefix.accept("spring.groovy");
		prefix.accept("spring.mustache");
		prefix.accept("spring.thymeleaf");
		prefix.accept("spring.groovy.template.configuration", "See GroovyMarkupConfigurer");
	}

	private void serverPrefixes(Config prefix) {
		prefix.accept("server");
	}

	private void securityPrefixes(Config prefix) {
		prefix.accept("spring.security");
	}

	private void rsocketPrefixes(Config prefix) {
		prefix.accept("spring.rsocket");
	}

	private void actuatorPrefixes(Config prefix) {
		prefix.accept("management");
	}

	private void dockerComposePrefixes(Config prefix) {
		prefix.accept("spring.docker.compose");
	}

	private void devtoolsPrefixes(Config prefix) {
		prefix.accept("spring.devtools");
	}

	private void testingPrefixes(Config prefix) {
		prefix.accept("spring.test");
	}

}
