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

package org.springframework.boot.devtools.restart.server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultSourceDirectoryUrlFilter}.
 *
 * @author Phillip Webb
 */
class DefaultSourceDirectoryUrlFilterTests {

	private static final String SOURCE_ROOT = "/Users/me/code/some-root/";

	private static final List<String> COMMON_POSTFIXES;

	static {
		List<String> postfixes = new ArrayList<>();
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

	private final DefaultSourceDirectoryUrlFilter filter = new DefaultSourceDirectoryUrlFilter();

	@Test
	void mavenSourceDirectory() throws Exception {
		doTest("my-module/target/classes/");
	}

	@Test
	void gradleEclipseSourceDirectory() throws Exception {
		doTest("my-module/bin/");
	}

	@Test
	void unusualSourceDirectory() throws Exception {
		doTest("my-module/something/quite/quite/mad/");
	}

	private void doTest(String sourcePostfix) throws MalformedURLException {
		doTest(sourcePostfix, "my-module", true);
		doTest(sourcePostfix, "my-module-other", false);
		doTest(sourcePostfix, "my-module-other-again", false);
		doTest(sourcePostfix, "my-module.other", false);
	}

	private void doTest(String sourcePostfix, String moduleRoot, boolean expected) throws MalformedURLException {
		String sourceDirectory = SOURCE_ROOT + sourcePostfix;
		for (String postfix : COMMON_POSTFIXES) {
			for (URL url : getUrls(moduleRoot + postfix)) {
				boolean match = this.filter.isMatch(sourceDirectory, url);
				assertThat(match).as(url + " against " + sourceDirectory).isEqualTo(expected);
			}
		}
	}

	private List<URL> getUrls(String name) throws MalformedURLException {
		List<URL> urls = new ArrayList<>();
		urls.add(new URL("file:/some/path/" + name));
		urls.add(new URL("file:/some/path/" + name + "!/"));
		for (String postfix : COMMON_POSTFIXES) {
			urls.add(new URL("jar:file:/some/path/lib-module" + postfix + "!/lib/" + name));
			urls.add(new URL("jar:file:/some/path/lib-module" + postfix + "!/lib/" + name + "!/"));
		}
		return urls;
	}

}
