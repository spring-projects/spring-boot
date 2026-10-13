/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.webservices;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.XsdSchema;

@Configuration(proxyBeanMethods = false)
class WebServiceConfig {

	@Bean(name = "holiday")
	DefaultWsdl11Definition defaultWsdl11Definition(@Qualifier("hr") XsdSchema hrSchema) {
		DefaultWsdl11Definition wsdl = new DefaultWsdl11Definition();
		wsdl.setPortTypeName("HumanResource");
		wsdl.setLocationUri("/holidayService/");
		wsdl.setTargetNamespace("https://company.example.com/hr/definitions");
		wsdl.setSchema(hrSchema);
		return wsdl;
	}

}
