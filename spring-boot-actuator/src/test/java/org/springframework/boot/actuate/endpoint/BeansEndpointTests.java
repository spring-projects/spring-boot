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

package org.springframework.boot.actuate.endpoint;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link BeansEndpoint}.
 *
 * @author Phillip Webb
 */
public class BeansEndpointTests extends AbstractEndpointTests<BeansEndpoint> {

	public BeansEndpointTests() {
		super(Config.class, BeansEndpoint.class, "beans", true, "endpoints.beans");
	}

	@Test
	public void invoke() throws Exception {
		List<Object> result = getEndpointBean().invoke();
		assertEquals(1, result.size());
		assertTrue(result.get(0) instanceof Map);
	}

	@Configuration
	@EnableConfigurationProperties
	public static class Config {

		@Bean
		public BeansEndpoint endpoint() {
			return new BeansEndpoint();
		}

	}
}
