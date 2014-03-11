package sample.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.RestTemplates;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Basic integration tests for demo application.
 * 
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes=SampleWebUiApplication.class)
@WebAppConfiguration
@IntegrationTest
@DirtiesContext
public class SampleWebUiApplicationTests {

	@Test
	public void testHome() throws Exception {
		ResponseEntity<String> entity = RestTemplates.get().getForEntity(
				"http://localhost:8080", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertTrue("Wrong body (title doesn't match):\n" + entity.getBody(), entity
				.getBody().contains("<title>Messages"));
		assertFalse("Wrong body (found layout:fragment):\n" + entity.getBody(), entity
				.getBody().contains("layout:fragment"));
	}

	@Test
	public void testCreate() throws Exception {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.set("text", "FOO text");
		map.set("summary", "FOO");
		URI location = RestTemplates.get().postForLocation("http://localhost:8080", map);
		assertTrue("Wrong location:\n" + location,
				location.toString().contains("localhost:8080"));
	}

	@Test
	public void testCss() throws Exception {
		ResponseEntity<String> entity = RestTemplates.get().getForEntity(
				"http://localhost:8080/css/bootstrap.min.css", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertTrue("Wrong body:\n" + entity.getBody(), entity.getBody().contains("body"));
	}

}
