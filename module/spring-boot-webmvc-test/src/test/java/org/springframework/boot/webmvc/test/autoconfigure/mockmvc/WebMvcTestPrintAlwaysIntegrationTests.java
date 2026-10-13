/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} default print output.
 *
 * @author Phillip Webb
 */
@WebMvcTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@ExtendWith(OutputCaptureExtension.class)
class WebMvcTestPrintAlwaysIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void shouldPrint(CapturedOutput output) {
		assertThat(this.mvc.get().uri("/one")).hasStatusOk().hasBodyTextEqualTo("one");
		assertThat(output).contains("Request URI = /one");
	}

}
