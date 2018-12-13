/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to reproduce reported issues.
 *
 * @author Phillip Webb
 */
public class AutoConfigurationReproTests {

	private ConfigurableApplicationContext context;

	@After
	public void cleanup() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void doesNotEarlyInitializeFactoryBeans() {
		SpringApplication application = new SpringApplication(EarlyInitConfig.class,
				PropertySourcesPlaceholderConfigurer.class,
				ServletWebServerFactoryAutoConfiguration.class);
		this.context = application.run("--server.port=0");
		String bean = (String) this.context.getBean("earlyInit");
		assertThat(bean).isEqualTo("bucket");
	}

	@Configuration
	public static class Config {

	}

	@Configuration
	@ImportResource("classpath:/early-init-test.xml")
	public static class EarlyInitConfig {

	}

}
