package sample.jersey1;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import sample.jersey1.SampleJersey1Application;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleJersey1Application.class)
@WebAppConfiguration
@IntegrationTest("server.port:0")
public class SampleJersey1ApplicationTests {
	
	@Value("${local.server.port}")
	private int port;

	@Test
	public void contextLoads() {
		assertEquals("Hello World", new TestRestTemplate().getForObject("http://localhost:" + port + "/", String.class));
	}

}
