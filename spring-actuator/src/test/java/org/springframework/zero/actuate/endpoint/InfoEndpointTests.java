/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.zero.actuate.endpoint;

import java.util.Collections;

import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.zero.actuate.endpoint.InfoEndpoint;
import org.springframework.zero.context.properties.EnableConfigurationProperties;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link InfoEndpoint}.
 * 
 * @author Phillip Webb
 */
public class InfoEndpointTests extends AbstractEndpointTests<InfoEndpoint> {

	public InfoEndpointTests() {
		super(Config.class, InfoEndpoint.class, "/info", true, "endpoints.info");
	}

	@Test
	public void invoke() throws Exception {
		assertThat(getEndpointBean().invoke().get("a"), equalTo((Object) "b"));
	}

	@Configuration
	@EnableConfigurationProperties
	public static class Config {

		@Bean
		public InfoEndpoint endpoint() {
			return new InfoEndpoint(Collections.singletonMap("a", "b"));
		}

	}
}
