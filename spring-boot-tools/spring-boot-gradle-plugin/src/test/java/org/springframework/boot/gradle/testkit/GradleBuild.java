/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.gradle.testkit;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.spring.gradle.dependencymanagement.DependencyManagementPlugin;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.springframework.boot.loader.tools.LaunchScript;

/**
 * A {@link TestRule} for running a Gradle build using {@link GradleRunner}.
 *
 * @author Andy Wilkinson
 */
public class GradleBuild implements TestRule {

	private final TemporaryFolder temp = new TemporaryFolder();

	private File projectDir;

	private String script;

	@Override
	public Statement apply(Statement base, Description description) {
		String name = description.getTestClass().getSimpleName() + ".gradle";
		URL scriptUrl = description.getTestClass().getResource(name);
		if (scriptUrl != null) {
			script(scriptUrl.getFile());
		}
		return this.temp.apply(new Statement() {

			@Override
			public void evaluate() throws Throwable {
				before();
				try {
					base.evaluate();
				}
				finally {
					after();
				}
			}

		}, description);
	}

	private void before() throws IOException {
		this.projectDir = this.temp.newFolder();
	}

	private void after() {
		GradleBuild.this.script = null;
	}

	private String pluginClasspath() {
		return new File("build/classes/main").getAbsolutePath() + ","
				+ new File("build/resources/main").getAbsolutePath() + ","
				+ LaunchScript.class.getProtectionDomain().getCodeSource().getLocation()
						.getPath()
				+ "," + DependencyManagementPlugin.class.getProtectionDomain()
						.getCodeSource().getLocation().getPath();
	}

	public GradleBuild script(String script) {
		this.script = script;
		return this;
	}

	public BuildResult build(String... arguments) {
		try {
			Files.copy(new File(this.script).toPath(),
					new File(this.projectDir, "build.gradle").toPath(),
					StandardCopyOption.REPLACE_EXISTING);
			GradleRunner gradleRunner = GradleRunner.create()
					.withProjectDir(this.projectDir).forwardOutput();
			List<String> allArguments = new ArrayList<String>();
			allArguments.add("-PpluginClasspath=" + pluginClasspath());
			allArguments.addAll(Arrays.asList(arguments));
			return gradleRunner.withArguments(allArguments).build();
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

}
