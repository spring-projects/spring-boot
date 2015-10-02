package sample.secure.sso;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Series of automated integration tests to verify proper behavior of auto-configured,
 * OAuth2-secured system
 *
 * @author Greg Turnquist
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@SpringApplicationConfiguration(SampleOAuth2SsoApplication.class)
@IntegrationTest("server.port:0")
public class SampleOAuth2SsoApplicationTests {

	@Autowired
	WebApplicationContext context;

	@Autowired
	FilterChainProxy filterChain;

	private MockMvc mvc;

	@Before
	public void setUp() {
		this.mvc = webAppContextSetup(this.context).addFilters(this.filterChain).build();
		SecurityContextHolder.clearContext();
	}

	@Test
	public void everythingIsSecuredByDefault() throws Exception {
		this.mvc.perform(get("/"))
				.andExpect(status().isFound())
				.andExpect(header().string("location", containsString("localhost/login")));
	}

}
