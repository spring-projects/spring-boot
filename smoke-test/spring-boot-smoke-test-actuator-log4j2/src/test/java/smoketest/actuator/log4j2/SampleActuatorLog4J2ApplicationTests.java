/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator.log4j2;

import java.util.Base64;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

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
	private MockMvcTester mvc;

	@Test
	void testLogger(CapturedOutput output) {
		logger.info("Hello World");
		assertThat(output).contains("Hello World");
	}

	@Test
	void validateLoggersEndpoint() {
		assertThat(this.mvc.get()
			.uri("/actuator/loggers/org.apache.coyote.http11.Http11NioProtocol")
			.header("Authorization", getBasicAuth())).hasStatusOk()
			.hasBodyTextEqualTo("{\"configuredLevel\":\"WARN\",\"effectiveLevel\":\"WARN\"}");
	}

	private String getBasicAuth() {
		return "Basic " + Base64.getEncoder().encodeToString("user:password".getBytes());
	}

}
