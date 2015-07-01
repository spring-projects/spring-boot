package org.springframework.boot.actuate.autoconfigure;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.ContextPathHypermediaIntegrationTests.SpringBootHypermediaApplication;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SpringBootHypermediaApplication.class)
@WebAppConfiguration
@TestPropertySource(properties = "management.contextPath:/admin")
@DirtiesContext
public class ContextPathHypermediaIntegrationTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private MvcEndpoints mvcEndpoints;

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@Test
	public void home() throws Exception {
		this.mockMvc.perform(get("/").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk()).andExpect(jsonPath("$._links").exists());
	}

	@Test
	public void links() throws Exception {
		this.mockMvc.perform(get("/admin").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk()).andExpect(jsonPath("$._links").exists());
	}

	@Test
	public void trace() throws Exception {
		this.mockMvc
		.perform(get("/admin/trace").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk())
		.andExpect(
				jsonPath("$._links.self.href").value(
						"http://localhost/admin/trace"))
						.andExpect(jsonPath("$.content").isArray());
	}

	@Test
	public void endpointsAllListed() throws Exception {
		for (MvcEndpoint endpoint : this.mvcEndpoints.getEndpoints()) {
			String path = endpoint.getPath();
			path = path.startsWith("/") ? path.substring(1) : path;
			path = path.length() > 0 ? path : "self";
			this.mockMvc
			.perform(get("/admin").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(
					jsonPath("$._links.%s.href", path).value(
							"http://localhost/admin" + endpoint.getPath()));
		}
	}

	@MinimalActuatorHypermediaApplication
	@RestController
	public static class SpringBootHypermediaApplication {

		@RequestMapping("")
		public ResourceSupport home() {
			ResourceSupport resource = new ResourceSupport();
			resource.add(linkTo(SpringBootHypermediaApplication.class).slash("/")
					.withSelfRel());
			return resource;
		}

		public static void main(String[] args) {
			new SpringApplicationBuilder(SpringBootHypermediaApplication.class)
			.properties("management.contextPath:/admin").run(args);
		}
	}

}
