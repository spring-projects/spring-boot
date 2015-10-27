/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import org.flywaydb.core.Flyway;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link FlywayEndpoint}.
 *
 * @author Eddú Meléndez
 */
public class FlywayEndpointTests extends AbstractEndpointTests<FlywayEndpoint> {

	public FlywayEndpointTests() {
		super(Config.class, FlywayEndpoint.class, "flyway", true, "endpoints.flyway");
	}

	@Test
	public void invoke() throws Exception {
		assertThat(getEndpointBean().invoke().size(), is(1));
	}

	@Configuration
	@Import({ EmbeddedDataSourceConfiguration.class, FlywayAutoConfiguration.class })
	public static class Config {

		@Autowired
		private Flyway flyway;

		@Bean
		public FlywayEndpoint endpoint() {
			return new FlywayEndpoint(this.flyway);
		}

	}

}
