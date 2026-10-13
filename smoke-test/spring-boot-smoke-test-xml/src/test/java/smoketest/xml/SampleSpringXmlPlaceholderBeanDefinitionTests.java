/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.xml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import smoketest.xml.service.OtherService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for XML config with placeholders in bean definitions.
 *
 * @author Madhura Bhave
 */
@SpringBootTest(
		classes = { SampleSpringXmlApplication.class, SampleSpringXmlPlaceholderBeanDefinitionTests.TestConfig.class })
@ExtendWith(OutputCaptureExtension.class)
class SampleSpringXmlPlaceholderBeanDefinitionTests {

	@Test
	void beanWithPlaceholderShouldNotFail(CapturedOutput output) {
		assertThat(output).contains("Hello Other World");
	}

	@Configuration(proxyBeanMethods = false)
	@ImportResource({ "classpath:/META-INF/context.xml" })
	static class TestConfig {

		@Bean
		CommandLineRunner testCommandLineRunner(OtherService service) {
			return (args) -> System.out.println(service.getMessage());
		}

	}

}
