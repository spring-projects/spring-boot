/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hazelcast.autoconfigure;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HazelcastAutoConfiguration} with full classpath.
 *
 * @author Stephane Nicoll
 */
class HazelcastAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HazelcastAutoConfiguration.class));

	@Test
	@WithResource(name = "hazelcast.xml", content = """
			<hazelcast
				xsi:schemaLocation="http://www.hazelcast.com/schema/config hazelcast-config-5.0.xsd"
				xmlns="http://www.hazelcast.com/schema/config" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
				<instance-name>default-instance</instance-name>
				<map name="defaultCache" />
				<network>
					<join>
						<auto-detection enabled="false" />
						<multicast enabled="false" />
					</join>
				</network>
			</hazelcast>
			""")
	@WithResource(name = "hazelcast.yml", content = """
			hazelcast:
			  network:
			    join:
			      auto-detection:
			        enabled: false
			      multicast:
			        enabled: false
			""")
	@WithResource(name = "hazelcast.yaml", content = """
			hazelcast:
			  network:
			    join:
			      auto-detection:
			        enabled: false
			      multicast:
			        enabled: false
			""")
	void defaultConfigFileIsHazelcastXml() {
		// no hazelcast-client.xml and hazelcast.xml is present in root classpath
		// this also asserts that XML has priority over YAML
		// as hazelcast.yaml, hazelcast.yml, and hazelcast.xml are available.
		this.contextRunner.run((context) -> {
			Config config = context.getBean(HazelcastInstance.class).getConfig();
			assertThat(config.getConfigurationUrl()).isEqualTo(new ClassPathResource("hazelcast.xml").getURL());
		});
	}

}
