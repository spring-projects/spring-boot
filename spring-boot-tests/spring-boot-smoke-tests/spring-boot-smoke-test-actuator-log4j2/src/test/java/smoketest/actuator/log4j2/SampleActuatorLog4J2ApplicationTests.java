/*
 * Copyright 2012-2019 the original author or authors.
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

package smoketest.actuator.log4j2;

import java.util.Base64;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link SampleActuatorLog4J2Application}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class SampleActuatorLog4J2ApplicationTests {

	private static final Logger logger = LogManager.getLogger(SampleActuatorLog4J2ApplicationTests.class);

	@Autowired
	private MockMvc mvc;

	@Test
	void testLogger(CapturedOutput output) {
		logger.info("Hello World");
		assertThat(output).contains("Hello World");
	}

	@Test
	void validateLoggersEndpoint() throws Exception {
		this.mvc.perform(get("/actuator/loggers/org.apache.coyote.http11.Http11NioProtocol").header("Authorization",
				getBasicAuth())).andExpect(status().isOk())
				.andExpect(content().string("{\"configuredLevel\":\"WARN\",\"effectiveLevel\":\"WARN\"}"));
	}

	private String getBasicAuth() {
		return "Basic " + Base64.getEncoder().encodeToString("user:password".getBytes());
	}

}
