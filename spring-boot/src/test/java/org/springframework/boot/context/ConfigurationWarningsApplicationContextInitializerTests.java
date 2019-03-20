/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.context;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.ConfigurationWarningsApplicationContextInitializer.ComponentScanPackageCheck;
import org.springframework.boot.context.configwarnings.dflt.InDefaultPackageConfiguration;
import org.springframework.boot.context.configwarnings.dflt.InDefaultPackageWithBasePackageClassesConfiguration;
import org.springframework.boot.context.configwarnings.dflt.InDefaultPackageWithBasePackagesConfiguration;
import org.springframework.boot.context.configwarnings.dflt.InDefaultPackageWithMetaAnnotationConfiguration;
import org.springframework.boot.context.configwarnings.dflt.InDefaultPackageWithValueConfiguration;
import org.springframework.boot.context.configwarnings.dflt.InDefaultPackageWithoutScanConfiguration;
import org.springframework.boot.context.configwarnings.orgspring.InOrgSpringPackageConfiguration;
import org.springframework.boot.context.configwarnings.real.InRealButScanningProblemPackages;
import org.springframework.boot.context.configwarnings.real.InRealPackageConfiguration;
import org.springframework.boot.testutil.InternalOutputCapture;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationWarningsApplicationContextInitializer}.
 *
 * @author Phillip Webb
 */
public class ConfigurationWarningsApplicationContextInitializerTests {

	private static final String DEFAULT_SCAN_WARNING = "Your ApplicationContext is unlikely to "
			+ "start due to a @ComponentScan of the default package.";

	private static final String ORGSPRING_SCAN_WARNING = "Your ApplicationContext is unlikely to "
			+ "start due to a @ComponentScan of 'org.springframework'.";

	@Rule
	public InternalOutputCapture output = new InternalOutputCapture();

	@Test
	public void logWarningInDefaultPackage() {
		load(InDefaultPackageConfiguration.class);
		assertThat(this.output.toString()).contains(DEFAULT_SCAN_WARNING);
	}

	@Test
	public void logWarningInDefaultPackageAndMetaAnnotation() {
		load(InDefaultPackageWithMetaAnnotationConfiguration.class);
		assertThat(this.output.toString()).contains(DEFAULT_SCAN_WARNING);
	}

	@Test
	public void noLogIfInRealPackage() throws Exception {
		load(InRealPackageConfiguration.class);
		assertThat(this.output.toString()).doesNotContain(DEFAULT_SCAN_WARNING);
	}

	@Test
	public void noLogWithoutComponentScanAnnotation() throws Exception {
		load(InDefaultPackageWithoutScanConfiguration.class);
		assertThat(this.output.toString()).doesNotContain(DEFAULT_SCAN_WARNING);
	}

	@Test
	public void noLogIfHasValue() throws Exception {
		load(InDefaultPackageWithValueConfiguration.class);
		assertThat(this.output.toString()).doesNotContain(DEFAULT_SCAN_WARNING);
	}

	@Test
	public void noLogIfHasBasePackages() throws Exception {
		load(InDefaultPackageWithBasePackagesConfiguration.class);
		assertThat(this.output.toString()).doesNotContain(DEFAULT_SCAN_WARNING);
	}

	@Test
	public void noLogIfHasBasePackageClasses() throws Exception {
		load(InDefaultPackageWithBasePackageClassesConfiguration.class);
		assertThat(this.output.toString()).doesNotContain(DEFAULT_SCAN_WARNING);
	}

	@Test
	public void logWarningInOrgSpringPackage() {
		load(InOrgSpringPackageConfiguration.class);
		assertThat(this.output.toString()).contains(ORGSPRING_SCAN_WARNING);
	}

	@Test
	public void logWarningIfScanningProblemPackages() throws Exception {
		load(InRealButScanningProblemPackages.class);
		assertThat(this.output.toString())
				.contains("Your ApplicationContext is unlikely to start due to a "
						+ "@ComponentScan of the default package, 'org.springframework'.");

	}

	private void load(Class<?> configClass) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		new TestConfigurationWarningsApplicationContextInitializer().initialize(context);
		context.register(configClass);
		try {
			context.refresh();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		finally {
			context.close();
		}
	}

	/**
	 * Testable version of {@link ConfigurationWarningsApplicationContextInitializer}.
	 */
	public static class TestConfigurationWarningsApplicationContextInitializer
			extends ConfigurationWarningsApplicationContextInitializer {

		@Override
		protected Check[] getChecks() {
			return new Check[] { new TestComponentScanPackageCheck() };
		}

	}

	/**
	 * Testable ComponentScanPackageCheck that doesn't need to use the default or
	 * {@code org.springframework} package.
	 */
	static class TestComponentScanPackageCheck extends ComponentScanPackageCheck {

		@Override
		protected Set<String> getComponentScanningPackages(
				BeanDefinitionRegistry registry) {
			Set<String> scannedPackages = super.getComponentScanningPackages(registry);
			Set<String> result = new LinkedHashSet<String>();
			for (String scannedPackage : scannedPackages) {
				if (scannedPackage.endsWith("dflt")) {
					result.add("");
				}
				else if (scannedPackage.endsWith("orgspring")) {
					result.add("org.springframework");
				}
				else {
					result.add(scannedPackage);
				}
			}
			return result;
		}

	}

}
