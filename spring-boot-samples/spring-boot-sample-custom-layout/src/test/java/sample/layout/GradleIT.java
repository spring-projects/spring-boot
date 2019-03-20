/*
 * Copyright 2012-2017 the original author or authors.
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

package sample.layout;

import java.io.File;
import java.io.FileReader;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.junit.Test;
import org.xml.sax.InputSource;

import org.springframework.util.FileCopyUtils;

public class GradleIT {

	@Test
	public void sampleDefault() throws Exception {
		test("default", "sample");
	}

	@Test
	public void sampleCustom() throws Exception {
		test("custom", "custom");
	}

	private void test(String name, String expected) throws Exception {
		File projectDirectory = new File("target/gradleit/" + name);
		File javaDirectory = new File(
				"target/gradleit/" + name + "/src/main/java/org/test/");
		projectDirectory.mkdirs();
		javaDirectory.mkdirs();
		File script = new File(projectDirectory, "build.gradle");
		FileCopyUtils.copy(new File("src/it/" + name + "/build.gradle"), script);
		FileCopyUtils.copy(
				new File("src/it/" + name
						+ "/src/main/java/org/test/SampleApplication.java"),
				new File(javaDirectory, "SampleApplication.java"));
		GradleConnector gradleConnector = GradleConnector.newConnector();
		gradleConnector.useGradleVersion("2.9");
		((DefaultGradleConnector) gradleConnector).embedded(true);
		ProjectConnection project = gradleConnector.forProjectDirectory(projectDirectory)
				.connect();
		project.newBuild().forTasks("clean", "build").setStandardOutput(System.out)
				.setStandardError(System.err)
				.withArguments("-PbootVersion=" + getBootVersion()).run();
		Verify.verify(
				new File("target/gradleit/" + name + "/build/libs/" + name + ".jar"),
				expected);
	}

	public static String getBootVersion() {
		return evaluateExpression(
				"/*[local-name()='project']/*[local-name()='parent']/*[local-name()='version']"
						+ "/text()");
	}

	private static String evaluateExpression(String expression) {
		try {
			XPathFactory xPathFactory = XPathFactory.newInstance();
			XPath xpath = xPathFactory.newXPath();
			XPathExpression expr = xpath.compile(expression);
			String version = expr.evaluate(new InputSource(new FileReader("pom.xml")));
			return version;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to evaluate expression", ex);
		}
	}

}
