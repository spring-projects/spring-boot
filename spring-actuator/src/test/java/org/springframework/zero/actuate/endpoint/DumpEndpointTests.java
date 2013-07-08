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

import java.lang.management.ThreadInfo;
import java.util.List;

import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.zero.actuate.endpoint.DumpEndpoint;
import org.springframework.zero.context.properties.EnableConfigurationProperties;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link DumpEndpoint}.
 * 
 * @author Phillip Webb
 */
public class DumpEndpointTests extends AbstractEndpointTests<DumpEndpoint> {

	public DumpEndpointTests() {
		super(Config.class, DumpEndpoint.class, "/dump", true, "endpoints.dump");
	}

	@Test
	public void invoke() throws Exception {
		List<ThreadInfo> threadInfo = getEndpointBean().invoke();
		assertThat(threadInfo.size(), greaterThan(0));
	}

	@Configuration
	@EnableConfigurationProperties
	public static class Config {

		@Bean
		public DumpEndpoint endpoint() {
			return new DumpEndpoint();
		}

	}
}
