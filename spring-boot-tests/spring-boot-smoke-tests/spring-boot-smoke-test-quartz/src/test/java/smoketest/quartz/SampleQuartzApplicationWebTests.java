/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.MapAssert;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Web tests for {@link SampleQuartzApplication}.
 *
 * @author Stephane Nicoll
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class SampleQuartzApplicationWebTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void quartzGroupNames() {
		Map<String, Object> content = getContent("/actuator/quartz");
		assertThat(content).containsOnlyKeys("jobs", "triggers");
	}

	@Test
	void quartzJobGroups() {
		Map<String, Object> content = getContent("/actuator/quartz/jobs");
		assertThat(content).containsOnlyKeys("groups");
		assertThat(content).extractingByKey("groups", nestedMap()).containsOnlyKeys("samples");
	}

	@Test
	void quartzTriggerGroups() {
		Map<String, Object> content = getContent("/actuator/quartz/triggers");
		assertThat(content).containsOnlyKeys("groups");
		assertThat(content).extractingByKey("groups", nestedMap()).containsOnlyKeys("DEFAULT", "samples");
	}

	@Test
	void quartzJobDetail() {
		Map<String, Object> content = getContent("/actuator/quartz/jobs/samples/helloJob");
		assertThat(content).containsEntry("name", "helloJob").containsEntry("group", "samples");
	}

	@Test
	void quartzJobDetailWhenNameDoesNotExistReturns404() {
		ResponseEntity<String> response = this.restTemplate.getForEntity("/actuator/quartz/jobs/samples/does-not-exist",
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void quartzTriggerDetail() {
		Map<String, Object> content = getContent("/actuator/quartz/triggers/samples/3am-weekdays");
		assertThat(content).contains(entry("group", "samples"), entry("name", "3am-weekdays"), entry("state", "NORMAL"),
				entry("type", "cron"));
	}

	@Test
	void quartzTriggerDetailWhenNameDoesNotExistReturns404() {
		ResponseEntity<String> response = this.restTemplate
				.getForEntity("/actuator/quartz/triggers/samples/does-not-exist", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	private Map<String, Object> getContent(String path) {
		ResponseEntity<Map<String, Object>> entity = asMapEntity(this.restTemplate.getForEntity(path, Map.class));
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		return entity.getBody();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <K, V> ResponseEntity<Map<K, V>> asMapEntity(ResponseEntity<Map> entity) {
		return (ResponseEntity) entity;
	}

	@SuppressWarnings("rawtypes")
	private static InstanceOfAssertFactory<Map, MapAssert<String, Object>> nestedMap() {
		return InstanceOfAssertFactories.map(String.class, Object.class);
	}

}
