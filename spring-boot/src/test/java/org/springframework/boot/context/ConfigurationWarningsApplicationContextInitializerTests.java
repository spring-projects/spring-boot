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

package org.springframework.boot.context;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.context.ConfigurationWarningsApplicationContextInitializer.ComponentScanDefaultPackageCheck;
import org.springframework.boot.context.configwarnings.InDefaultPackageConfiguration;
import org.springframework.boot.context.configwarnings.InDefaultPackageWithBasePackageClassesConfiguration;
import org.springframework.boot.context.configwarnings.InDefaultPackageWithBasePackagesConfiguration;
import org.springframework.boot.context.configwarnings.InDefaultPackageWithMetaAnnotationConfiguration;
import org.springframework.boot.context.configwarnings.InDefaultPackageWithValueConfiguration;
import org.springframework.boot.context.configwarnings.InDefaultPackageWithoutScanConfiguration;
import org.springframework.boot.context.configwarnings.InRealPackageConfiguration;
import org.springframework.boot.test.OutputCapture;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ConfigurationWarningsApplicationContextInitializer}.
 *
 * @author Phillip Webb
 */
public class ConfigurationWarningsApplicationContextInitializerTests {

	private static final String SCAN_WARNING = "Your ApplicationContext is unlikely to "
			+ "start due to a @ComponentScan of the default package";

	@Rule
	public OutputCapture output = new OutputCapture();

	@Test
	public void logWarningInDefaultPackage() {
		load(InDefaultPackageConfiguration.class);
		assertThat(this.output.toString(), containsString(SCAN_WARNING));
	}

	@Test
	public void logWarningInDefaultPackageAndMetaAnnotation() {
		load(InDefaultPackageWithMetaAnnotationConfiguration.class);
		assertThat(this.output.toString(), containsString(SCAN_WARNING));
	}

	@Test
	public void noLogIfInRealPackage() throws Exception {
		load(InRealPackageConfiguration.class);
		assertThat(this.output.toString(), not(containsString(SCAN_WARNING)));
	}

	@Test
	public void noLogWithoutComponetScanAnnotation() throws Exception {
		load(InDefaultPackageWithoutScanConfiguration.class);
		assertThat(this.output.toString(), not(containsString(SCAN_WARNING)));
	}

	@Test
	public void noLogIfHasValue() throws Exception {
		load(InDefaultPackageWithValueConfiguration.class);
		assertThat(this.output.toString(), not(containsString(SCAN_WARNING)));
	}

	@Test
	public void noLogIfHasBasePackages() throws Exception {
		load(InDefaultPackageWithBasePackagesConfiguration.class);
		assertThat(this.output.toString(), not(containsString(SCAN_WARNING)));
	}

	@Test
	public void noLogIfHasBasePackageClasses() throws Exception {
		load(InDefaultPackageWithBasePackageClassesConfiguration.class);
		assertThat(this.output.toString(), not(containsString(SCAN_WARNING)));
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
	public static class TestConfigurationWarningsApplicationContextInitializer extends
			ConfigurationWarningsApplicationContextInitializer {

		@Override
		protected Check[] getChecks() {
			return new Check[] { new TestComponentScanDefaultPackageCheck() };
		}

	}

	/**
	 * Testable ComponentScanDefaultPackageCheck that doesn't need to use the default
	 * package.
	 */
	static class TestComponentScanDefaultPackageCheck extends
			ComponentScanDefaultPackageCheck {

		@Override
		protected boolean isInDefaultPackage(String className) {
			return className.contains("InDefault");
		}

	}

}
