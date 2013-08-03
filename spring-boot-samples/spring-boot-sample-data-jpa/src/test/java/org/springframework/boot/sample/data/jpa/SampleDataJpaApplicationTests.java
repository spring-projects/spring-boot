package org.springframework.boot.sample.data.jpa;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration test to run the application.
 * 
 * @author Oliver Gierke
 */
public class SampleDataJpaApplicationTests extends AbstractIntegrationTests {

	@Autowired 
	WebApplicationContext context;
	MockMvc mvc;

	@Before
	public void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Test
	public void testHome() throws Exception {

		mvc.perform(get("/")).
			andExpect(status().isOk()).
			andExpect(content().string("Bath"));
			}
}
