/*
 * Copyright 2012-present the original author or authors.
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
