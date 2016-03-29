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

package org.springframework.boot.context.scan;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.context.scan.TestEntityScanRegistrar.TestFactoryBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestEntityScan}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class TestEntityScanTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testValue() throws Exception {
		this.context = new AnnotationConfigApplicationContext(ValueConfig.class);
		assertSetPackagesToScan("com.mycorp.entity");
	}

	@Test
	public void basePackages() throws Exception {
		this.context = new AnnotationConfigApplicationContext(BasePackagesConfig.class);
		assertSetPackagesToScan("com.mycorp.entity2");
	}

	@Test
	public void basePackageClasses() throws Exception {
		this.context = new AnnotationConfigApplicationContext(
				BasePackageClassesConfig.class);
		assertSetPackagesToScan(getClass().getPackage().getName());
	}

	@Test
	public void fromConfigurationClass() throws Exception {
		this.context = new AnnotationConfigApplicationContext(FromConfigConfig.class);
		assertSetPackagesToScan(getClass().getPackage().getName());
	}

	@Test
	public void valueAndBasePackagesThrows() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("@TestEntityScan basePackages and value "
				+ "attributes are mutually exclusive");
		this.context = new AnnotationConfigApplicationContext(ValueAndBasePackages.class);
	}

	@Test
	public void valueAndBasePackageClassesMerges() throws Exception {
		this.context = new AnnotationConfigApplicationContext(
				ValueAndBasePackageClasses.class);
		assertSetPackagesToScan("com.mycorp.entity", getClass().getPackage().getName());
	}

	@Test
	public void basePackageAndBasePackageClassesMerges() throws Exception {
		this.context = new AnnotationConfigApplicationContext(
				BasePackagesAndBasePackageClasses.class);
		assertSetPackagesToScan("com.mycorp.entity2", getClass().getPackage().getName());
	}

	@Test
	public void considersMultipleAnnotations() {
		this.context = new AnnotationConfigApplicationContext(MultiScanFirst.class,
				MultiScanSecond.class);
		assertSetPackagesToScan("foo", "bar");
	}

	private void assertSetPackagesToScan(String... expected) {
		String[] actual = this.context.getBean(TestFactoryBean.class).getPackagesToScan();
		assertThat(actual).isEqualTo(expected);
	}

	@Configuration
	static class BaseConfig {

		@Bean
		public TestFactoryBean testFactoryBean() {
			return new TestFactoryBean();
		}

	}

	@TestEntityScan("com.mycorp.entity")
	static class ValueConfig extends BaseConfig {

	}

	@TestEntityScan(basePackages = "com.mycorp.entity2")
	static class BasePackagesConfig extends BaseConfig {

	}

	@TestEntityScan(basePackageClasses = TestEntityScanTests.class)
	static class BasePackageClassesConfig extends BaseConfig {

	}

	@TestEntityScan
	static class FromConfigConfig extends BaseConfig {

	}

	@TestEntityScan(value = "com.mycorp.entity", basePackages = "com.mycorp")
	static class ValueAndBasePackages extends BaseConfig {

	}

	@TestEntityScan(value = "com.mycorp.entity", basePackageClasses = TestEntityScanTests.class)
	static class ValueAndBasePackageClasses extends BaseConfig {

	}

	@TestEntityScan(basePackages = "com.mycorp.entity2", basePackageClasses = TestEntityScanTests.class)
	static class BasePackagesAndBasePackageClasses extends BaseConfig {

	}

	@TestEntityScan(basePackages = "foo")
	static class MultiScanFirst extends BaseConfig {

	}

	@TestEntityScan(basePackages = "bar")
	static class MultiScanSecond extends BaseConfig {

	}

}
