/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.liquibase;

import java.lang.reflect.Field;

import liquibase.servicelocator.ServiceLocator;

import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link LiquibaseServiceLocatorApplicationListener}.
 *
 * @author Phillip Webb
 */
public class LiquibaseServiceLocatorApplicationListenerTests {

	@Test
	public void replacesServiceLocator() throws Exception {
		SpringApplication application = new SpringApplication(Conf.class);
		application.setWebEnvironment(false);
		application.run();
		ServiceLocator instance = ServiceLocator.getInstance();
		Field field = ReflectionUtils.findField(ServiceLocator.class, "classResolver");
		field.setAccessible(true);
		Object resolver = field.get(instance);
		assertThat(resolver, instanceOf(SpringPackageScanClassResolver.class));
	}

	@Configuration
	public static class Conf {

	}

}
