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

package org.springframework.boot.autoconfigure.domain;

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
 * Tests for {@link EntityScanPackages}.
 *
 * @author Phillip Webb
 */
class EntityScanPackagesTests {

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
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages).isNotNull();
		assertThat(packages.getPackageNames()).isEmpty();
	}

	@Test
	void getShouldReturnRegisterPackages() {
		this.context = new AnnotationConfigApplicationContext();
		EntityScanPackages.register(this.context, "a", "b");
		EntityScanPackages.register(this.context, "b", "c");
		this.context.refresh();
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly("a", "b", "c");
	}

	@Test
	void registerFromArrayWhenRegistryIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> EntityScanPackages.register(null))
				.withMessageContaining("Registry must not be null");

	}

	@Test
	void registerFromArrayWhenPackageNamesIsNullShouldThrowException() {
		this.context = new AnnotationConfigApplicationContext();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> EntityScanPackages.register(this.context, (String[]) null))
				.withMessageContaining("PackageNames must not be null");
	}

	@Test
	void registerFromCollectionWhenRegistryIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> EntityScanPackages.register(null, Collections.emptyList()))
				.withMessageContaining("Registry must not be null");
	}

	@Test
	void registerFromCollectionWhenPackageNamesIsNullShouldThrowException() {
		this.context = new AnnotationConfigApplicationContext();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> EntityScanPackages.register(this.context, (Collection<String>) null))
				.withMessageContaining("PackageNames must not be null");
	}

	@Test
	void entityScanAnnotationWhenHasValueAttributeShouldSetupPackages() {
		this.context = new AnnotationConfigApplicationContext(EntityScanValueConfig.class);
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly("a");
	}

	@Test
	void entityScanAnnotationWhenHasValueAttributeShouldSetupPackagesAsm() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.registerBeanDefinition("entityScanValueConfig",
				new RootBeanDefinition(EntityScanValueConfig.class.getName()));
		this.context.refresh();
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly("a");
	}

	@Test
	void entityScanAnnotationWhenHasBasePackagesAttributeShouldSetupPackages() {
		this.context = new AnnotationConfigApplicationContext(EntityScanBasePackagesConfig.class);
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly("b");
	}

	@Test
	void entityScanAnnotationWhenHasValueAndBasePackagesAttributeShouldThrow() {
		assertThatExceptionOfType(AnnotationConfigurationException.class)
				.isThrownBy(() -> this.context = new AnnotationConfigApplicationContext(
						EntityScanValueAndBasePackagesConfig.class));
	}

	@Test
	void entityScanAnnotationWhenHasBasePackageClassesAttributeShouldSetupPackages() {
		this.context = new AnnotationConfigApplicationContext(EntityScanBasePackageClassesConfig.class);
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly(getClass().getPackage().getName());
	}

	@Test
	void entityScanAnnotationWhenNoAttributesShouldSetupPackages() {
		this.context = new AnnotationConfigApplicationContext(EntityScanNoAttributesConfig.class);
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly(getClass().getPackage().getName());
	}

	@Test
	void entityScanAnnotationWhenLoadingFromMultipleConfigsShouldCombinePackages() {
		this.context = new AnnotationConfigApplicationContext(EntityScanValueConfig.class,
				EntityScanBasePackagesConfig.class);
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly("a", "b");
	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan("a")
	static class EntityScanValueConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan(basePackages = "b")
	static class EntityScanBasePackagesConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan(value = "a", basePackages = "b")
	static class EntityScanValueAndBasePackagesConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan(basePackageClasses = EntityScanPackagesTests.class)
	static class EntityScanBasePackageClassesConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan
	static class EntityScanNoAttributesConfig {

	}

}
