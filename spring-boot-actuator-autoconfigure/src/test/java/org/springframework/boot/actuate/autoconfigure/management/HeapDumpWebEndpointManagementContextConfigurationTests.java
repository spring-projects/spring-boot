/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.management;

import org.junit.Test;

import org.springframework.boot.actuate.management.HeapDumpWebEndpoint;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HeapDumpWebEndpointManagementContextConfiguration}.
 *
 * @author Phillip Webb
 */
public class HeapDumpWebEndpointManagementContextConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withPropertyValues("endpoints.default.web.enabled:true")
			.withUserConfiguration(
					HeapDumpWebEndpointManagementContextConfiguration.class);

	@Test
	public void runShouldCreateIndicator() throws Exception {
		this.contextRunner.run((context) -> assertThat(context)
				.hasSingleBean(HeapDumpWebEndpoint.class));
	}

	@Test
	public void runWhenDisabledShouldNotCreateIndicator() throws Exception {
		this.contextRunner.withPropertyValues("endpoints.heapdump.enabled:false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(HeapDumpWebEndpoint.class));
	}

}
