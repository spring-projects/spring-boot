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

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SpringApplicationBannerPrinter}.
 *
 * @author Moritz Halbritter
 */
class SpringApplicationBannerPrinterTests {

	@Test
	void shouldRegisterRuntimeHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new SpringApplicationBannerPrinter.SpringApplicationBannerPrinterRuntimeHints().registerHints(runtimeHints,
				getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.resource().forResource("banner.txt")).accepts(runtimeHints);
	}

	@Test
	void shouldUseUtf8() {
		ResourceLoader resourceLoader = new GenericApplicationContext();
		Resource resource = resourceLoader.getResource("classpath:/banner-utf8.txt");
		SpringApplicationBannerPrinter printer = new SpringApplicationBannerPrinter(resourceLoader,
				new ResourceBanner(resource));
		Log log = mock(Log.class);
		printer.print(new MockEnvironment(), SpringApplicationBannerPrinterTests.class, log);
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		then(log).should().info(captor.capture());
		assertThat(captor.getValue()).isEqualToIgnoringNewLines("\uD83D\uDE0D Spring Boot! \uD83D\uDE0D");
	}

}
