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

package org.springframework.boot.actuate.endpoint;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.junit.Test;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfoEndpoint}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Meang Akira Tanaka
 */
public class InfoEndpointTests extends AbstractEndpointTests<InfoEndpoint> {

	public InfoEndpointTests() {
		super(Config.class, InfoEndpoint.class, "info", false, "endpoints.info");
	}

	@Test
	public void invoke() throws Exception {
		Info actual = ((Info) getEndpointBean().invoke().get("environment"));
		assertThat(actual.get("key1"), equalTo((Object) "value1"));
	}

	@Test
	public void invoke_HasProvider_GetProviderInfo() throws Exception {
		@SuppressWarnings("unchecked")
		Map<String, Object> actual = ((Map<String, Object>) getEndpointBean().invoke().get("infoProvider"));
		assertThat(actual.get("key1"), equalTo((Object) "value1"));
	}

	@Configuration
	@EnableConfigurationProperties
	public static class Config {

		@Bean
		public InfoProvider infoProvider() {
			return new InfoProvider() {

				@Override
				public String name() {
					return "environment";
				}

				@Override
				public Info provide() {
					Info result = new Info();
					result.put("key1", "value1");

					return result;
				}

			};
		}


		@Bean
		public InfoEndpoint endpoint(Map<String, InfoProvider> infoProviders) {
			return new InfoEndpoint(infoProviders);
		}
	}
}
