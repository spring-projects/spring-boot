/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cli.command.init;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServiceCapabilitiesReportGenerator}
 *
 * @author Stephane Nicoll
 */
class ServiceCapabilitiesReportGeneratorTests extends AbstractHttpClientMockTests {

	private final ServiceCapabilitiesReportGenerator command = new ServiceCapabilitiesReportGenerator(
			new InitializrService(this.http));

	@Test
	void listMetadataFromServer() throws IOException {
		mockSuccessfulMetadataTextGet();
		String expected = new String(readClasspathResource("metadata/service-metadata-2.1.0.txt"));
		String content = this.command.generate("http://localhost");
		assertThat(content).isEqualTo(expected);
	}

	@Test
	void listMetadata() throws IOException {
		mockSuccessfulMetadataGet(true);
		doTestGenerateCapabilitiesFromJson();
	}

	@Test
	void listMetadataV2() throws IOException {
		mockSuccessfulMetadataGetV2(true);
		doTestGenerateCapabilitiesFromJson();
	}

	private void doTestGenerateCapabilitiesFromJson() throws IOException {
		String content = this.command.generate("http://localhost");
		assertThat(content).contains("aop - AOP");
		assertThat(content).contains("security - Security: Security description");
		assertThat(content).contains("type: maven-project");
		assertThat(content).contains("packaging: jar");
	}

}
