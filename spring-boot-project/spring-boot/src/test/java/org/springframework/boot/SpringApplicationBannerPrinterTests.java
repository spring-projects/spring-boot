/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot;

import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.BannerTests.Config;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringApplicationBannerPrinter}.
 *
 * @author Moritz Halbritter
 * @author Junhyung Park
 */
@ExtendWith(OutputCaptureExtension.class)
public class SpringApplicationBannerPrinterTests {

	private ConfigurableApplicationContext context;

	@AfterEach
	void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void shouldRegisterRuntimeHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new SpringApplicationBannerPrinter.SpringApplicationBannerPrinterRuntimeHints().registerHints(runtimeHints,
				getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.resource().forResource("banner.txt")).accepts(runtimeHints);
	}

	@Test
	void shouldPrintWithCustomPrinter(CapturedOutput capturedOutput) {
		SpringApplication application = createSpringApplicationWithBannerModeLog();
		this.context = application.run();
		assertThat(capturedOutput).contains(DefaultSpringApplicationBannerPrinter.class.getSimpleName());
	}

	@Test
	void shouldPrintWithDefaultPrinter(CapturedOutput capturedOutput) {
		SpringApplication application = createSpringApplicationWithBannerModeLog();
		application.setBannerPrinter(new CustomSpringApplicationBannerPrinter());
		this.context = application.run();
		assertThat(capturedOutput).doesNotContain(DefaultSpringApplicationBannerPrinter.class.getSimpleName());
	}

	private SpringApplication createSpringApplicationWithBannerModeLog() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setBannerMode(Mode.LOG);
		application.setBanner(new DummyBanner());
		application.setWebApplicationType(WebApplicationType.NONE);
		return application;
	}

	static class CustomSpringApplicationBannerPrinter implements SpringApplicationBannerPrinter {

		@Override
		public Banner print(Environment environment, Class<?> sourceClass, Mode bannerMode, Banner banner) {
			if (banner == null) {
				return null;
			}
			banner.printBanner(environment, sourceClass, System.out);
			return banner;
		}

	}

	static class DummyBanner implements Banner {

		@Override
		public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
			out.println("My Banner");
		}

	}

}
