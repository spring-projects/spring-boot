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

	private static final Set<String> TOMCAT_EXPECTED_IN_WEB_INF_LIB_PROVIDED = new HashSet<String>(
			Arrays.asList(WEB_INF_LIB_PROVIDED_PREFIX + "spring-boot-starter-tomcat-",
					WEB_INF_LIB_PROVIDED_PREFIX + "tomcat-embed-core-",
					WEB_INF_LIB_PROVIDED_PREFIX + "tomcat-embed-el-",
					WEB_INF_LIB_PROVIDED_PREFIX + "tomcat-embed-logging-juli-"));

	private static final Set<String> JETTY_EXPECTED_IN_WEB_INF_LIB_PROVIDED = new HashSet<String>(
			Arrays.asList(WEB_INF_LIB_PROVIDED_PREFIX + "spring-boot-starter-jetty-",
					WEB_INF_LIB_PROVIDED_PREFIX + "jetty-util-",
					WEB_INF_LIB_PROVIDED_PREFIX + "jetty-xml-",
					WEB_INF_LIB_PROVIDED_PREFIX + "javax.servlet-",
					WEB_INF_LIB_PROVIDED_PREFIX + "jetty-continuation-",
					WEB_INF_LIB_PROVIDED_PREFIX + "jetty-io-",
					WEB_INF_LIB_PROVIDED_PREFIX + "jetty-http-",
					WEB_INF_LIB_PROVIDED_PREFIX + "jetty-server-",
					WEB_INF_LIB_PROVIDED_PREFIX + "jetty-security-",
					WEB_INF_LIB_PROVIDED_PREFIX + "jetty-servlet-",
					WEB_INF_LIB_PROVIDED_PREFIX + "jetty-webapp-",
					WEB_INF_LIB_PROVIDED_PREFIX + "javax.servlet.jsp-",
					WEB_INF_LIB_PROVIDED_PREFIX + "org.apache.jasper.glassfish-",
					WEB_INF_LIB_PROVIDED_PREFIX + "javax.servlet.jsp.jstl-",
					WEB_INF_LIB_PROVIDED_PREFIX
							+ "org.apache.taglibs.standard.glassfish-",
					WEB_INF_LIB_PROVIDED_PREFIX + "javax.el-",
					WEB_INF_LIB_PROVIDED_PREFIX + "com.sun.el-",
					WEB_INF_LIB_PROVIDED_PREFIX + "org.eclipse.jdt.core-",
					WEB_INF_LIB_PROVIDED_PREFIX + "jetty-jsp-"));

	private static final String BOOT_VERSION = ManagedDependencies.get()
			.find("spring-boot").getVersion();

	private static ProjectConnection project;

	@BeforeClass
	public static void createProject() throws IOException {
		project = new ProjectCreator().createProject("war-packaging");
	}

	@Test
	public void onlyTomcatIsPackackedInWebInfLibProvided() throws IOException {
		checkWebInfLibProvidedEntriesForServletContainer("tomcat",
				TOMCAT_EXPECTED_IN_WEB_INF_LIB_PROVIDED);
	}

	@Test
	public void onlyJettyIsPackackedInWebInfLibProvided() throws IOException {
		checkWebInfLibProvidedEntriesForServletContainer("jetty",
				JETTY_EXPECTED_IN_WEB_INF_LIB_PROVIDED);
	}

	private void checkWebInfLibProvidedEntriesForServletContainer(
			String servletContainer, Set<String> expectedEntries) throws IOException {
		project.newBuild()
				.forTasks("clean", "build")
				.withArguments("-PbootVersion=" + BOOT_VERSION,
						"-PservletContainer=" + servletContainer).run();

		JarFile war = new JarFile("target/war-packaging/build/libs/war-packaging.war");
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

	private boolean isWebInfLibProvidedEntry(String name) {
		return name.startsWith(WEB_INF_LIB_PROVIDED_PREFIX)
				&& !name.equals(WEB_INF_LIB_PROVIDED_PREFIX);
	}

	private boolean isExpectedInWebInfLibProvided(String name, Set<String> expectedEntries) {
		for (String expected : expectedEntries) {
			if (name.startsWith(expected)) {
				return true;
			}
		}
		return false;
	}
}
