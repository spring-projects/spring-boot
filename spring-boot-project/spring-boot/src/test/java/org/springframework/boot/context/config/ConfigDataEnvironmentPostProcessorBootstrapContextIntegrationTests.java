/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.context.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.config.TestConfigDataBootstrap.LoaderHelper;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ConfigDataEnvironmentPostProcessor} when used with a
 * {@link BootstrapRegistry}.
 *
 * @author Phillip Webb
 */
class ConfigDataEnvironmentPostProcessorBootstrapContextIntegrationTests {

	private SpringApplication application;

	@BeforeEach
	void setup() {
		this.application = new SpringApplication(Config.class);
		this.application.setWebApplicationType(WebApplicationType.NONE);
	}

	@Test
	@WithResource(name = "imported.properties", content = """
			spring.config.import=testbootstrap:test
			spring.profiles.active=test
			myprop=igotbound
			#---
			spring.config.activate.on-profile=test
			myprofileprop=igotprofilebound

			""")
	@WithResource(name = "META-INF/spring.factories", content = """
			org.springframework.boot.context.config.ConfigDataLoader=\
			org.springframework.boot.context.config.TestConfigDataBootstrap.Loader
			org.springframework.boot.context.config.ConfigDataLocationResolver=\
			org.springframework.boot.context.config.TestConfigDataBootstrap.LocationResolver
			""")
	void bootstrapsApplicationContext() {
		try (ConfigurableApplicationContext context = this.application
			.run("--spring.config.import=classpath:imported.properties")) {
			LoaderHelper bean = context.getBean(TestConfigDataBootstrap.LoaderHelper.class);
			assertThat(bean).isNotNull();
			assertThat(bean.getBound()).isEqualTo("igotbound");
			assertThat(bean.getProfileBound()).isEqualTo("igotprofilebound");
			assertThat(bean.getLocation().getResolverHelper().getLocation())
				.isEqualTo(ConfigDataLocation.of("testbootstrap:test"));
		}
	}

	@Configuration
	static class Config {

	}

}
