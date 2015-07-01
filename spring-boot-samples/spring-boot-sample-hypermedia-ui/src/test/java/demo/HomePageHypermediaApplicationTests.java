package demo;

import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SpringBootHypermediaApplication.class)
@WebAppConfiguration
@IntegrationTest({ "management.contextPath=", "server.port=0" })
public class HomePageHypermediaApplicationTests {

	@Value("${local.server.port}")
	private int port;

	@Test
	public void home() {
		String response = new TestRestTemplate().getForObject("http://localhost:" + port,
				String.class);
		assertTrue("Wrong body: " + response, response.contains("Hello World"));
	}

	@Test
	public void links() {
		String response = new TestRestTemplate().getForObject("http://localhost:" + port + "/links",
				String.class);
		assertTrue("Wrong body: " + response, response.contains("\"_links\":"));
	}

	@Test
	public void linksWithJson() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		ResponseEntity<String> response = new TestRestTemplate().exchange(
				new RequestEntity<Void>(headers , HttpMethod.GET, new URI("http://localhost:"
						+ port + "/links")), String.class);
		assertTrue("Wrong body: " + response, response.getBody().contains("\"_links\":"));
	}

	@Test
	public void homeWithHtml() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		ResponseEntity<String> response = new TestRestTemplate().exchange(
				new RequestEntity<Void>(headers , HttpMethod.GET, new URI("http://localhost:"
						+ port)), String.class);
		assertTrue("Wrong body: " + response, response.getBody().contains("Hello World"));
	}

}
