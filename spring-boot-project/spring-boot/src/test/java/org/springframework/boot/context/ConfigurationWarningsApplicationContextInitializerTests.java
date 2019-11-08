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

package org.springframework.boot.context;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationWarningsApplicationContextInitializer}.
 *
 * @author Phillip Webb
 */
@ExtendWith(OutputCaptureExtension.class)
class ConfigurationWarningsApplicationContextInitializerTests {

	private static final String DEFAULT_SCAN_WARNING = "Your ApplicationContext is unlikely to "
			+ "start due to a @ComponentScan of the default package.";

	private static final String ORGSPRING_SCAN_WARNING = "Your ApplicationContext is unlikely to "
			+ "start due to a @ComponentScan of 'org.springframework'.";

	@Test
	void logWarningInDefaultPackage(CapturedOutput output) {
		load(InDefaultPackageConfiguration.class);
		assertThat(output).contains(DEFAULT_SCAN_WARNING);
	}

	@Test
	void logWarningInDefaultPackageAndMetaAnnotation(CapturedOutput output) {
		load(InDefaultPackageWithMetaAnnotationConfiguration.class);
		assertThat(output).contains(DEFAULT_SCAN_WARNING);
	}

	@Test
	void noLogIfInRealPackage(CapturedOutput output) {
		load(InRealPackageConfiguration.class);
		assertThat(output).doesNotContain(DEFAULT_SCAN_WARNING);
	}

	@Test
	void noLogWithoutComponentScanAnnotation(CapturedOutput output) {
		load(InDefaultPackageWithoutScanConfiguration.class);
		assertThat(output).doesNotContain(DEFAULT_SCAN_WARNING);
	}

	@Test
	void noLogIfHasValue(CapturedOutput output) {
		load(InDefaultPackageWithValueConfiguration.class);
		assertThat(output).doesNotContain(DEFAULT_SCAN_WARNING);
	}

	@Test
	void noLogIfHasBasePackages(CapturedOutput output) {
		load(InDefaultPackageWithBasePackagesConfiguration.class);
		assertThat(output).doesNotContain(DEFAULT_SCAN_WARNING);
	}

	@Test
	void noLogIfHasBasePackageClasses(CapturedOutput output) {
		load(InDefaultPackageWithBasePackageClassesConfiguration.class);
		assertThat(output).doesNotContain(DEFAULT_SCAN_WARNING);
	}

	@Test
	void logWarningInOrgSpringPackage(CapturedOutput output) {
		load(InOrgSpringPackageConfiguration.class);
		assertThat(output).contains(ORGSPRING_SCAN_WARNING);
	}

	@Test
	void logWarningIfScanningProblemPackages(CapturedOutput output) {
		load(InRealButScanningProblemPackages.class);
		assertThat(output).contains("Your ApplicationContext is unlikely to start due to a "
				+ "@ComponentScan of the default package, 'org.springframework'.");

	}

	private void load(Class<?> configClass) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			new TestConfigurationWarningsApplicationContextInitializer().initialize(context);
			context.register(configClass);
			context.refresh();
		}
	}

	/**
	 * Testable version of {@link ConfigurationWarningsApplicationContextInitializer}.
	 */
	static class TestConfigurationWarningsApplicationContextInitializer
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
		protected Set<String> getComponentScanningPackages(BeanDefinitionRegistry registry) {
			Set<String> scannedPackages = super.getComponentScanningPackages(registry);
			Set<String> result = new LinkedHashSet<>();
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
