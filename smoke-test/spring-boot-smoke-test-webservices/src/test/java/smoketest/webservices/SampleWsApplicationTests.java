/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.webservices;

import java.io.StringReader;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.ws.client.core.WebServiceTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
class SampleWsApplicationTests {

	private final WebServiceTemplate webServiceTemplate = new WebServiceTemplate();

	@LocalServerPort
	private int serverPort;

	@BeforeEach
	void setUp() {
		this.webServiceTemplate.setDefaultUri("http://localhost:" + this.serverPort + "/services/");
	}

	@Test
	void testSendingHolidayRequest(CapturedOutput output) {
		String request = """
				<hr:HolidayRequest xmlns:hr="https://company.example.com/hr/schemas">
					<hr:Holiday>
						<hr:StartDate>2013-10-20</hr:StartDate>
						<hr:EndDate>2013-11-22</hr:EndDate>
					</hr:Holiday>
					<hr:Employee>
						<hr:Number>1</hr:Number>
						<hr:FirstName>John</hr:FirstName>
						<hr:LastName>Doe</hr:LastName>
					</hr:Employee>
				</hr:HolidayRequest>""";
		StreamSource source = new StreamSource(new StringReader(request));
		StreamResult result = new StreamResult(System.out);
		this.webServiceTemplate.sendSourceAndReceiveToResult(source, result);
		assertThat(output).contains("Booking holiday for");
	}

}
