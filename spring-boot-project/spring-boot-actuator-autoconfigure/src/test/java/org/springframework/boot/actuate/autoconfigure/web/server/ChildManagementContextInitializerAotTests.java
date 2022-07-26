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

package org.springframework.boot.actuate.autoconfigure.web.server;

import java.net.URL;
import java.util.function.Consumer;

import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.aot.test.generator.compile.CompileWithTargetClassAccess;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;
import org.springframework.test.aot.generate.TestGenerationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AOT tests for {@link ChildManagementContextInitializer}.
 *
 * @author Phillip Webb
 */
@ExtendWith(OutputCaptureExtension.class)
class ChildManagementContextInitializerAotTests {

	@BeforeEach
	@AfterEach
	void reset() {
		ReflectionTestUtils.setField(TomcatURLStreamHandlerFactory.class, "instance", null);
		ReflectionTestUtils.setField(URL.class, "factory", null);
	}

	@Test
	@CompileWithTargetClassAccess
	@SuppressWarnings("unchecked")
	void aotContributedInitializerStartsManagementContext(CapturedOutput output) {
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
						.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class,
								ServletWebServerFactoryAutoConfiguration.class,
								ServletManagementContextAutoConfiguration.class, WebEndpointAutoConfiguration.class,
								EndpointAutoConfiguration.class));
		contextRunner.withPropertyValues("server.port=0", "management.server.port=0").prepare((context) -> {
			TestGenerationContext generationContext = new TestGenerationContext(TestTarget.class);
			ClassName className = new ApplicationContextAotGenerator().generateApplicationContext(
					(GenericApplicationContext) context.getSourceApplicationContext(), generationContext);
			generationContext.writeGeneratedContent();
			TestCompiler compiler = TestCompiler.forSystem();
			compiler.withFiles(generationContext.getGeneratedFiles()).compile((compiled) -> {
				ServletWebServerApplicationContext freshApplicationContext = new ServletWebServerApplicationContext();
				TestPropertyValues.of("server.port=0", "management.server.port=0").applyTo(freshApplicationContext);
				ApplicationContextInitializer<GenericApplicationContext> initializer = compiled
						.getInstance(ApplicationContextInitializer.class, className.toString());
				initializer.initialize(freshApplicationContext);
				assertThat(output).satisfies(numberOfOccurrences("Tomcat started on port", 0));
				freshApplicationContext.refresh();
				assertThat(output).satisfies(numberOfOccurrences("Tomcat started on port", 2));
			});
		});
	}

	private <T extends CharSequence> Consumer<T> numberOfOccurrences(String substring, int expectedCount) {
		return (charSequence) -> {
			int count = StringUtils.countOccurrencesOf(charSequence.toString(), substring);
			assertThat(count).isEqualTo(expectedCount);
		};
	}

	static class TestTarget {

	}

}
