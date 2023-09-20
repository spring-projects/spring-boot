/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.boot.build.bom.bomr;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.build.bom.bomr.ReleaseSchedule.Release;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link ReleaseSchedule}.
 *
 * @author Andy Wilkinson
 */
public class ReleaseScheduleTests {

	private final RestTemplate rest = new RestTemplate();

	private final ReleaseSchedule releaseSchedule = new ReleaseSchedule(this.rest);

	private final MockRestServiceServer server = MockRestServiceServer.bindTo(this.rest).build();

	@Test
	void releasesBetween() {
		this.server
			.expect(requestTo("https://calendar.spring.io/releases?start=2023-09-01T00:00Z&end=2023-09-21T23:59Z"))
			.andRespond(withSuccess(new ClassPathResource("releases.json"), MediaType.APPLICATION_JSON));
		Map<String, List<Release>> releases = this.releaseSchedule
			.releasesBetween(OffsetDateTime.parse("2023-09-01T00:00Z"), OffsetDateTime.parse("2023-09-21T23:59Z"));
		assertThat(releases).hasSize(23);
		assertThat(releases.get("Spring Framework")).hasSize(3);
		assertThat(releases.get("Spring Boot")).hasSize(4);
		assertThat(releases.get("Spring Modulith")).hasSize(1);
		assertThat(releases.get("spring graphql")).hasSize(3);
	}

}
