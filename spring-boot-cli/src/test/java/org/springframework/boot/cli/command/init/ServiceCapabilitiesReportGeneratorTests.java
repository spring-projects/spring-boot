/*
 * Copyright 2012-2015 the original author or authors.
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ServiceCapabilitiesReportGenerator}
 *
 * @author Stephane Nicoll
 */
public class ServiceCapabilitiesReportGeneratorTests extends AbstractHttpClientMockTests {

	private final ServiceCapabilitiesReportGenerator command = new ServiceCapabilitiesReportGenerator(
			new InitializrService(this.http));

	@Test
	public void listMetadataFromServer() throws IOException {
		mockSuccessfulMetadataTextGet();
		String expected = new String(readClasspathResource("metadata/service-metadata-2.1.0.txt"));
		String content = this.command.generate("http://localhost");
		assertThat(content, equalTo(expected));
	}

	@Test
	public void listMetadata() throws IOException {
		mockSuccessfulMetadataGet(true);
		doTestGenerateCapabilitiesFromJson();
	}

	@Test
	public void listMetadataV2() throws IOException {
		mockSuccessfulMetadataGetV2(true);
		doTestGenerateCapabilitiesFromJson();
	}

	private void doTestGenerateCapabilitiesFromJson() throws IOException {
		String content = this.command.generate("http://localhost");
		assertThat(content, containsString("aop - AOP"));
		assertThat(content, containsString("security - Security: Security description"));
		assertThat(content, containsString("type: maven-project"));
		assertThat(content, containsString("packaging: jar"));
	}

}
