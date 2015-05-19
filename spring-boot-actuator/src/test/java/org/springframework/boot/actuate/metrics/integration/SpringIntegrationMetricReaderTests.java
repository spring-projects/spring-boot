package org.springframework.boot.actuate.metrics.integration;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.integration.SpringIntegrationMetricReaderTests.TestConfiguration;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes=TestConfiguration.class)
@IntegrationTest("spring.jmx.enabled=true")
@DirtiesContext
public class SpringIntegrationMetricReaderTests {
	
	@Autowired
	private SpringIntegrationMetricReader reader;

	@Test
	public void test() {
		assertTrue(reader.count()>0);
	}
	
	@Configuration
	@Import({JmxAutoConfiguration.class, IntegrationAutoConfiguration.class})
	protected static class TestConfiguration {
		@Bean
		public SpringIntegrationMetricReader reader(IntegrationMBeanExporter exporter) {
			return new SpringIntegrationMetricReader(exporter);
		}
	}

}
