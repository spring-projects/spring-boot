/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.util.Properties;
import java.util.function.Consumer;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProjectMetricsAutoConfiguration}.
 *
 * @author Matthias Friedrich
 */
class ProjectMetricsAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(ProjectMetricsAutoConfiguration.class));

	@Test
	void gitPropertiesNotPresent() {
		this.contextRunner.run(withRegistry((registry) -> assertThat(registry.find("git.info").meter()).isNull()));
	}

	@Test
	void gitPropertiesEmpty() {
		this.contextRunner.withBean(GitProperties.class, new Properties()).run(withRegistry((registry) -> {
			Gauge gauge = registry.find("git.info").gauge();

			assertThat(gauge).isNotNull();
			assertThat(gauge.getId().getTags()).isEmpty();
		}));
	}

	@Test
	void gitPropertiesPresent() {
		Properties props = new Properties();
		props.setProperty("commit.id.abbrev", "cafebabe");
		props.setProperty("commit.time", "2022-04-02T11:19:47+0000");
		props.setProperty("branch", "main");

		this.contextRunner.withBean(GitProperties.class, props).run(withRegistry((registry) -> {
			Gauge gauge = registry.find("git.info").gauge();

			assertThat(gauge).isNotNull();
			assertThat(gauge.getId().getTag("branch")).isEqualTo("main");
			assertThat(gauge.getId().getTag("id")).isEqualTo("cafebabe");
			assertThat(gauge.getId().getTag("time")).isEqualTo("2022-04-02T11:19:47Z");
		}));
	}

	@Test
	void buildPropertiesNotPresent() {
		this.contextRunner.run(withRegistry((registry) -> assertThat(registry.find("build.info").meter()).isNull()));
	}

	@Test
	void buildPropertiesEmpty() {
		this.contextRunner.withBean(BuildProperties.class, new Properties()).run(withRegistry((registry) -> {
			Gauge gauge = registry.find("build.info").gauge();

			assertThat(gauge).isNotNull();
			assertThat(gauge.getId().getTags()).isEmpty();
		}));
	}

	@Test
	void buildPropertiesPresent() {
		Properties props = new Properties();
		props.setProperty("name", "Spring Boot");
		props.setProperty("artifact", "spring-boot");
		props.setProperty("group", "org.springframework.boot");
		props.setProperty("version", "1.0.0");
		props.setProperty("time", "2022-04-02T12:02:13Z");

		this.contextRunner.withBean(BuildProperties.class, props).run(withRegistry((registry) -> {
			Gauge gauge = registry.find("build.info").gauge();

			assertThat(gauge).isNotNull();
			assertThat(gauge.getId().getTag("name")).isEqualTo("Spring Boot");
			assertThat(gauge.getId().getTag("artifact")).isEqualTo("spring-boot");
			assertThat(gauge.getId().getTag("group")).isEqualTo("org.springframework.boot");
			assertThat(gauge.getId().getTag("version")).isEqualTo("1.0.0");
			assertThat(gauge.getId().getTag("time")).isEqualTo("2022-04-02T12:02:13Z");
		}));
	}

	private ContextConsumer<AssertableApplicationContext> withRegistry(Consumer<MeterRegistry> consumer) {
		return (context) -> consumer.accept(context.getBean(MeterRegistry.class));
	}

}
