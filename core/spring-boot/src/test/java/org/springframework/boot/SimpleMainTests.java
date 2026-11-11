/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SpringApplication} main method.
 *
 * @author Dave Syer
 */
@Configuration(proxyBeanMethods = false)
@ExtendWith(OutputCaptureExtension.class)
class SimpleMainTests {

	private static final String SPRING_STARTUP = "Started SpringApplication in";

	@Test
	void emptyApplicationContext() {
		assertThatIllegalStateException().isThrownBy(() -> SpringApplication.main(getArgs()));
	}

	@Test
	void basePackageScan(CapturedOutput output) throws Exception {
		SpringApplication.main(getArgs(ClassUtils.getPackageName(getClass()) + ".sampleconfig"));
		assertThat(output).contains(SPRING_STARTUP);
	}

	@Test
	void configClassContext(CapturedOutput output) throws Exception {
		SpringApplication.main(getArgs(getClass().getName()));
		assertThat(output).contains(SPRING_STARTUP);
	}

	@Test
	@WithSampleBeansXmlResource
	void xmlContext(CapturedOutput output) throws Exception {
		SpringApplication.main(getArgs("org/springframework/boot/sample-beans.xml"));
		assertThat(output).contains(SPRING_STARTUP);
	}

	@Test
	@WithSampleBeansXmlResource
	void mixedContext(CapturedOutput output) throws Exception {
		SpringApplication.main(getArgs(getClass().getName(), "org/springframework/boot/sample-beans.xml"));
		assertThat(output).contains(SPRING_STARTUP);
	}

	private String[] getArgs(String... args) {
		List<String> list = new ArrayList<>(Arrays.asList("--spring.main.web-application-type=none",
				"--spring.main.show-banner=OFF", "--spring.main.register-shutdownHook=false"));
		if (args.length > 0) {
			list.add("--spring.main.sources=" + StringUtils.arrayToCommaDelimitedString(args));
		}
		return StringUtils.toStringArray(list);
	}

}
