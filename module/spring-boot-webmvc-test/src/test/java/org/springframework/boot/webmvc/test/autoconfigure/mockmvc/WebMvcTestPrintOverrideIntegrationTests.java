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
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} when a specific print option is defined.
 *
 * @author Phillip Webb
 */
@WebMvcTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ExtendWith(OutputCaptureExtension.class)
class WebMvcTestPrintOverrideIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void shouldNotPrint(CapturedOutput output) {
		assertThat(this.mvc.get().uri("/one")).hasStatusOk().hasBodyTextEqualTo("one");
		assertThat(output).doesNotContain("Request URI = /one");
	}

}
