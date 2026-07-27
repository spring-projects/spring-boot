/*
 * Copyright 2012-present the original author or authors.
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

package smoketest.quartz;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.MapAssert;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.BodySpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.within;

/**
 * Web tests for {@link SampleQuartzApplication}.
 *
 * @author Stephane Nicoll
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
@AutoConfigureRestTestClient
class SampleQuartzApplicationWebTests {

	private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {
	};

	@Autowired
	private RestTestClient restTestClient;

	@Test
	void quartzGroupNames() {
		assertContent("/actuator/quartz").value((content) -> assertThat(content).containsOnlyKeys("jobs", "triggers"));
	}

	@Test
	void quartzJobGroups() {
		assertContent("/actuator/quartz/jobs").value((content) -> {
			assertThat(content).containsOnlyKeys("groups");
			assertThat(content).extractingByKey("groups", nestedMap()).containsOnlyKeys("samples");
		});
	}

	@Test
	void quartzTriggerGroups() {
		assertContent("/actuator/quartz/triggers").value((content) -> {
			assertThat(content).containsOnlyKeys("groups");
			assertThat(content).extractingByKey("groups", nestedMap()).containsOnlyKeys("DEFAULT", "samples");
		});
	}

	@Test
	void quartzJobDetail() {
		assertContent("/actuator/quartz/jobs/samples/helloJob").value(
				(content) -> assertThat(content).containsEntry("name", "helloJob").containsEntry("group", "samples"));
	}

	@Test
	void quartzJobDetailWhenNameDoesNotExistReturns404() {
		this.restTestClient.get()
			.uri("/actuator/quartz/jobs/samples/does-not-exist")
			.exchange()
			.expectStatus()
			.isNotFound();
	}

	@Test
	void quartzTriggerDetail() {
		assertContent("/actuator/quartz/triggers/samples/3am-weekdays")
			.value((content) -> assertThat(content).contains(entry("group", "samples"), entry("name", "3am-weekdays"),
					entry("state", "NORMAL"), entry("type", "cron")));
	}

	@Test
	void quartzTriggerDetailWhenNameDoesNotExistReturns404() {
		this.restTestClient.get()
			.uri("/actuator/quartz/triggers/samples/does-not-exist")
			.exchange()
			.expectStatus()
			.isNotFound();
	}

	@Test
	void quartzJobTriggeredManually(CapturedOutput output) {
		this.restTestClient.post()
			.uri("/actuator/quartz/jobs/samples/onDemandJob")
			.body(Map.of("state", "running"))
			.exchangeSuccessfully()
			.expectBody(MAP_TYPE)
			.value((content) -> {
				assertThat(content).contains(entry("group", "samples"), entry("name", "onDemandJob"),
						entry("className", SampleJob.class.getName()));
				assertThat(content).extractingByKey("triggerTime", InstanceOfAssertFactories.STRING)
					.satisfies((triggerTime) -> assertThat(Instant.parse(triggerTime)).isCloseTo(Instant.now(),
							within(10, ChronoUnit.SECONDS)));
			});
		Awaitility.await()
			.atMost(Duration.ofSeconds(30))
			.untilAsserted(() -> assertThat(output).contains("Hello On Demand Job"));
	}

	private BodySpec<Map<String, Object>, ?> assertContent(String path) {
		return this.restTestClient.get().uri(path).exchangeSuccessfully().expectBody(MAP_TYPE);
	}

	@SuppressWarnings("rawtypes")
	private static InstanceOfAssertFactory<Map, MapAssert<String, Object>> nestedMap() {
		return InstanceOfAssertFactories.map(String.class, Object.class);
	}

}
