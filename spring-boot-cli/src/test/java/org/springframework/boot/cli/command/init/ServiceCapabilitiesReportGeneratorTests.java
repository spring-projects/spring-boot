/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.cli.command.init;

import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link ServiceCapabilitiesReportGenerator}
 *
 * @author Stephane Nicoll
 */
public class ServiceCapabilitiesReportGeneratorTests extends AbstractHttpClientMockTests {

	private final ServiceCapabilitiesReportGenerator command = new ServiceCapabilitiesReportGenerator(
			new InitializrService(this.http));

	@Test
	public void listMetadata() throws IOException {
		mockSuccessfulMetadataGet();
		String content = this.command.generate("http://localhost");
		assertTrue(content.contains("aop - AOP"));
		assertTrue(content.contains("security - Security: Security description"));
		assertTrue(content.contains("type: maven-project"));
		assertTrue(content.contains("packaging: jar"));
	}

}
