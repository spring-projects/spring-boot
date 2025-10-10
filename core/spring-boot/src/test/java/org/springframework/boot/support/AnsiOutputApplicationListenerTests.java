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

package org.springframework.boot.support;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiOutput.Enabled;
import org.springframework.boot.ansi.AnsiOutputEnabledValue;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnsiOutputApplicationListener}.
 *
 * @author Phillip Webb
 */
class AnsiOutputApplicationListenerTests {

	private @Nullable ConfigurableApplicationContext context;

	@BeforeEach
	void resetAnsi() {
		AnsiOutput.setEnabled(Enabled.DETECT);
	}

	@AfterEach
	void cleanUp() {
		resetAnsi();
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void enabled() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		Map<String, Object> props = new HashMap<>();
		props.put("spring.output.ansi.enabled", "ALWAYS");
		application.setDefaultProperties(props);
		this.context = application.run();
		assertThat(AnsiOutputEnabledValue.get()).isEqualTo(Enabled.ALWAYS);
	}

	@Test
	void disabled() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		Map<String, Object> props = new HashMap<>();
		props.put("spring.output.ansi.enabled", "never");
		application.setDefaultProperties(props);
		this.context = application.run();
		assertThat(AnsiOutputEnabledValue.get()).isEqualTo(Enabled.NEVER);
	}

	@Test
	@WithResource(name = "application.properties", content = "spring.output.ansi.enabled=never")
	void disabledViaApplicationProperties() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(AnsiOutputEnabledValue.get()).isEqualTo(Enabled.NEVER);
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
