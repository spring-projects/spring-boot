/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.web.client;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.junit.runner.classpath.ClassPathExclusions;
import org.springframework.boot.junit.runner.classpath.ModifiedClassPathRunner;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RestTemplateBuilder} when Netty is used by default.
 *
 * @author Andy Wilkinson
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions({ "httpclient-*.jar", "okhttp-*.jar" })
public class RestTemplateBuilderNettyTests {

	@Test
	public void sslContextIsInitialized() {
		RestTemplate restTemplate = new RestTemplateBuilder().build();
		ClientHttpRequestFactory requestFactory = restTemplate.getRequestFactory();
		assertThat(requestFactory).isInstanceOf(Netty4ClientHttpRequestFactory.class);
		assertThat(ReflectionTestUtils.getField(requestFactory, "sslContext"))
				.isNotNull();
	}

}
