/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docs.features.developingautoconfiguration.testing;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class MyServiceAutoConfigurationTests {

	// tag::runner[]
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(MyServiceAutoConfiguration.class));

	// end::runner[]

	// tag::test-env[]
	@Test
	void serviceNameCanBeConfigured() {
		this.contextRunner.withPropertyValues("user.name=test123").run((context) -> {
			assertThat(context).hasSingleBean(MyService.class);
			assertThat(context.getBean(MyService.class).getName()).isEqualTo("test123");
		});
	}
	// end::test-env[]

	// tag::test-classloader[]
	@Test
	void serviceIsIgnoredIfLibraryIsNotPresent() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(MyService.class))
			.run((context) -> assertThat(context).doesNotHaveBean("myService"));
	}
	// end::test-classloader[]

	// tag::test-user-config[]
	@Test
	void defaultServiceBacksOff() {
		this.contextRunner.withUserConfiguration(UserConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(MyService.class);
			assertThat(context).getBean("myCustomService").isSameAs(context.getBean(MyService.class));
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class UserConfiguration {

		@Bean
		MyService myCustomService() {
			return new MyService("mine");
		}

	}
	// end::test-user-config[]

}
