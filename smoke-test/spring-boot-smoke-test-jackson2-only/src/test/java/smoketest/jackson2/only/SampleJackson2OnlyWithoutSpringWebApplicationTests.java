/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jackson2.only;

import java.lang.management.ManagementFactory;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Actuator running in {@link SampleJackson2OnlyApplication} with
 * {@code spring-web} on the classpath.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("spring-web-*")
@SpringBootTest(properties = "spring.jmx.enabled=true")
class SampleJackson2OnlyWithoutSpringWebApplicationTests {

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
