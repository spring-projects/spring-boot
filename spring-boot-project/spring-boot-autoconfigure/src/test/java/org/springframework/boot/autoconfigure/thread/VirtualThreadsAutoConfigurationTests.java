/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.thread;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link VirtualThreadsAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
class VirtualThreadsAutoConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(VirtualThreadsAutoConfiguration.class));

	@Test
	void shouldBeRegisteredInAutoConfigurationImports() {
		assertThat(ImportCandidates.load(AutoConfiguration.class, null).getCandidates())
			.contains(VirtualThreadsAutoConfiguration.class.getName());
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void shouldSupplyBeans() {
		this.runner.withPropertyValues("spring.threads.virtual.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(VirtualThreads.class));
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void shouldNotSupplyBeansIfVirtualThreadsAreNotEnabled() {
		this.runner.withPropertyValues("spring.threads.virtual.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(VirtualThreads.class));
	}

}
