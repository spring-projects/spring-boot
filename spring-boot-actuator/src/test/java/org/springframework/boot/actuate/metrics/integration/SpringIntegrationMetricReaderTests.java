/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.metrics.integration;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.support.management.IntegrationManagementConfigurer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringIntegrationMetricReader}.
 *
 * @author Dave Syer
 * @author Artem Bilan
 */
@RunWith(SpringRunner.class)
@SpringBootTest("spring.jmx.enabled=true")
@DirtiesContext
public class SpringIntegrationMetricReaderTests {

	@Autowired
	private SpringIntegrationMetricReader reader;

	@Test
	public void test() {
		assertThat(this.reader.count() > 0).isTrue();
	}

	@Configuration
	@Import({ JmxAutoConfiguration.class, IntegrationAutoConfiguration.class })
	protected static class TestConfiguration {

		@Bean
		public SpringIntegrationMetricReader reader(
				IntegrationManagementConfigurer managementConfigurer) {
			return new SpringIntegrationMetricReader(managementConfigurer);
		}

	}

}
