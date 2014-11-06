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

package org.springframework.boot.gradle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.gradle.tooling.ProjectConnection;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.dependency.tools.ManagedDependencies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for war packaging with Gradle to ensure that only the Servlet container and its
 * dependencies are packaged in WEB-INF/lib-provided
 *
 * @author Andy Wilkinson
 */
public class WarPackagingTests {

	private static final String WEB_INF_LIB_PROVIDED_PREFIX = "WEB-INF/lib-provided/";

	private static final String WEB_INF_LIB_PREFIX = "WEB-INF/lib/";

	private static final Set<String> TOMCAT_EXPECTED_IN_WEB_INF_LIB_PROVIDED = new HashSet<String>(
			Arrays.asList("spring-boot-starter-tomcat-", "tomcat-embed-core-",
					"tomcat-embed-el-", "tomcat-embed-logging-juli-"));

	private static final Set<String> JETTY_EXPECTED_IN_WEB_INF_LIB_PROVIDED = new HashSet<String>(
			Arrays.asList("spring-boot-starter-jetty-", "jetty-util-", "jetty-xml-",
					"jetty-schemas-", "javax.servlet-", "jetty-io-", "jetty-http-",
					"jetty-server-", "jetty-security-", "jetty-servlet-",
					"jetty-webapp-", "javax.servlet.jsp-2", "javax.servlet.jsp-api-",
					"javax.servlet.jsp.jstl-1.2.2", "javax.servlet.jsp.jstl-1.2.0",
					"javax.el-", "org.eclipse.jdt.core-", "jetty-jsp-"));

	private static final String BOOT_VERSION = ManagedDependencies.get()
			.find("spring-boot").getVersion();

	private static ProjectConnection project;

	@BeforeClass
	public static void createProject() throws IOException {
		project = new ProjectCreator().createProject("war-packaging");
	}

	@Test
	public void onlyTomcatIsPackackedInWebInfLibProvided() throws IOException {
		checkWebInfEntriesForServletContainer("tomcat",
				TOMCAT_EXPECTED_IN_WEB_INF_LIB_PROVIDED);
	}

	@Test
	public void onlyJettyIsPackackedInWebInfLibProvided() throws IOException {
		checkWebInfEntriesForServletContainer("jetty",
				JETTY_EXPECTED_IN_WEB_INF_LIB_PROVIDED);
	}

	private void checkWebInfEntriesForServletContainer(String servletContainer,
			Set<String> expectedLibProvidedEntries) throws IOException {
		project.newBuild()
				.forTasks("clean", "build")
				.withArguments("-PbootVersion=" + BOOT_VERSION,
						"-PservletContainer=" + servletContainer).run();

		JarFile war = new JarFile("target/war-packaging/build/libs/war-packaging.war");

		checkWebInfLibProvidedEntries(war, expectedLibProvidedEntries);

		checkWebInfLibEntries(war, expectedLibProvidedEntries);
	}

	private void checkWebInfLibProvidedEntries(JarFile war, Set<String> expectedEntries)
			throws IOException {
		Set<String> entries = getWebInfLibProvidedEntries(war);

		assertEquals(
				"Expected " + expectedEntries.size() + " but found " + entries.size()
						+ ": " + entries, expectedEntries.size(), entries.size());

		List<String> unexpectedLibProvidedEntries = new ArrayList<String>();
		for (String entry : entries) {
			if (!isExpectedInWebInfLibProvided(entry, expectedEntries)) {
				unexpectedLibProvidedEntries.add(entry);
			}
		}
		assertTrue("Found unexpected entries in WEB-INF/lib-provided: "
				+ unexpectedLibProvidedEntries, unexpectedLibProvidedEntries.isEmpty());
	}

	private void checkWebInfLibEntries(JarFile war, Set<String> entriesOnlyInLibProvided)
			throws IOException {
		Set<String> entries = getWebInfLibEntries(war);

		List<String> unexpectedLibEntries = new ArrayList<String>();
		for (String entry : entries) {
			if (!isExpectedInWebInfLib(entry, entriesOnlyInLibProvided)) {
				unexpectedLibEntries.add(entry);
			}
		}

		assertTrue("Found unexpected entries in WEB-INF/lib: " + unexpectedLibEntries,
				unexpectedLibEntries.isEmpty());
	}

	private Set<String> getWebInfLibProvidedEntries(JarFile war) throws IOException {
		Set<String> webInfLibProvidedEntries = new HashSet<String>();
		Enumeration<JarEntry> entries = war.entries();
		while (entries.hasMoreElements()) {
			String name = entries.nextElement().getName();
			if (isWebInfLibProvidedEntry(name)) {
				webInfLibProvidedEntries.add(name);
			}
		}
		return webInfLibProvidedEntries;
	}

	private Set<String> getWebInfLibEntries(JarFile war) throws IOException {
		Set<String> webInfLibEntries = new HashSet<String>();
		Enumeration<JarEntry> entries = war.entries();
		while (entries.hasMoreElements()) {
			String name = entries.nextElement().getName();
			if (isWebInfLibEntry(name)) {
				webInfLibEntries.add(name);
			}
		}
		return webInfLibEntries;
	}

	private boolean isWebInfLibProvidedEntry(String name) {
		return name.startsWith(WEB_INF_LIB_PROVIDED_PREFIX)
				&& !name.equals(WEB_INF_LIB_PROVIDED_PREFIX);
	}

	private boolean isWebInfLibEntry(String name) {
		return name.startsWith(WEB_INF_LIB_PREFIX) && !name.equals(WEB_INF_LIB_PREFIX);
	}

	private boolean isExpectedInWebInfLibProvided(String name, Set<String> expectedEntries) {
		for (String expected : expectedEntries) {
			if (name.startsWith(WEB_INF_LIB_PROVIDED_PREFIX + expected)) {
				return true;
			}
		}
		return false;
	}

	private boolean isExpectedInWebInfLib(String name, Set<String> unexpectedEntries) {
		for (String unexpected : unexpectedEntries) {
			if (name.startsWith(WEB_INF_LIB_PREFIX + unexpected)) {
				return false;
			}
		}
		return true;
	}
}
