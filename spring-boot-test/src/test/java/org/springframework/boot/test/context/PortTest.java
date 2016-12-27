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

package org.springframework.boot.test.context;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.PortTest.RandomPortInitailizer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(initializers = RandomPortInitailizer.class)
public class PortTest {

	@Autowired
	private SomeService service;

	@Test
	public void testName() throws Exception {
		System.out.println(this.service);
		assertThat(this.service.toString()).containsOnlyDigits();
	}

	@Configuration
	static class MyConfig {

		@Bean
		public SomeService someService(@Value("${my.random.port}") int port) {
			return new SomeService(port);
		}

	}

	static class SomeService {

		private final int port;

		public SomeService(int port) {
			this.port = port;
		}

		@Override
		public String toString() {
			return String.valueOf(this.port);
		}

	}

	public static class RandomPortInitailizer
			implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			int randomPort = SocketUtils.findAvailableTcpPort();
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
					"my.random.port=" + randomPort);
		}

	}

}
