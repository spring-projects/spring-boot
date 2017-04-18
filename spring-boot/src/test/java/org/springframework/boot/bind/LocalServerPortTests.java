/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.bind;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.bind.PropertySourcesBindingTests.TestConfig;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LocalServerPort} annotation based injection of local.server.port
 * property.
 *
 * @author Anand Shah
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class LocalServerPortTests {

	private ConfigurableApplicationContext context;

	@Value("${local.server.port:}")
	private String localServerPortFromValue;

	@LocalServerPort
	private String localServerPortFromAnnotation;

	@Test
	public void testLocalServerPortAnnotation() {
		SpringApplication application = new SpringApplication(LocalServerPortTests.class);
		application.setWebEnvironment(true);

		this.context = application.run();

		assertThat(this.localServerPortFromAnnotation).isNotNull().isNotEmpty()
				.isEqualTo(this.localServerPortFromValue).isEqualTo(
						this.context.getEnvironment().getProperty("local.server.port"));
	}

	@Bean
	public JettyEmbeddedServletContainerFactory container() {
		return new JettyEmbeddedServletContainerFactory(8081);
	}
}
