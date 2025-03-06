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

package org.springframework.boot.autoconfigure.netty;

import io.netty.util.ResourceLeakDetector;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NettyAutoConfiguration}.
 *
 * @author Brian Clozel
 */
class NettyAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(NettyAutoConfiguration.class));

	@Test
	void leakDetectionShouldBeConfigured() {
		this.contextRunner.withPropertyValues("spring.netty.leak-detection=paranoid").run((context) -> {
			assertThat(ResourceLeakDetector.getLevel()).isEqualTo(ResourceLeakDetector.Level.PARANOID);
			// reset configuration for the following tests.
			ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
		});
	}

}
