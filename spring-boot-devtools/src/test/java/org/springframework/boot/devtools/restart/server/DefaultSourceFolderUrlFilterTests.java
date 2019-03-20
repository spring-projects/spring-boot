/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.devtools.restart.server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultSourceFolderUrlFilter}.
 *
 * @author Phillip Webb
 */
public class DefaultSourceFolderUrlFilterTests {

	private static final String SOURCE_ROOT = "/Users/me/code/some-root/";

	private static final List<String> COMMON_POSTFIXES;

	static {
		List<String> postfixes = new ArrayList<String>();
		postfixes.add(".jar");
		postfixes.add("-1.3.0.jar");
		postfixes.add("-1.3.0-SNAPSHOT.jar");
		postfixes.add("-1.3.0.BUILD-SNAPSHOT.jar");
		postfixes.add("-1.3.0.M1.jar");
		postfixes.add("-1.3.0.RC1.jar");
		postfixes.add("-1.3.0.RELEASE.jar");
		postfixes.add("-1.3.0.Final.jar");
		postfixes.add("-1.3.0.GA.jar");
		postfixes.add("-1.3.0.0.0.0.jar");
		COMMON_POSTFIXES = Collections.unmodifiableList(postfixes);
	}

	private DefaultSourceFolderUrlFilter filter = new DefaultSourceFolderUrlFilter();

	@Test
	public void mavenSourceFolder() throws Exception {
		doTest("my-module/target/classes/");
	}

	@Test
	public void gradleEclipseSourceFolder() throws Exception {
		doTest("my-module/bin/");
	}

	@Test
	public void unusualSourceFolder() throws Exception {
		doTest("my-module/something/quite/quite/mad/");
	}

	@Test
	public void skippedProjects() throws Exception {
		String sourceFolder = "/Users/me/code/spring-boot-samples/"
				+ "spring-boot-sample-devtools";
		URL jarUrl = new URL("jar:file:/Users/me/tmp/"
				+ "spring-boot-sample-devtools-1.3.0.BUILD-SNAPSHOT.jar!/");
		assertThat(this.filter.isMatch(sourceFolder, jarUrl)).isTrue();
		URL nestedJarUrl = new URL("jar:file:/Users/me/tmp/"
				+ "spring-boot-sample-devtools-1.3.0.BUILD-SNAPSHOT.jar!/"
				+ "lib/spring-boot-1.3.0.BUILD-SNAPSHOT.jar!/");
		assertThat(this.filter.isMatch(sourceFolder, nestedJarUrl)).isFalse();
		URL fileUrl = new URL("file:/Users/me/tmp/"
				+ "spring-boot-sample-devtools-1.3.0.BUILD-SNAPSHOT.jar");
		assertThat(this.filter.isMatch(sourceFolder, fileUrl)).isTrue();
	}

	private void doTest(String sourcePostfix) throws MalformedURLException {
		doTest(sourcePostfix, "my-module", true);
		doTest(sourcePostfix, "my-module-other", false);
		doTest(sourcePostfix, "my-module-other-again", false);
		doTest(sourcePostfix, "my-module.other", false);
	}

	private void doTest(String sourcePostfix, String moduleRoot, boolean expected)
			throws MalformedURLException {
		String sourceFolder = SOURCE_ROOT + sourcePostfix;
		for (String postfix : COMMON_POSTFIXES) {
			for (URL url : getUrls(moduleRoot + postfix)) {
				boolean match = this.filter.isMatch(sourceFolder, url);
				assertThat(match).as(url + " against " + sourceFolder)
						.isEqualTo(expected);
			}
		}
	}

	private List<URL> getUrls(String name) throws MalformedURLException {
		List<URL> urls = new ArrayList<URL>();
		urls.add(new URL("file:/some/path/" + name));
		urls.add(new URL("file:/some/path/" + name + "!/"));
		for (String postfix : COMMON_POSTFIXES) {
			urls.add(new URL(
					"jar:file:/some/path/lib-module" + postfix + "!/lib/" + name));
			urls.add(new URL(
					"jar:file:/some/path/lib-module" + postfix + "!/lib/" + name + "!/"));
		}
		return urls;
	}

}
