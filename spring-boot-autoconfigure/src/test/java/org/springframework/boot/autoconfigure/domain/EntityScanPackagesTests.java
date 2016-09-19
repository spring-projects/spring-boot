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

package org.springframework.boot.autoconfigure.domain;

import java.util.Collection;
import java.util.Collections;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EntityScanPackages}.
 *
 * @author Phillip Webb
 */
public class EntityScanPackagesTests {

	private AnnotationConfigApplicationContext context;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@After
	public void cleanup() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void getWhenNoneRegisteredShouldReturnNone() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.refresh();
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages).isNotNull();
		assertThat(packages.getPackageNames()).isEmpty();
	}

	@Test
	public void getShouldReturnRegisterPackages() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EntityScanPackages.register(this.context, "a", "b");
		EntityScanPackages.register(this.context, "b", "c");
		this.context.refresh();
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly("a", "b", "c");
	}

	@Test
	public void registerFromArrayWhenRegistryIsNullShouldThrowException()
			throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Registry must not be null");
		EntityScanPackages.register(null);

	}

	@Test
	public void registerFromArrayWhenPackageNamesIsNullShouldThrowException()
			throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("PackageNames must not be null");
		EntityScanPackages.register(this.context, (String[]) null);
	}

	@Test
	public void registerFromCollectionWhenRegistryIsNullShouldThrowException()
			throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Registry must not be null");
		EntityScanPackages.register(null, Collections.<String>emptyList());
	}

	@Test
	public void registerFromCollectionWhenPackageNamesIsNullShouldThrowException()
			throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("PackageNames must not be null");
		EntityScanPackages.register(this.context, (Collection<String>) null);
	}

	@Test
	public void entityScanAnnotationWhenHasValueAttributeShouldSetupPackages()
			throws Exception {
		this.context = new AnnotationConfigApplicationContext(
				EntityScanValueConfig.class);
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly("a");
	}

	@Test
	public void entityScanAnnotationWhenHasValueAttributeShouldSetupPackagesAsm()
			throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.registerBeanDefinition("entityScanValueConfig",
				new RootBeanDefinition(EntityScanValueConfig.class.getName()));
		this.context.refresh();
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly("a");
	}

	@Test
	public void entityScanAnnotationWhenHasBasePackagesAttributeShouldSetupPackages()
			throws Exception {
		this.context = new AnnotationConfigApplicationContext(
				EntityScanBasePackagesConfig.class);
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly("b");
	}

	@Test
	public void entityScanAnnotationWhenHasValueAndBasePackagesAttributeShouldThrow()
			throws Exception {
		this.thrown.expect(AnnotationConfigurationException.class);
		this.context = new AnnotationConfigApplicationContext(
				EntityScanValueAndBasePackagesConfig.class);
	}

	@Test
	public void entityScanAnnotationWhenHasBasePackageClassesAttributeShouldSetupPackages()
			throws Exception {
		this.context = new AnnotationConfigApplicationContext(
				EntityScanBasePackageClassesConfig.class);
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages.getPackageNames())
				.containsExactly(getClass().getPackage().getName());
	}

	@Test
	public void entityScanAnnotationWhenNoAttributesShouldSetupPackages()
			throws Exception {
		this.context = new AnnotationConfigApplicationContext(
				EntityScanNoAttributesConfig.class);
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages.getPackageNames())
				.containsExactly(getClass().getPackage().getName());
	}

	@Test
	public void entityScanAnnotationWhenLoadingFromMultipleConfigsShouldCombinePackages()
			throws Exception {
		this.context = new AnnotationConfigApplicationContext(EntityScanValueConfig.class,
				EntityScanBasePackagesConfig.class);
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly("a", "b");
	}

	@Configuration
	@EntityScan("a")
	static class EntityScanValueConfig {

	}

	@Configuration
	@EntityScan(basePackages = "b")
	static class EntityScanBasePackagesConfig {

	}

	@Configuration
	@EntityScan(value = "a", basePackages = "b")
	static class EntityScanValueAndBasePackagesConfig {

	}

	@Configuration
	@EntityScan(basePackageClasses = EntityScanPackagesTests.class)
	static class EntityScanBasePackageClassesConfig {

	}

	@Configuration
	@EntityScan
	static class EntityScanNoAttributesConfig {

	}

}
