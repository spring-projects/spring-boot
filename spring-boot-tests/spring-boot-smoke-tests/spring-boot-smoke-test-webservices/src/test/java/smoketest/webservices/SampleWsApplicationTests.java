/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.ws.client.core.WebServiceTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
class SampleWsApplicationTests {

	private WebServiceTemplate webServiceTemplate = new WebServiceTemplate();

	@LocalServerPort
	private int serverPort;

	@BeforeEach
	void setUp() {
		this.webServiceTemplate.setDefaultUri("http://localhost:" + this.serverPort + "/services/");
	}

	@Test
	void testSendingHolidayRequest(CapturedOutput output) {
		final String request = "<hr:HolidayRequest xmlns:hr=\"https://company.example.com/hr/schemas\">"
				+ "   <hr:Holiday>      <hr:StartDate>2013-10-20</hr:StartDate>"
				+ "      <hr:EndDate>2013-11-22</hr:EndDate>   </hr:Holiday>   <hr:Employee>"
				+ "      <hr:Number>1</hr:Number>      <hr:FirstName>John</hr:FirstName>"
				+ "      <hr:LastName>Doe</hr:LastName>   </hr:Employee></hr:HolidayRequest>";
		StreamSource source = new StreamSource(new StringReader(request));
		StreamResult result = new StreamResult(System.out);
		this.webServiceTemplate.sendSourceAndReceiveToResult(source, result);
		assertThat(output).contains("Booking holiday for");
	}

}
