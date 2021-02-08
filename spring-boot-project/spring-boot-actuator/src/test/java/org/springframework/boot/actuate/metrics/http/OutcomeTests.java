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

package org.springframework.boot.actuate.metrics.http;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Outcome}.
 *
 * @author Andy Wilkinson
 */
public class OutcomeTests {

	@Test
	void outcomeForInformationalStatusIsInformational() {
		for (int status = 100; status < 200; status++) {
			assertThat(Outcome.forStatus(status)).isEqualTo(Outcome.INFORMATIONAL);
		}
	}

	@Test
	void outcomeForSuccessStatusIsSuccess() {
		for (int status = 200; status < 300; status++) {
			assertThat(Outcome.forStatus(status)).isEqualTo(Outcome.SUCCESS);
		}
	}

	@Test
	void outcomeForRedirectionStatusIsRedirection() {
		for (int status = 300; status < 400; status++) {
			assertThat(Outcome.forStatus(status)).isEqualTo(Outcome.REDIRECTION);
		}
	}

	@Test
	void outcomeForClientErrorStatusIsClientError() {
		for (int status = 400; status < 500; status++) {
			assertThat(Outcome.forStatus(status)).isEqualTo(Outcome.CLIENT_ERROR);
		}
	}

	@Test
	void outcomeForServerErrorStatusIsServerError() {
		for (int status = 500; status < 600; status++) {
			assertThat(Outcome.forStatus(status)).isEqualTo(Outcome.SERVER_ERROR);
		}
	}

	@Test
	void outcomeForStatusBelowLowestKnownSeriesIsUnknown() {
		assertThat(Outcome.forStatus(99)).isEqualTo(Outcome.UNKNOWN);
	}

	@Test
	void outcomeForStatusAboveHighestKnownSeriesIsUnknown() {
		assertThat(Outcome.forStatus(600)).isEqualTo(Outcome.UNKNOWN);
	}

}
