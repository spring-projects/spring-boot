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

package smoketest.ant;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.boot.loader.tools.JavaExecutable;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for {@code SampleAntApplication}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
public class SampleAntApplicationIT {

	@Test
	void runJar() throws Exception {
		File target = new File("build/ant/libs");
		File[] jarFiles = target.listFiles(new FileFilter() {

			@Override
			public boolean accept(File file) {
				return file.getName().endsWith(".jar");
			}

		});
		assertThat(jarFiles).hasSize(1);
		Process process = new JavaExecutable().processBuilder("-jar", jarFiles[0].getName()).directory(target).start();
		process.waitFor(5, TimeUnit.MINUTES);
		assertThat(process.exitValue()).isEqualTo(0);
		String output = FileCopyUtils.copyToString(new InputStreamReader(process.getInputStream()));
		assertThat(output).contains("Spring Boot Ant Example");
	}

}
