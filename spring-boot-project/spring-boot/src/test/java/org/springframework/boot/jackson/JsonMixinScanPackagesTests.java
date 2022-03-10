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

package org.springframework.boot.jackson;

import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link JsonMixinScanPackages}.
 *
 * @author Guirong Hu
 */
class JsonMixinScanPackagesTests {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void cleanup() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void getWhenNoneRegisteredShouldReturnNone() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.refresh();
		JsonMixinScanPackages packages = JsonMixinScanPackages.get(this.context);
		assertThat(packages).isNotNull();
		assertThat(packages.getPackageNames()).isEmpty();
	}

	@Test
	void getShouldReturnRegisterPackages() {
		this.context = new AnnotationConfigApplicationContext();
		JsonMixinScanPackages.register(this.context, "a", "b");
		JsonMixinScanPackages.register(this.context, "b", "c");
		this.context.refresh();
		JsonMixinScanPackages packages = JsonMixinScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly("a", "b", "c");
	}

	@Test
	void registerFromArrayWhenRegistryIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> JsonMixinScanPackages.register(null))
				.withMessageContaining("Registry must not be null");

	}

	@Test
	void registerFromArrayWhenPackageNamesIsNullShouldThrowException() {
		this.context = new AnnotationConfigApplicationContext();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> JsonMixinScanPackages.register(this.context, (String[]) null))
				.withMessageContaining("PackageNames must not be null");
	}

	@Test
	void registerFromCollectionWhenRegistryIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> JsonMixinScanPackages.register(null, Collections.emptyList()))
				.withMessageContaining("Registry must not be null");
	}

	@Test
	void registerFromCollectionWhenPackageNamesIsNullShouldThrowException() {
		this.context = new AnnotationConfigApplicationContext();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> JsonMixinScanPackages.register(this.context, (Collection<String>) null))
				.withMessageContaining("PackageNames must not be null");
	}

	@Test
	void jsonMixinScanAnnotationWhenHasValueAttributeShouldSetupPackages() {
		this.context = new AnnotationConfigApplicationContext(JsonMixinScanValueConfig.class);
		JsonMixinScanPackages packages = JsonMixinScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly("a");
	}

	@Test
	void jsonMixinScanAnnotationWhenHasValueAttributeShouldSetupPackagesAsm() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.registerBeanDefinition("jsonMixinScanValueConfig",
				new RootBeanDefinition(JsonMixinScanValueConfig.class.getName()));
		this.context.refresh();
		JsonMixinScanPackages packages = JsonMixinScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly("a");
	}

	@Test
	void jsonMixinScanAnnotationWhenHasBasePackagesAttributeShouldSetupPackages() {
		this.context = new AnnotationConfigApplicationContext(JsonMixinScanBasePackagesConfig.class);
		JsonMixinScanPackages packages = JsonMixinScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly("b");
	}

	@Test
	void jsonMixinScanAnnotationWhenHasValueAndBasePackagesAttributeShouldThrow() {
		assertThatExceptionOfType(AnnotationConfigurationException.class)
				.isThrownBy(() -> this.context = new AnnotationConfigApplicationContext(
						JsonMixinScanValueAndBasePackagesConfig.class));
	}

	@Test
	void jsonMixinScanAnnotationWhenHasBasePackageClassesAttributeShouldSetupPackages() {
		this.context = new AnnotationConfigApplicationContext(JsonMixinScanBasePackageClassesConfig.class);
		JsonMixinScanPackages packages = JsonMixinScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly(getClass().getPackage().getName());
	}

	@Test
	void jsonMixinScanAnnotationWhenNoAttributesShouldSetupPackages() {
		this.context = new AnnotationConfigApplicationContext(JsonMixinScanNoAttributesConfig.class);
		JsonMixinScanPackages packages = JsonMixinScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly(getClass().getPackage().getName());
	}

	@Test
	void jsonMixinScanAnnotationWhenLoadingFromMultipleConfigsShouldCombinePackages() {
		this.context = new AnnotationConfigApplicationContext(JsonMixinScanValueConfig.class,
				JsonMixinScanBasePackagesConfig.class);
		JsonMixinScanPackages packages = JsonMixinScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly("a", "b");
	}

	@Configuration(proxyBeanMethods = false)
	@JsonMixinScan("a")
	static class JsonMixinScanValueConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@JsonMixinScan(basePackages = "b")
	static class JsonMixinScanBasePackagesConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@JsonMixinScan(value = "a", basePackages = "b")
	static class JsonMixinScanValueAndBasePackagesConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@JsonMixinScan(basePackageClasses = JsonMixinScanPackagesTests.class)
	static class JsonMixinScanBasePackageClassesConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@JsonMixinScan
	static class JsonMixinScanNoAttributesConfig {

	}

}
