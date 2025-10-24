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
