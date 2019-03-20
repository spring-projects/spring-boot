/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.hypermedia;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;

import groovy.text.GStringTemplateEngine;
import groovy.text.TemplateEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.lang.UsesJava8;

// Flyway must go first
@SpringBootApplication
@Import({ FlywayAutoConfiguration.class, LiquibaseAutoConfiguration.class })
public class SpringBootHypermediaApplication implements CommandLineRunner {

	@Autowired
	private AuditEventRepository auditEventRepository;

	@Bean
	public TemplateEngine groovyTemplateEngine() {
		return new GStringTemplateEngine();
	}

	@Bean
	public EnvironmentEndpoint environmentEndpoint() {
		return new LimitedEnvironmentEndpoint();
	}

	public static void main(String[] args) {
		SpringApplication.run(SpringBootHypermediaApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		this.auditEventRepository.add(
				createEvent("2016-11-01T11:00:00Z", "user", "AUTHENTICATION_FAILURE"));
		this.auditEventRepository.add(
				createEvent("2016-11-01T12:00:00Z", "admin", "AUTHENTICATION_SUCCESS"));
	}

	@UsesJava8
	private AuditEvent createEvent(String instant, String principal, String type) {
		return new AuditEvent(Date.from(Instant.parse(instant)), principal, type,
				Collections.<String, Object>emptyMap());
	}

}
