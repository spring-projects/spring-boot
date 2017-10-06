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

package org.springframework.boot.system;

import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.boot.web.reactive.context.ReactiveWebServerInitializedEvent;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests {@link EmbeddedServerPortFileWriter}.
 *
 * @author David Liu
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@RunWith(Parameterized.class)
public class EmbeddedServerPortFileWriterTests {

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Parameters(name = "{0}")
	public static Object[] parameters() {
		Map<String, BiFunction<String, Integer, WebServerInitializedEvent>> parameters = new LinkedHashMap<>();
		parameters.put("Servlet",
				EmbeddedServerPortFileWriterTests::servletEventParameter);
		parameters.put("Reactive",
				EmbeddedServerPortFileWriterTests::reactiveEventParameter);
		return parameters.entrySet().stream()
				.map((e) -> new Object[] { e.getKey(), e.getValue() }).toArray();
	}

	private static WebServerInitializedEvent servletEventParameter(String name,
			Integer port) {
		ServletWebServerApplicationContext applicationContext = mock(
				ServletWebServerApplicationContext.class);
		given(applicationContext.getNamespace()).willReturn(name);
		WebServer source = mock(WebServer.class);
		given(source.getPort()).willReturn(port);
		ServletWebServerInitializedEvent event = new ServletWebServerInitializedEvent(
				source, applicationContext);
		return event;
	}

	private static WebServerInitializedEvent reactiveEventParameter(String name,
			Integer port) {
		ReactiveWebServerApplicationContext applicationContext = mock(
				ReactiveWebServerApplicationContext.class);
		given(applicationContext.getNamespace()).willReturn(name);
		WebServer source = mock(WebServer.class);
		given(source.getPort()).willReturn(port);
		return new ReactiveWebServerInitializedEvent(source, applicationContext);
	}

	private final BiFunction<String, Integer, ? extends WebServerInitializedEvent> eventFactory;

	public EmbeddedServerPortFileWriterTests(String name,
			BiFunction<String, Integer, ? extends WebServerInitializedEvent> eventFactory) {
		this.eventFactory = eventFactory;
	}

	@Before
	@After
	public void reset() {
		System.clearProperty("PORTFILE");
	}

	@Test
	public void createPortFile() throws Exception {
		File file = this.temporaryFolder.newFile();
		EmbeddedServerPortFileWriter listener = new EmbeddedServerPortFileWriter(file);
		listener.onApplicationEvent(mockEvent("", 8080));
		assertThat(FileCopyUtils.copyToString(new FileReader(file))).isEqualTo("8080");
	}

	@Test
	public void overridePortFileWithDefault() throws Exception {
		System.setProperty("PORTFILE", this.temporaryFolder.newFile().getAbsolutePath());
		EmbeddedServerPortFileWriter listener = new EmbeddedServerPortFileWriter();
		listener.onApplicationEvent(mockEvent("", 8080));
		FileReader reader = new FileReader(System.getProperty("PORTFILE"));
		assertThat(FileCopyUtils.copyToString(reader)).isEqualTo("8080");
	}

	@Test
	public void overridePortFileWithExplicitFile() throws Exception {
		File file = this.temporaryFolder.newFile();
		System.setProperty("PORTFILE", this.temporaryFolder.newFile().getAbsolutePath());
		EmbeddedServerPortFileWriter listener = new EmbeddedServerPortFileWriter(file);
		listener.onApplicationEvent(mockEvent("", 8080));
		FileReader reader = new FileReader(System.getProperty("PORTFILE"));
		assertThat(FileCopyUtils.copyToString(reader)).isEqualTo("8080");
	}

	@Test
	public void createManagementPortFile() throws Exception {
		File file = this.temporaryFolder.newFile();
		EmbeddedServerPortFileWriter listener = new EmbeddedServerPortFileWriter(file);
		listener.onApplicationEvent(mockEvent("", 8080));
		listener.onApplicationEvent(mockEvent("management", 9090));
		assertThat(FileCopyUtils.copyToString(new FileReader(file))).isEqualTo("8080");
		String managementFile = file.getName();
		managementFile = managementFile.substring(0, managementFile.length()
				- StringUtils.getFilenameExtension(managementFile).length() - 1);
		managementFile = managementFile + "-management."
				+ StringUtils.getFilenameExtension(file.getName());
		FileReader reader = new FileReader(
				new File(file.getParentFile(), managementFile));
		assertThat(FileCopyUtils.copyToString(reader)).isEqualTo("9090");
		assertThat(collectFileNames(file.getParentFile())).contains(managementFile);
	}

	@Test
	public void createUpperCaseManagementPortFile() throws Exception {
		File file = this.temporaryFolder.newFile();
		file = new File(file.getParentFile(), file.getName().toUpperCase());
		EmbeddedServerPortFileWriter listener = new EmbeddedServerPortFileWriter(file);
		listener.onApplicationEvent(mockEvent("management", 9090));
		String managementFile = file.getName();
		managementFile = managementFile.substring(0, managementFile.length()
				- StringUtils.getFilenameExtension(managementFile).length() - 1);
		managementFile = managementFile + "-MANAGEMENT."
				+ StringUtils.getFilenameExtension(file.getName());
		FileReader reader = new FileReader(
				new File(file.getParentFile(), managementFile));
		assertThat(FileCopyUtils.copyToString(reader)).isEqualTo("9090");
		assertThat(collectFileNames(file.getParentFile())).contains(managementFile);
	}

	private WebServerInitializedEvent mockEvent(String name, int port) {
		return this.eventFactory.apply(name, port);
	}

	private Set<String> collectFileNames(File directory) {
		Set<String> names = new HashSet<>();
		if (directory.isDirectory()) {
			for (File file : directory.listFiles()) {
				names.add(file.getName());
			}
		}
		return names;
	}

}
