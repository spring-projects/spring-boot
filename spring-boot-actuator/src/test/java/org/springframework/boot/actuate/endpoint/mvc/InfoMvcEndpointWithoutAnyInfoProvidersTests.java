package org.springframework.boot.actuate.endpoint.mvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.boot.actuate.endpoint.InfoEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.InfoMvcEndpointWithoutAnyInfoProvidersTests.TestConfiguration;
import org.springframework.boot.actuate.info.InfoProvider;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Tests for {@link InfoMvcEndpointWithoutAnyInfoProvidersTests}
 *
 * @author Meang Akira Tanaka
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { TestConfiguration.class })
@WebAppConfiguration
public class InfoMvcEndpointWithoutAnyInfoProvidersTests {
	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@Before
	public void setUp() {

		this.context.getBean(InfoEndpoint.class).setEnabled(true);
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@Test
	public void home() throws Exception {
		this.mvc.perform(get("/info")).andExpect(status().isOk());
	}
	
	@Import({ JacksonAutoConfiguration.class,
		HttpMessageConvertersAutoConfiguration.class,
		EndpointWebMvcAutoConfiguration.class,
		WebMvcAutoConfiguration.class,
		ManagementServerPropertiesAutoConfiguration.class })
	@Configuration
	public static class TestConfiguration {

		private Map<String, InfoProvider> infoProviders = Maps.newHashMap();

		@Bean
		public InfoEndpoint endpoint() {
			return new InfoEndpoint(infoProviders);
		}

	}
	
}
