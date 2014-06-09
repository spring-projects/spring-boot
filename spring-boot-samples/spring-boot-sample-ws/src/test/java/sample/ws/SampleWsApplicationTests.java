package sample.ws;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.ws.client.core.WebServiceTemplate;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;

/**
 * Tests handling SOAP message
 *
 * @author Maciej Walkowiak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleWsApplication.class)
@WebAppConfiguration
@IntegrationTest
public class SampleWsApplicationTests {
	private WebServiceTemplate webServiceTemplate = new WebServiceTemplate();

	@Value("${local.server.port}")
	private int serverPort;

	@Before
	public void setUp() {
		webServiceTemplate.setDefaultUri("http://localhost:" + serverPort + "/services/");
	}

	@Test
	public void testSendingHolidayRequest() {
		final String request = "<hr:HolidayRequest xmlns:hr=\"http://mycompany.com/hr/schemas\">"
				+ "   <hr:Holiday>"
				+ "      <hr:StartDate>2013-10-20</hr:StartDate>"
				+ "      <hr:EndDate>2013-11-22</hr:EndDate>"
				+ "   </hr:Holiday>"
				+ "   <hr:Employee>"
				+ "      <hr:Number>1</hr:Number>"
				+ "      <hr:FirstName>John</hr:FirstName>"
				+ "      <hr:LastName>Doe</hr:LastName>"
				+ "   </hr:Employee>"
				+ "</hr:HolidayRequest>";

		StreamSource source = new StreamSource(new StringReader(request));
		StreamResult result = new StreamResult(System.out);

		webServiceTemplate.sendSourceAndReceiveToResult(source, result);
	}
}