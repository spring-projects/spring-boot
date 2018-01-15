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

package org.springframework.boot.actuate.flyway;

import org.junit.Test;

import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FlywayEndpoint}.
 *
 * @author Eddú Meléndez
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
public class FlywayEndpointTests {

	@Test
	public void flywayReportIsProduced() {
		new ApplicationContextRunner().withUserConfiguration(Config.class)
				.run((context) -> assertThat(
						context.getBean(FlywayEndpoint.class).flywayBeans().getContexts()
								.get(context.getId()).getFlywayBeans()).hasSize(1));
	}

	@Configuration
	@Import({ EmbeddedDataSourceConfiguration.class, FlywayAutoConfiguration.class })
	public static class Config {

		@Bean
		public FlywayEndpoint endpoint(ApplicationContext context) {
			return new FlywayEndpoint(context);
		}

	}

}
