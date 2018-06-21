package org.springframework.boot.actuate.autoconfigure;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@TestPropertySource(properties = "management.security.enabled=false")
public class MetricFilterJerseyTests {

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Autowired
	private CounterService counterService;

	@Autowired
	private GaugeService gaugeService;

	@Test
	public void recordsHttpInteractionsWithTemplateVariables() {
		String body = testRestTemplate.getForObject("/api/templateVarTest/foo", String.class);
		assertThat(body).isEqualTo("foo");

		body = testRestTemplate.getForObject("/api/templateRegexpTest/foo", String.class);
		assertThat(body).isEqualTo("foo");

		verify(counterService).increment("status.200.api.templateVarTest.someVariable");
		verify(gaugeService).submit(eq("response.api.templateVarTest.someVariable"), anyDouble());

		verify(counterService).increment("status.200.api.templateRegexpTest.someRegexp");
		verify(gaugeService).submit(eq("response.api.templateRegexpTest.someRegexp"), anyDouble());
	}

	@MinimalActuatorHypermediaApplication
	@ImportAutoConfiguration({JerseyAutoConfiguration.class, MetricFilterAutoConfiguration.class})
	@Import({JerseyConfig.class, MetricFilterAutoConfigurationTests.Config.class})
	public static class Application {
	}

	@Component
	@ApplicationPath("/api")
	public static class JerseyConfig extends ResourceConfig {
		public JerseyConfig() {
			register(TemplateVarEndpoint.class);
			register(TemplateRegexpEndpoint.class);
		}
	}

	@Path("/templateVarTest/{someVariable}")
	public static class TemplateVarEndpoint {
		@GET
		public String get(@PathParam("someVariable") String someVariable) {
			return someVariable;
		}
	}

	@Path("/templateRegexpTest/{someRegexp: [a-zA-Z]*}")
	public static class TemplateRegexpEndpoint {
		@GET
		public String get(@PathParam("someRegexp") String someRegexp) {
			return someRegexp;
		}
	}

}
