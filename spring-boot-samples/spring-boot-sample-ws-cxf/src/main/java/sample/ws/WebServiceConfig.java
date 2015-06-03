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

package sample.ws;

import org.springframework.boot.context.embedded.ServletRegistrationBean;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.apache.cxf.bus.spring.SpringBus;
import sample.ws.service.Hello;
import sample.ws.service.HelloPortImpl;
import javax.xml.ws.Endpoint;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.apache.cxf.jaxws.EndpointImpl
;

@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter {

	@Bean 
	public ServletRegistrationBean dispatcherServlet() { 
	    CXFServlet cxfServlet = new CXFServlet(); 
	    return new ServletRegistrationBean(cxfServlet, "/Service/*"); 
	} 

	@Bean(name="cxf") 
	public SpringBus springBus() { 
	    return new SpringBus(); 
	} 

	@Bean 
	public Hello myService() { 
	    return new HelloPortImpl(); 
	} 

	@Bean 
	public Endpoint endpoint() { 
	    EndpointImpl endpoint = new EndpointImpl(springBus(), myService()); 
	    endpoint.publish("/Hello"); 
	    return endpoint; 
	} 
}
