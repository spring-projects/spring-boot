/*
 * Copyright 2012-2019 the original author or authors.
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
	void beanWithPlaceholderShouldNotFail(CapturedOutput output) throws Exception {
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
