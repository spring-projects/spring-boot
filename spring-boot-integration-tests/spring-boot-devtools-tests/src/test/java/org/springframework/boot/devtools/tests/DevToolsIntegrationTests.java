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

package org.springframework.boot.devtools.tests;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.FixedValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DevTools.
 *
 * @author Andy Wilkinson
 */
@RunWith(Parameterized.class)
public class DevToolsIntegrationTests {

	private LaunchedApplication launchedApplication;

	private final File serverPortFile = new File("target/server.port");

	private final ApplicationLauncher applicationLauncher;

	@Rule
	public JvmLauncher javaLauncher = new JvmLauncher();

	@Parameters(name = "{0}")
	public static Object[] parameters() {
		return new Object[] { new Object[] { new LocalApplicationLauncher() },
				new Object[] { new ExplodedRemoteApplicationLauncher() },
				new Object[] { new JarFileRemoteApplicationLauncher() } };
	}

	public DevToolsIntegrationTests(ApplicationLauncher applicationLauncher) {
		this.applicationLauncher = applicationLauncher;
	}

	@Before
	public void launchApplication() throws Exception {
		this.serverPortFile.delete();
		this.launchedApplication = this.applicationLauncher
				.launchApplication(this.javaLauncher);
	}

	@After
	public void stopApplication() throws InterruptedException {
		this.launchedApplication.stop();
	}

	@Test
	public void addARequestMappingToAnExistingController() throws Exception {
		TestRestTemplate template = new TestRestTemplate();
		String urlBase = "http://localhost:" + awaitServerPort();
		assertThat(template.getForObject(urlBase + "/one", String.class))
				.isEqualTo("one");
		assertThat(template.getForEntity(urlBase + "/two", String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
		controller("com.example.ControllerOne").withRequestMapping("one")
				.withRequestMapping("two").build();
		assertThat(template.getForObject(urlBase + "/one", String.class))
				.isEqualTo("one");
		assertThat(template.getForObject("http://localhost:" + awaitServerPort() + "/two",
				String.class)).isEqualTo("two");
	}

	@Test
	public void removeARequestMappingFromAnExistingController() throws Exception {
		TestRestTemplate template = new TestRestTemplate();
		assertThat(template.getForObject("http://localhost:" + awaitServerPort() + "/one",
				String.class)).isEqualTo("one");
		controller("com.example.ControllerOne").build();
		assertThat(template.getForEntity("http://localhost:" + awaitServerPort() + "/one",
				String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void createAController() throws Exception {
		TestRestTemplate template = new TestRestTemplate();
		String urlBase = "http://localhost:" + awaitServerPort();
		assertThat(template.getForObject(urlBase + "/one", String.class))
				.isEqualTo("one");
		assertThat(template.getForEntity(urlBase + "/two", String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
		controller("com.example.ControllerTwo").withRequestMapping("two").build();
		assertThat(template.getForObject(urlBase + "/one", String.class))
				.isEqualTo("one");
		assertThat(template.getForObject("http://localhost:" + awaitServerPort() + "/two",
				String.class)).isEqualTo("two");

	}

	@Test
	public void deleteAController() throws Exception {
		TestRestTemplate template = new TestRestTemplate();
		assertThat(template.getForObject("http://localhost:" + awaitServerPort() + "/one",
				String.class)).isEqualTo("one");
		assertThat(new File(this.launchedApplication.getClassesDirectory(),
				"com/example/ControllerOne.class").delete()).isTrue();
		assertThat(template.getForEntity("http://localhost:" + awaitServerPort() + "/one",
				String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

	}

	private int awaitServerPort() throws Exception {
		long end = System.currentTimeMillis() + 30000;
		while (this.serverPortFile.length() == 0) {
			if (System.currentTimeMillis() > end) {
				throw new IllegalStateException(String.format(
						"server.port file was not written within 30 seconds. "
								+ "Application output:%n%s",
						FileCopyUtils.copyToString(new FileReader(
								this.launchedApplication.getStandardOut()))));
			}
			Thread.sleep(100);
		}
		FileReader portReader = new FileReader(this.serverPortFile);
		int port = Integer.valueOf(FileCopyUtils.copyToString(portReader));
		this.serverPortFile.delete();
		return port;
	}

	private ControllerBuilder controller(String name) {
		return new ControllerBuilder(name,
				this.launchedApplication.getClassesDirectory());
	}

	private static final class ControllerBuilder {

		private final List<String> mappings = new ArrayList<>();

		private final String name;

		private final File classesDirectory;

		private ControllerBuilder(String name, File classesDirectory) {
			this.name = name;
			this.classesDirectory = classesDirectory;
		}

		public ControllerBuilder withRequestMapping(String mapping) {
			this.mappings.add(mapping);
			return this;
		}

		public void build() throws Exception {
			Builder<Object> builder = new ByteBuddy().subclass(Object.class)
					.name(this.name).annotateType(AnnotationDescription.Builder
							.ofType(RestController.class).build());
			for (String mapping : this.mappings) {
				builder = builder.defineMethod(mapping, String.class, Visibility.PUBLIC)
						.intercept(FixedValue.value(mapping)).annotateMethod(
								AnnotationDescription.Builder.ofType(RequestMapping.class)
										.defineArray("value", mapping).build());
			}
			builder.make().saveIn(this.classesDirectory);
		}

	}

}
