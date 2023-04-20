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

package org.springframework.boot.devtools.classpath;

import java.io.File;

import org.junit.jupiter.api.Test;

import org.springframework.boot.devtools.filewatch.ChangedFile;
import org.springframework.boot.devtools.filewatch.ChangedFile.Type;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PatternClassPathRestartStrategy}.
 *
 * @author Phillip Webb
 * @author Andrew Landsverk
 */
class PatternClassPathRestartStrategyTests {

	@Test
	void nullPattern() {
		ClassPathRestartStrategy strategy = createStrategy(null);
		assertRestartRequired(strategy, "a/b.txt", true);
	}

	@Test
	void emptyPattern() {
		ClassPathRestartStrategy strategy = createStrategy("");
		assertRestartRequired(strategy, "a/b.txt", true);
	}

	@Test
	void singlePattern() {
		ClassPathRestartStrategy strategy = createStrategy("static/**");
		assertRestartRequired(strategy, "static/file.txt", false);
		assertRestartRequired(strategy, "static/directory/file.txt", false);
		assertRestartRequired(strategy, "public/file.txt", true);
		assertRestartRequired(strategy, "public/directory/file.txt", true);
	}

	@Test
	void multiplePatterns() {
		ClassPathRestartStrategy strategy = createStrategy("static/**,public/**");
		assertRestartRequired(strategy, "static/file.txt", false);
		assertRestartRequired(strategy, "static/directory/file.txt", false);
		assertRestartRequired(strategy, "public/file.txt", false);
		assertRestartRequired(strategy, "public/directory/file.txt", false);
		assertRestartRequired(strategy, "src/file.txt", true);
		assertRestartRequired(strategy, "src/directory/file.txt", true);
	}

	@Test
	void pomChange() {
		ClassPathRestartStrategy strategy = createStrategy("META-INF/maven/**");
		assertRestartRequired(strategy, "pom.xml", true);
		String mavenDirectory = "META-INF/maven/org.springframework.boot/spring-boot-devtools";
		assertRestartRequired(strategy, mavenDirectory + "/pom.xml", false);
		assertRestartRequired(strategy, mavenDirectory + "/pom.properties", false);
	}

	@Test
	void testChange() {
		ClassPathRestartStrategy strategy = createStrategy("**/*Test.class,**/*Tests.class");
		assertRestartRequired(strategy, "com/example/ExampleTests.class", false);
		assertRestartRequired(strategy, "com/example/ExampleTest.class", false);
		assertRestartRequired(strategy, "com/example/Example.class", true);
	}

	private ClassPathRestartStrategy createStrategy(String pattern) {
		return new PatternClassPathRestartStrategy(pattern);
	}

	private void assertRestartRequired(ClassPathRestartStrategy strategy, String relativeName, boolean expected) {
		assertThat(strategy.isRestartRequired(mockFile(relativeName))).isEqualTo(expected);
	}

	private ChangedFile mockFile(String relativeName) {
		return new ChangedFile(new File("."), new File("./" + relativeName), Type.ADD);
	}

}
