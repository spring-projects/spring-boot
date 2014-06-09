/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sample.ws.endpoint;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;

import sample.ws.service.HumanResourceService;

@Endpoint
public class HolidayEndpoint {

	private static final String NAMESPACE_URI = "http://mycompany.com/hr/schemas";

	private XPathExpression<Element> startDateExpression;
	private XPathExpression<Element> endDateExpression;
	private XPathExpression<String> nameExpression;

	private HumanResourceService humanResourceService;

	@Autowired
	public HolidayEndpoint(HumanResourceService humanResourceService)
			throws JDOMException, XPathFactoryConfigurationException,
			XPathExpressionException {
		this.humanResourceService = humanResourceService;

		Namespace namespace = Namespace.getNamespace("hr", NAMESPACE_URI);

		XPathFactory xPathFactory = XPathFactory.instance();

		this.startDateExpression = xPathFactory.compile("//hr:StartDate",
				Filters.element(), null, namespace);
		this.endDateExpression = xPathFactory.compile("//hr:EndDate", Filters.element(),
				null, namespace);
		this.nameExpression = xPathFactory.compile(
				"concat(//hr:FirstName,' ',//hr:LastName)", Filters.fstring(), null,
				namespace);
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "HolidayRequest")
	public void handleHolidayRequest(@RequestPayload Element holidayRequest)
			throws Exception {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date startDate = dateFormat.parse(this.startDateExpression.evaluateFirst(
				holidayRequest).getText());
		Date endDate = dateFormat.parse(this.endDateExpression.evaluateFirst(
				holidayRequest).getText());
		String name = this.nameExpression.evaluateFirst(holidayRequest);

		this.humanResourceService.bookHoliday(startDate, endDate, name);
	}
}
