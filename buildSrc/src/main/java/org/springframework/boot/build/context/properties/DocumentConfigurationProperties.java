/*
 * Copyright 2012-present the original author or authors.
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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * {@link Task} used to document configuration properties.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Kim Tae Eun
 */
public abstract class DocumentConfigurationProperties extends DefaultTask {

	private FileCollection configurationPropertyMetadata;

	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public FileCollection getConfigurationPropertyMetadata() {
		return this.configurationPropertyMetadata;
	}

	public void setConfigurationPropertyMetadata(FileCollection configurationPropertyMetadata) {
		this.configurationPropertyMetadata = configurationPropertyMetadata;
	}

	@OutputDirectory
	public abstract DirectoryProperty getOutputDir();

	@Input
	public abstract ListProperty<PropertySectionDefinition> getPropertySections();

	@TaskAction
	void documentConfigurationProperties() throws IOException {
		Snippets snippets = new Snippets(this.configurationPropertyMetadata);

		List<PropertySectionDefinition> sections = getPropertySections().getOrElse(getDefaultPropertySections());
		for (PropertySectionDefinition section : sections) {
			snippets.add(section.getFileName(), section.getTitle(), (config) -> {
				for (String prefix : section.getPrefixes()) {
					if (section.getDescriptions().containsKey(prefix)) {
						config.accept(prefix, section.getDescriptions().get(prefix));
					}
					else {
						config.accept(prefix);
					}
				}
			});
		}

		snippets.writeTo(getOutputDir().getAsFile().get().toPath());
	}

	private List<PropertySectionDefinition> getDefaultPropertySections() {
		return List.of(
				new PropertySectionDefinition("application-properties.core", "Core Properties",
						List.of("debug", "trace", "logging", "spring.aop", "spring.application", "spring.autoconfigure",
								"spring.banner", "spring.beaninfo", "spring.config", "spring.info", "spring.jmx",
								"spring.lifecycle", "spring.main", "spring.messages", "spring.pid", "spring.profiles",
								"spring.quartz", "spring.reactor", "spring.ssl", "spring.task", "spring.threads",
								"spring.validation", "spring.mandatory-file-encoding", "info",
								"spring.output.ansi.enabled"),
						Map.of()),
				new PropertySectionDefinition("application-properties.cache", "Cache Properties",
						List.of("spring.cache"), Map.of()),
				new PropertySectionDefinition("application-properties.mail", "Mail Properties",
						List.of("spring.mail", "spring.sendgrid"), Map.of()),
				new PropertySectionDefinition("application-properties.json", "JSON Properties",
						List.of("spring.jackson", "spring.gson"), Map.of()),
				new PropertySectionDefinition("application-properties.data", "Data Properties",
						List.of("spring.couchbase", "spring.cassandra", "spring.elasticsearch", "spring.h2",
								"spring.influx", "spring.ldap", "spring.mongodb", "spring.neo4j", "spring.dao",
								"spring.data", "spring.datasource", "spring.jooq", "spring.jdbc", "spring.jpa",
								"spring.r2dbc", "spring.datasource.oracleucp", "spring.datasource.dbcp2",
								"spring.datasource.tomcat", "spring.datasource.hikari"),
						Map.of("spring.datasource.oracleucp",
								"Oracle UCP specific settings bound to an instance of Oracle UCP's PoolDataSource",
								"spring.datasource.dbcp2",
								"Commons DBCP2 specific settings bound to an instance of DBCP2's BasicDataSource",
								"spring.datasource.tomcat",
								"Tomcat datasource specific settings bound to an instance of Tomcat JDBC's DataSource",
								"spring.datasource.hikari",
								"Hikari specific settings bound to an instance of Hikari's HikariDataSource")),
				new PropertySectionDefinition("application-properties.transaction", "Transaction Properties",
						List.of("spring.jta", "spring.transaction"), Map.of()),
				new PropertySectionDefinition("application-properties.data-migration", "Data Migration Properties",
						List.of("spring.flyway", "spring.liquibase", "spring.sql.init"), Map.of()),
				new PropertySectionDefinition("application-properties.integration", "Integration Properties",
						List.of("spring.activemq", "spring.artemis", "spring.batch", "spring.integration", "spring.jms",
								"spring.kafka", "spring.pulsar", "spring.rabbitmq", "spring.hazelcast",
								"spring.webservices"),
						Map.of()),
				new PropertySectionDefinition("application-properties.web", "Web Properties",
						List.of("spring.graphql", "spring.hateoas", "spring.http", "spring.jersey", "spring.mvc",
								"spring.netty", "spring.resources", "spring.servlet", "spring.session", "spring.web",
								"spring.webflux"),
						Map.of()),
				new PropertySectionDefinition("application-properties.templating", "Templating Properties",
						List.of("spring.freemarker", "spring.groovy", "spring.mustache", "spring.thymeleaf"), Map.of()),
				new PropertySectionDefinition("application-properties.server", "Server Properties", List.of("server"),
						Map.of()),
				new PropertySectionDefinition("application-properties.security", "Security Properties",
						List.of("spring.security"), Map.of()),
				new PropertySectionDefinition("application-properties.rsocket", "RSocket Properties",
						List.of("spring.rsocket"), Map.of()),
				new PropertySectionDefinition("application-properties.actuator", "Actuator Properties",
						List.of("management", "micrometer"), Map.of()),
				new PropertySectionDefinition("application-properties.devtools", "Devtools Properties",
						List.of("spring.devtools"), Map.of()),
				new PropertySectionDefinition("application-properties.docker-compose", "Docker Compose Properties",
						List.of("spring.docker.compose"), Map.of()),
				new PropertySectionDefinition("application-properties.testcontainers", "Testcontainers Properties",
						List.of("spring.testcontainers."), Map.of()),
				new PropertySectionDefinition("application-properties.testing", "Testing Properties",
						List.of("spring.test."), Map.of()));
	}

	/**
	 * Configuration for a property section.
	 */
	public static class PropertySectionDefinition {

		private final String fileName;

		private final String title;

		private final List<String> prefixes;

		private final Map<String, String> descriptions;

		public PropertySectionDefinition(String fileName, String title, List<String> prefixes,
				Map<String, String> descriptions) {
			this.fileName = fileName;
			this.title = title;
			this.prefixes = prefixes;
			this.descriptions = descriptions;
		}

		public String getFileName() {
			return this.fileName;
		}

		public String getTitle() {
			return this.title;
		}

		public List<String> getPrefixes() {
			return this.prefixes;
		}

		public Map<String, String> getDescriptions() {
			return this.descriptions;
		}

	}

}
