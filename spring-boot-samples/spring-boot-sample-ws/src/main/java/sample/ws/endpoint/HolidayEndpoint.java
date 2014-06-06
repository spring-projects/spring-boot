package sample.ws.endpoint;

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

import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactoryConfigurationException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author in329dei
 * @author Maciej Walkowiak
 */
@Endpoint
public class HolidayEndpoint {

	private static final String NAMESPACE_URI = "http://mycompany.com/hr/schemas";

	private XPathExpression<Element> startDateExpression;
	private XPathExpression<Element> endDateExpression;
	private XPathExpression<String> nameExpression;

	private HumanResourceService humanResourceService;

	@Autowired
	public HolidayEndpoint(HumanResourceService humanResourceService) throws JDOMException, XPathFactoryConfigurationException, XPathExpressionException {
		this.humanResourceService = humanResourceService;

		Namespace namespace = Namespace.getNamespace("hr", NAMESPACE_URI);

		XPathFactory xPathFactory = XPathFactory.instance();

		startDateExpression = xPathFactory.compile("//hr:StartDate", Filters.element(), null, namespace);
		endDateExpression = xPathFactory.compile("//hr:EndDate", Filters.element(), null, namespace);
		nameExpression = xPathFactory.compile("concat(//hr:FirstName,' ',//hr:LastName)", Filters.fstring(), null, namespace);
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "HolidayRequest")
	public void handleHolidayRequest(@RequestPayload Element holidayRequest) throws Exception {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date startDate = dateFormat.parse(startDateExpression.evaluateFirst(holidayRequest).getText());
		Date endDate = dateFormat.parse(endDateExpression.evaluateFirst(holidayRequest).getText());
		String name = nameExpression.evaluateFirst(holidayRequest);

		humanResourceService.bookHoliday(startDate, endDate, name);
	}
}
