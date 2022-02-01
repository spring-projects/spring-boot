/*
 * Copyright 2012-2021 the original author or authors.
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import smoketest.webservices.service.HumanResourceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.webservices.server.WebServiceServerTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.ws.test.server.MockWebServiceClient;
import org.springframework.ws.test.server.RequestCreators;
import org.springframework.ws.test.server.ResponseMatchers;

import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link SampleWsApplicationTests} that use {@link WebServiceServerTest} and
 * {@link MockWebServiceClient}.
 *
 * @author Andy Wilkinson
 */
@WebServiceServerTest
@ExtendWith(OutputCaptureExtension.class)
class WebServiceServerTestSampleWsApplicationTests {

	@MockBean
	HumanResourceService service;

	@Autowired
	private MockWebServiceClient client;

	@Test
	void testSendingHolidayRequest(CapturedOutput output) throws ParseException {
		String request = "<hr:HolidayRequest xmlns:hr=\"https://company.example.com/hr/schemas\">"
				+ "   <hr:Holiday>      <hr:StartDate>2013-10-20</hr:StartDate>"
				+ "      <hr:EndDate>2013-11-22</hr:EndDate>   </hr:Holiday>   <hr:Employee>"
				+ "      <hr:Number>1</hr:Number>      <hr:FirstName>John</hr:FirstName>"
				+ "      <hr:LastName>Doe</hr:LastName>   </hr:Employee></hr:HolidayRequest>";
		StreamSource source = new StreamSource(new StringReader(request));
		this.client.sendRequest(RequestCreators.withPayload(source)).andExpect(ResponseMatchers.noFault());
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		then(this.service).should().bookHoliday(dateFormat.parse("2013-10-20"), dateFormat.parse("2013-11-22"),
				"John Doe");
	}

}
