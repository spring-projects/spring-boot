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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.test.context.SpringBootTestContextHierarchyTests.ChildConfiguration;
import org.springframework.boot.test.context.SpringBootTestContextHierarchyTests.ParentConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Tests for {@link SpringBootTest} and {@link ContextHierarchy}.
 *
 * @author Andy Wilkinson
 */
@SpringBootTest
@ContextHierarchy({ @ContextConfiguration(classes = ParentConfiguration.class),
		@ContextConfiguration(classes = ChildConfiguration.class) })
@RunWith(SpringRunner.class)
public class SpringBootTestContextHierarchyTests {

	@Test
	public void contextLoads() {

	}

	@Configuration
	static class ParentConfiguration {

		@Bean
		MyBean myBean() {
			return new MyBean();
		}

	}

	@Configuration
	static class ChildConfiguration {

		ChildConfiguration(MyBean myBean) {

		}

	}

	static class MyBean {

	}

}
