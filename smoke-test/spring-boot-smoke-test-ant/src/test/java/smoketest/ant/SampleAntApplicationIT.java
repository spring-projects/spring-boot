/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.ant;

import java.io.File;
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
class SampleAntApplicationIT {

	@Test
	void runJar() throws Exception {
		File libs = new File("build/ant/libs");
		Process process = new JavaExecutable().processBuilder("-jar", "spring-boot-smoke-test-ant.jar")
			.directory(libs)
			.start();
		process.waitFor(5, TimeUnit.MINUTES);
		assertThat(process.exitValue()).isZero();
		String output = FileCopyUtils.copyToString(new InputStreamReader(process.getInputStream()));
		assertThat(output).contains("Spring Boot Ant Example");
	}

}
