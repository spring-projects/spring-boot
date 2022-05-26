/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.test.mock.mockito;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link SpyBean @SpyBean} with a JDK proxy.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(SpringExtension.class)
class SpyBeanWithJdkProxyTests {

	@Autowired
	private ExampleService service;

	@SpyBean
	private ExampleRepository repository;

	@Test
	void jdkProxyCanBeSpied() {
		Example example = this.service.find("id");
		assertThat(example.id).isEqualTo("id");
		then(this.repository).should().find("id");
	}

	@Configuration(proxyBeanMethods = false)
	@Import(ExampleService.class)
	static class Config {

		@Bean
		ExampleRepository dateService() {
			return (ExampleRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
					new Class<?>[] { ExampleRepository.class }, new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							return new Example((String) args[0]);
						}

					});
		}

	}

	static class ExampleService {

		private final ExampleRepository repository;

		ExampleService(ExampleRepository repository) {
			this.repository = repository;
		}

		Example find(String id) {
			return this.repository.find(id);
		}

	}

	interface ExampleRepository {

		Example find(String id);

	}

	static class Example {

		private final String id;

		Example(String id) {
			this.id = id;
		}

	}

}
