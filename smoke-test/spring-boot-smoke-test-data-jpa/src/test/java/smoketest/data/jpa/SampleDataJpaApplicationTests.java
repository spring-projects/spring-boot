/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.jpa;

import java.lang.management.ManagementFactory;

import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to run the application.
 *
 * @author Oliver Gierke
 * @author Dave Syer
 */
// Enable JMX so we can test the MBeans (you can't do this in a properties file)
@SpringBootTest(properties = "spring.jmx.enabled:true")
@AutoConfigureMockMvc
@ActiveProfiles("scratch")
// Separate profile for web tests to avoid clashing databases
class SampleDataJpaApplicationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void testHome() {
		assertThat(this.mvc.get().uri("/")).hasStatusOk().hasBodyTextEqualTo("Bath");
	}

	@Test
	void testJmx() throws Exception {
		assertThat(ManagementFactory.getPlatformMBeanServer()
			.queryMBeans(new ObjectName("jpa.sample:type=HikariDataSource,*"), null)).hasSize(1);
	}

}
