package sample.servlet;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.RestTemplates;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Basic integration tests for demo application.
 * 
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes=SampleServletApplication.class)
@WebAppConfiguration
@IntegrationTest
@DirtiesContext
public class SampleServletApplicationTests {

	@Autowired
	private SecurityProperties security;

	@Test
	public void testHomeIsSecure() throws Exception {
		ResponseEntity<String> entity = RestTemplates.get().getForEntity(
				"http://localhost:8080", String.class);
		assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
	}

	@Test
	public void testHome() throws Exception {
		ResponseEntity<String> entity = RestTemplates.get("user", getPassword())
				.getForEntity("http://localhost:8080", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("Hello World", entity.getBody());
	}

	private String getPassword() {
		return security.getUser().getPassword();
	}
}
