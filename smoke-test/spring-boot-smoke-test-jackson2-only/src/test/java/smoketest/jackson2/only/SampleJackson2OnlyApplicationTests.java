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

package smoketest.jackson2.only;

import java.lang.management.ManagementFactory;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Actuator running in {@link SampleJackson2OnlyApplication}.
 *
 * @author Andy Wilkinson
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "spring.jmx.enabled=true")
@AutoConfigureRestTestClient
class SampleJackson2OnlyApplicationTests {

	@Autowired
	RestTestClient rest;

	@Test
	@SuppressWarnings("unchecked")
	void configPropsShouldReturnOk() {
		this.rest.get()
			.uri("/actuator/configprops")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(Map.class)
			.value((body) -> assertThat(body).containsOnlyKeys("contexts"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void actuatorLinksShouldReturnOk() {
		this.rest.get()
			.uri("/actuator")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(Map.class)
			.value((body) -> assertThat(body).containsOnlyKeys("_links"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void jmxEndpointsShouldWork() throws Exception {
		MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
		Map<String, Object> result = (Map<String, Object>) mbeanServer.invoke(
				ObjectName.getInstance("org.springframework.boot:type=Endpoint,name=Configprops"),
				"configurationProperties", new Object[0], null);
		assertThat(result).containsOnlyKeys("contexts");
	}

}
