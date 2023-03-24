/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.context;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} with application arguments.
 *
 * @author Justin Griffin
 * @author Stephane Nicoll
 */
@SpringBootTest(args = { "--option.foo=foo-value", "other.bar=other-bar-value" })
class SpringBootTestArgsTests {

	@Autowired
	private ApplicationArguments args;

	@Test
	void applicationArgumentsPopulated() {
		assertThat(this.args.getOptionNames()).containsOnly("option.foo");
		assertThat(this.args.getOptionValues("option.foo")).containsOnly("foo-value");
		assertThat(this.args.getNonOptionArgs()).containsOnly("other.bar=other-bar-value");
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
