/*
 * Copyright 2012-2020 the original author or authors.
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

import org.springframework.boot.build.context.properties.DocumentOptions.Builder;

/**
 * {@link Task} used to document auto-configuration classes.
 *
 * @author Andy Wilkinson
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
		Builder builder = DocumentOptions.builder();
		builder.addSection("core")
				.withKeyPrefixes("debug", "trace", "logging", "spring.aop", "spring.application",
						"spring.autoconfigure", "spring.banner", "spring.beaninfo", "spring.codec", "spring.config",
						"spring.info", "spring.jmx", "spring.lifecycle", "spring.main", "spring.messages", "spring.pid",
						"spring.profiles", "spring.quartz", "spring.reactor", "spring.task",
						"spring.mandatory-file-encoding", "info", "spring.output.ansi.enabled")
				.addSection("mail").withKeyPrefixes("spring.mail", "spring.sendgrid").addSection("cache")
				.withKeyPrefixes("spring.cache").addSection("server").withKeyPrefixes("server").addSection("web")
				.withKeyPrefixes("spring.hateoas", "spring.http", "spring.servlet", "spring.jersey", "spring.mvc",
						"spring.resources", "spring.session", "spring.webflux")
				.addSection("json").withKeyPrefixes("spring.jackson", "spring.gson").addSection("rsocket")
				.withKeyPrefixes("spring.rsocket").addSection("templating")
				.withKeyPrefixes("spring.freemarker", "spring.groovy", "spring.mustache", "spring.thymeleaf")
				.addOverride("spring.groovy.template.configuration", "See GroovyMarkupConfigurer")
				.addSection("security").withKeyPrefixes("spring.security").addSection("data-migration")
				.withKeyPrefixes("spring.flyway", "spring.liquibase").addSection("data")
				.withKeyPrefixes("spring.couchbase", "spring.elasticsearch", "spring.h2", "spring.influx",
						"spring.ldap", "spring.mongodb", "spring.redis", "spring.dao", "spring.data",
						"spring.datasource", "spring.jooq", "spring.jdbc", "spring.jpa", "spring.r2dbc")
				.addOverride("spring.datasource.dbcp2",
						"Commons DBCP2 specific settings bound to an instance of DBCP2's BasicDataSource")
				.addOverride("spring.datasource.tomcat",
						"Tomcat datasource specific settings bound to an instance of Tomcat JDBC's DataSource")
				.addOverride("spring.datasource.hikari",
						"Hikari specific settings bound to an instance of Hikari's HikariDataSource")
				.addSection("transaction").withKeyPrefixes("spring.jta", "spring.transaction").addSection("integration")
				.withKeyPrefixes("spring.activemq", "spring.artemis", "spring.batch", "spring.integration",
						"spring.jms", "spring.kafka", "spring.rabbitmq", "spring.hazelcast", "spring.webservices")
				.addSection("actuator").withKeyPrefixes("management").addSection("devtools")
				.withKeyPrefixes("spring.devtools").addSection("testing").withKeyPrefixes("spring.test");
		DocumentOptions options = builder.build();
		new ConfigurationMetadataDocumentWriter().writeDocument(this.outputDir.toPath(), options,
				this.configurationPropertyMetadata);
	}

}
