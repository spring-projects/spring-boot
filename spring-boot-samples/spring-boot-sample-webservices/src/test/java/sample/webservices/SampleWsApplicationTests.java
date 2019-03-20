/*
 * Copyright 2012-2018 the original author or authors.
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

package sample.webservices;

import java.io.StringReader;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.ws.client.core.WebServiceTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class SampleWsApplicationTests {

	@Rule
	public final OutputCapture output = new OutputCapture();

	private WebServiceTemplate webServiceTemplate = new WebServiceTemplate();

	@LocalServerPort
	private int serverPort;

	@Before
	public void setUp() {
		this.webServiceTemplate
				.setDefaultUri("http://localhost:" + this.serverPort + "/services/");
	}

	@Test
	public void testSendingHolidayRequest() {
		final String request = "<hr:HolidayRequest xmlns:hr=\"http://mycompany.com/hr/schemas\">"
				+ "   <hr:Holiday>" + "      <hr:StartDate>2013-10-20</hr:StartDate>"
				+ "      <hr:EndDate>2013-11-22</hr:EndDate>" + "   </hr:Holiday>"
				+ "   <hr:Employee>" + "      <hr:Number>1</hr:Number>"
				+ "      <hr:FirstName>John</hr:FirstName>"
				+ "      <hr:LastName>Doe</hr:LastName>" + "   </hr:Employee>"
				+ "</hr:HolidayRequest>";
		StreamSource source = new StreamSource(new StringReader(request));
		StreamResult result = new StreamResult(System.out);
		this.webServiceTemplate.sendSourceAndReceiveToResult(source, result);
		assertThat(this.output.toString()).contains("Booking holiday for");
	}

}
