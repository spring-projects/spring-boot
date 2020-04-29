/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.packagestest.one.FirstConfiguration;
import org.springframework.boot.autoconfigure.packagestest.two.SecondConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link AutoConfigurationPackages}.
 *
 * @author Phillip Webb
 * @author Oliver Gierke
 */
@SuppressWarnings("resource")
public class AutoConfigurationPackagesTests {

	@Test
	void setAndGet() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ConfigWithAutoConfigurationPackage.class);
		assertThat(AutoConfigurationPackages.get(context.getBeanFactory()))
				.containsExactly(getClass().getPackage().getName());
	}

	@Test
	void getWithoutSet() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(EmptyConfig.class);
		assertThatIllegalStateException().isThrownBy(() -> AutoConfigurationPackages.get(context.getBeanFactory()))
				.withMessageContaining("Unable to retrieve @EnableAutoConfiguration base packages");
	}

	@Test
	void detectsMultipleAutoConfigurationPackages() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(FirstConfiguration.class,
				SecondConfiguration.class);
		List<String> packages = AutoConfigurationPackages.get(context.getBeanFactory());
		Package package1 = FirstConfiguration.class.getPackage();
		Package package2 = SecondConfiguration.class.getPackage();
		assertThat(packages).containsOnly(package1.getName(), package2.getName());
	}

	@Test
	void whenBasePackagesAreSpecifiedThenTheyAreRegistered() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ConfigWithAutoConfigurationBasePackages.class);
		List<String> packages = AutoConfigurationPackages.get(context.getBeanFactory());
		assertThat(packages).containsExactly("com.example.alpha", "com.example.bravo");
	}

	@Test
	void whenBasePackageClassesAreSpecifiedThenTheirPackagesAreRegistered() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ConfigWithAutoConfigurationBasePackageClasses.class);
		List<String> packages = AutoConfigurationPackages.get(context.getBeanFactory());
		assertThat(packages).containsOnly(FirstConfiguration.class.getPackage().getName(),
				SecondConfiguration.class.getPackage().getName());
	}

	@Configuration(proxyBeanMethods = false)
	@AutoConfigurationPackage
	static class ConfigWithAutoConfigurationPackage {

	}

	@Configuration(proxyBeanMethods = false)
	@AutoConfigurationPackage(basePackages = { "com.example.alpha", "com.example.bravo" })
	static class ConfigWithAutoConfigurationBasePackages {

	}

	@Configuration(proxyBeanMethods = false)
	@AutoConfigurationPackage(basePackageClasses = { FirstConfiguration.class, SecondConfiguration.class })
	static class ConfigWithAutoConfigurationBasePackageClasses {

	}

	@Configuration(proxyBeanMethods = false)
	static class EmptyConfig {

	}

}
