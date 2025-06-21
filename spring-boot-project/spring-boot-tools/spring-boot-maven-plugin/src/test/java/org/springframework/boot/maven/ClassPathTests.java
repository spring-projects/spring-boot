/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.maven;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ClassPath}.
 *
 * @author Dmytro Nosan
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ClassPathTests {

	@Test
	void argsWhenNoClassPathReturnsEmptyList() {
		assertThat(ClassPath.of(Collections.emptyList()).args(false)).isEmpty();
	}

	@Test
	void argsWhenSingleUrlOnWindowsUsesPath(@TempDir Path temp) throws Exception {
		Path path = temp.resolve("test.jar");
		ClassPath classPath = ClassPath.of(onWindows(), List.of(path.toUri().toURL()));
		assertThat(classPath.args(true)).containsExactly("-cp", path.toString());
	}

	@Test
	void argsWhenSingleUrlNotOnWindowsUsesPath(@TempDir Path temp) throws Exception {
		Path path = temp.resolve("test.jar");
		ClassPath classPath = ClassPath.of(onLinux(), List.of(path.toUri().toURL()));
		assertThat(classPath.args(true)).containsExactly("-cp", path.toString());
	}

	@Test
	void argsWhenMultipleUrlsOnWindowsAndAllowedUsesArgFile(@TempDir Path temp) throws Exception {
		Path path1 = temp.resolve("test1.jar");
		Path path2 = temp.resolve("test2.jar");
		ClassPath classPath = ClassPath.of(onWindows(), List.of(path1.toUri().toURL(), path2.toUri().toURL()));
		List<String> args = classPath.args(true);
		assertThat(args.get(0)).isEqualTo("-cp");
		assertThat(args.get(1)).startsWith("@");
		assertThat(Paths.get(args.get(1).substring(1)))
			.hasContent("\"" + (path1 + File.pathSeparator + path2).replace("\\", "\\\\") + "\"");
	}

	@Test
	void argsWhenMultipleUrlsOnWindowsAndNotAllowedUsesPath(@TempDir Path temp) throws Exception {
		Path path1 = temp.resolve("test1.jar");
		Path path2 = temp.resolve("test2.jar");
		ClassPath classPath = ClassPath.of(onWindows(), List.of(path1.toUri().toURL(), path2.toUri().toURL()));
		assertThat(classPath.args(false)).containsExactly("-cp", path1 + File.pathSeparator + path2);
	}

	@Test
	void argsWhenMultipleUrlsNotOnWindowsUsesPath(@TempDir Path temp) throws Exception {
		Path path1 = temp.resolve("test1.jar");
		Path path2 = temp.resolve("test2.jar");
		ClassPath classPath = ClassPath.of(onLinux(), List.of(path1.toUri().toURL(), path2.toUri().toURL()));
		assertThat(classPath.args(true)).containsExactly("-cp", path1 + File.pathSeparator + path2);
	}

	@Test
	void toStringShouldReturnClassPath(@TempDir Path temp) throws Exception {
		Path path1 = temp.resolve("test1.jar");
		Path path2 = temp.resolve("test2.jar");
		ClassPath classPath = ClassPath.of(onLinux(), List.of(path1.toUri().toURL(), path2.toUri().toURL()));
		assertThat(classPath.toString()).isEqualTo(path1 + File.pathSeparator + path2);
	}

	private UnaryOperator<String> onWindows() {
		return Map.of("os.name", "windows")::get;
	}

	private UnaryOperator<String> onLinux() {
		return Map.of("os.name", "linux")::get;
	}

}
