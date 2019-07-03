package smoketest.jersey;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.web.server.LocalManagementPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"management.server.port=0","management.endpoints.web.base-path=/"})
public class JerseyDifferentPortSampleActuatorApplicationTests {

	@LocalManagementPort
	private int managementPort;

	@LocalServerPort
	private int port;

	@Test
	void testDifferentServerPort(){
		if(this.managementPort != this.port) {
			ResponseEntity<String> entity = new TestRestTemplate("user", getPassword())
					.getForEntity("http://localhost:" + this.managementPort + "/", String.class);
			assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(entity.getBody()).contains("\"_links\"");
		}
	}

	private String getPassword(){
		return "password";
	}
}
