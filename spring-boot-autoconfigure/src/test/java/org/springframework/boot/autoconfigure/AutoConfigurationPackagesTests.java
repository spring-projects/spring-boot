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

package org.springframework.boot.autoconfigure;

import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages.BasePackages;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages.Registrar;
import org.springframework.boot.autoconfigure.packages.one.FirstConfiguration;
import org.springframework.boot.autoconfigure.packages.two.SecondConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Tests for {@link AutoConfigurationPackages}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("resource")
public class AutoConfigurationPackagesTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void setAndGet() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ConfigWithRegistrar.class);
		assertThat(AutoConfigurationPackages.get(context.getBeanFactory()),
				equalTo(Collections.singletonList(getClass().getPackage().getName())));
	}

	@Test
	public void getWithoutSet() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				EmptyConfig.class);
		this.thrown.expect(IllegalStateException.class);
		this.thrown
				.expectMessage("Unable to retrieve @EnableAutoConfiguration base packages");
		AutoConfigurationPackages.get(context.getBeanFactory());
	}
	
	/**
	 * @see #1983
	 */
	@Test
	public void detectsMultipleAutoConfigurationPackages() {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(FirstConfiguration.class, SecondConfiguration.class);
		
		List<String> packages = AutoConfigurationPackages.get(context.getBeanFactory());

		assertThat(packages, hasItems(FirstConfiguration.class.getPackage().getName(), SecondConfiguration.class.getPackage().getName()));
		assertThat(packages, hasSize(2));
	}

	@Configuration
	@Import(AutoConfigurationPackages.Registrar.class)
	static class ConfigWithRegistrar {
	}

	@Configuration
	static class EmptyConfig {
	}

	/**
	 * Test helper to allow {@link Registrar} to be referenced from other packages.
	 *
	 * @author Oliver Gierke
	 */
	public static class TestRegistrar extends Registrar {}
}
