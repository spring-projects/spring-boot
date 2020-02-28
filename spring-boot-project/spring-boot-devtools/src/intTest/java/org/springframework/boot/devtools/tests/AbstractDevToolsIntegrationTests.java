/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.devtools.tests;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FixedValue;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.testsupport.BuildOutput;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Base class for DevTools integration tests.
 *
 * @author Andy Wilkinson
 */
abstract class AbstractDevToolsIntegrationTests {

	protected static final BuildOutput buildOutput = new BuildOutput(AbstractDevToolsIntegrationTests.class);

	protected final File serverPortFile = new File(buildOutput.getRootLocation(), "server.port");

	@RegisterExtension
	protected final JvmLauncher javaLauncher = new JvmLauncher();

	@TempDir
	protected static File temp;

	protected LaunchedApplication launchedApplication;

	protected void launchApplication(ApplicationLauncher applicationLauncher, String... args) throws Exception {
		this.serverPortFile.delete();
		this.launchedApplication = applicationLauncher.launchApplication(this.javaLauncher, this.serverPortFile, args);
	}

	@AfterEach
	void stopApplication() throws InterruptedException {
		this.launchedApplication.stop();
	}

	protected int awaitServerPort() throws Exception {
		int port = Awaitility.waitAtMost(Duration.ofSeconds(30))
				.until(() -> new ApplicationState(this.serverPortFile, this.launchedApplication),
						ApplicationState::hasServerPort)
				.getServerPort();
		this.serverPortFile.delete();
		this.launchedApplication.restartRemote(port);
		Thread.sleep(1000);
		return port;
	}

	protected ControllerBuilder controller(String name) {
		return new ControllerBuilder(name, this.launchedApplication.getClassesDirectory());
	}

	protected static final class ControllerBuilder {

		private final List<String> mappings = new ArrayList<>();

		private final String name;

		private final File classesDirectory;

		protected ControllerBuilder(String name, File classesDirectory) {
			this.name = name;
			this.classesDirectory = classesDirectory;
		}

		protected ControllerBuilder withRequestMapping(String mapping) {
			this.mappings.add(mapping);
			return this;
		}

		protected void build() throws Exception {
			DynamicType.Builder<Object> builder = new ByteBuddy().subclass(Object.class).name(this.name)
					.annotateType(AnnotationDescription.Builder.ofType(RestController.class).build());
			for (String mapping : this.mappings) {
				builder = builder.defineMethod(mapping, String.class, Visibility.PUBLIC)
						.intercept(FixedValue.value(mapping)).annotateMethod(AnnotationDescription.Builder
								.ofType(RequestMapping.class).defineArray("value", mapping).build());
			}
			builder.make().saveIn(this.classesDirectory);
		}

	}

}
