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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.Banner.Mode;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultSpringApplicationBannerPrinter}.
 *
 * @author Moritz Halbritter
 * @author Junhyung Park
 */
@ExtendWith(OutputCaptureExtension.class)
class DefaultSpringApplicationBannerPrinterTests {

	@Test
	void shouldUseUtf8(CapturedOutput capturedOutput) {
		ResourceLoader resourceLoader = new GenericApplicationContext();
		Resource resource = resourceLoader.getResource("classpath:/banner-utf8.txt");
		SpringApplicationBannerPrinter printer = new DefaultSpringApplicationBannerPrinter();
		printer.print(new MockEnvironment(), DefaultSpringApplicationBannerPrinterTests.class, Mode.LOG,
				new ResourceBanner(resource));
		assertThat(capturedOutput).containsIgnoringNewLines("\uD83D\uDE0D Spring Boot! \uD83D\uDE0D");
	}

}
