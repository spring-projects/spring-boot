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

package org.springframework.boot.autoconfigure.msgpack;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link MessagePackAutoConfiguration}.
 *
 * @author Toshiaki Maki
 * @since 1.3.2
 */
public class MessagePackAutoConfigurationTests {
	AnnotationConfigApplicationContext context;

	@Before
	public void setUp() {
		this.context = new AnnotationConfigApplicationContext();
	}

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testMessagePackReadBytes() throws Exception {
		this.context.register(MessagePackAutoConfiguration.class);
		this.context.refresh();
		HttpMessageConverter converter = this.context
				.getBean("messagePackHttpMessageConverter", HttpMessageConverter.class);

		byte[] body = { -126, -93, 102, 111, 111, -91, 104, 101, 108, 108, 111, -93, 98,
				97, 114, -91, 119, 111, 114, 108, 100 };
		MockHttpInputMessage request = new MockHttpInputMessage(body);
		request.getHeaders().setContentType(new MediaType("application", "x-msgpack"));
		Map map = (Map) converter.read(Map.class, request);
		assertThat(map.size(), is(2));
		assertThat(map.get("foo"), is((Object) "hello"));
		assertThat(map.get("bar"), is((Object) "world"));
	}

	@Test
	public void testMessagePackWriteBytes() throws Exception {
		this.context.register(MessagePackAutoConfiguration.class);
		this.context.refresh();
		HttpMessageConverter converter = this.context
				.getBean("messagePackHttpMessageConverter", HttpMessageConverter.class);

		Map<String, Object> obj = new LinkedHashMap<String, Object>();
		obj.put("foo", "hello");
		obj.put("bar", "world");
		MockHttpOutputMessage response = new MockHttpOutputMessage();
		converter.write(obj, new MediaType("application", "x-msgpack"), response);

		byte[] body = { -126, -93, 102, 111, 111, -91, 104, 101, 108, 108, 111, -93, 98,
				97, 114, -91, 119, 111, 114, 108, 100 };
		assertThat(response.getBodyAsBytes(), is(body));
	}

	@Test
	public void testRestTempleIncludingAutoMessagePackMessageConverter()
			throws Exception {
		this.context.register(MessagePackAutoConfiguration.class,
				RestTemplateConfig.class);
		this.context.refresh();
		RestTemplate restTemplate = this.context.getBean(RestTemplate.class);
		assertThat(
				restTemplate.getMessageConverters()
						.get(restTemplate.getMessageConverters().size() - 1),
				is(instanceOf(MessagePackHttpMessageConverter.class)));
	}

	@Test
	public void testRestTempleIncludingManualMessagePackMessageConverter()
			throws Exception {
		this.context.register(MessagePackAutoConfiguration.class,
				MessagePackConfiguredRestTemplateConfig.class);
		this.context.refresh();
		RestTemplate restTemplate = this.context.getBean(RestTemplate.class);
		assertThat(restTemplate.getMessageConverters().get(0),
				is(instanceOf(MessagePackHttpMessageConverter.class)));
	}

	@Configuration
	static class RestTemplateConfig {
		@Bean
		public RestTemplate restTemplate() {
			return new RestTemplate();
		}
	}

	@Configuration
	static class MessagePackConfiguredRestTemplateConfig {
		@Bean
		public RestTemplate restTemplate() {
			RestTemplate restTemplate = new RestTemplate();
			// add first
			restTemplate.getMessageConverters().add(0,
					new MessagePackHttpMessageConverter());
			return restTemplate;
		}
	}
}
