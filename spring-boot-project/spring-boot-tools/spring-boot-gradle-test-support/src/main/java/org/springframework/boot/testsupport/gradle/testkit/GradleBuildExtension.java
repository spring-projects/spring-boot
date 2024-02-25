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

package org.springframework.boot.testsupport.gradle.testkit;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.regex.Pattern;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.util.ReflectionUtils;

/**
 * An {@link Extension} for managing the lifecycle of a {@link GradleBuild} stored in a
 * field named {@code gradleBuild}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
public class GradleBuildExtension implements BeforeEachCallback, AfterEachCallback {

	private static final Pattern GRADLE_VERSION_PATTERN = Pattern.compile("\\[Gradle .+\\]");

	private final Dsl dsl = Dsl.GROOVY;

	/**
	 * This method is executed before each test in the GradleBuildExtension class. It
	 * extracts the GradleBuild object from the ExtensionContext and sets the default
	 * script and settings if available. It then calls the before() method of the
	 * GradleBuild object.
	 * @param context the ExtensionContext object representing the current test context
	 * @throws Exception if an error occurs during the execution of the method
	 */
	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		GradleBuild gradleBuild = extractGradleBuild(context);
		URL scriptUrl = findDefaultScript(context);
		if (scriptUrl != null) {
			gradleBuild.script(scriptUrl.getFile());
		}
		URL settingsUrl = getSettings(context);
		if (settingsUrl != null) {
			gradleBuild.settings(settingsUrl.getFile());
		}
		gradleBuild.before();
	}

	/**
	 * Extracts the GradleBuild object from the given ExtensionContext.
	 * @param context the ExtensionContext from which to extract the GradleBuild object
	 * @return the extracted GradleBuild object
	 * @throws Exception if an error occurs during extraction
	 */
	private GradleBuild extractGradleBuild(ExtensionContext context) throws Exception {
		Object testInstance = context.getRequiredTestInstance();
		Field gradleBuildField = ReflectionUtils.findField(testInstance.getClass(), "gradleBuild");
		gradleBuildField.setAccessible(true);
		GradleBuild gradleBuild = (GradleBuild) gradleBuildField.get(testInstance);
		return gradleBuild;
	}

	/**
	 * Finds the default script URL for the given ExtensionContext.
	 * @param context the ExtensionContext to find the default script for
	 * @return the default script URL, or null if not found
	 */
	private URL findDefaultScript(ExtensionContext context) {
		URL scriptUrl = getScriptForTestMethod(context);
		if (scriptUrl != null) {
			return scriptUrl;
		}
		return getScriptForTestClass(context.getRequiredTestClass());
	}

	/**
	 * Returns the URL of the script file for the test method.
	 * @param context the ExtensionContext object representing the current test context
	 * @return the URL of the script file
	 */
	private URL getScriptForTestMethod(ExtensionContext context) {
		Class<?> testClass = context.getRequiredTestClass();
		String name = testClass.getSimpleName() + "-" + removeGradleVersion(context.getRequiredTestMethod().getName())
				+ this.dsl.getExtension();
		return testClass.getResource(name);
	}

	/**
	 * Removes the Gradle version from the given method name.
	 * @param methodName the method name to remove Gradle version from
	 * @return the method name without the Gradle version
	 */
	private String removeGradleVersion(String methodName) {
		return GRADLE_VERSION_PATTERN.matcher(methodName).replaceAll("").trim();
	}

	/**
	 * Returns the URL of the script file for the given test class.
	 * @param testClass the test class for which to retrieve the script file
	 * @return the URL of the script file
	 */
	private URL getScriptForTestClass(Class<?> testClass) {
		return testClass.getResource(testClass.getSimpleName() + this.dsl.getExtension());
	}

	/**
	 * Retrieves the URL of the settings.gradle file for the given ExtensionContext.
	 * @param context the ExtensionContext representing the current test context
	 * @return the URL of the settings.gradle file
	 */
	private URL getSettings(ExtensionContext context) {
		Class<?> testClass = context.getRequiredTestClass();
		return testClass.getResource("settings.gradle");
	}

	/**
	 * This method is called after each test execution. It extracts the Gradle build from
	 * the given context and calls the 'after' method on it.
	 * @param context the extension context
	 * @throws Exception if an error occurs during the 'after' method execution
	 */
	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		extractGradleBuild(context).after();
	}

}
