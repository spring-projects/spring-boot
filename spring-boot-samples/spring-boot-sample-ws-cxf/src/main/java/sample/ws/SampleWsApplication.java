/*
 * Copyright 2012-2015 the original author or authors.
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

import org.springframework.context.annotation.Bean;
import sample.ws.service.Hello;
import sample.ws.service.HelloPortImpl;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.apache.cxf.bus.spring.SpringBus;
import javax.xml.ws.Endpoint;
import org.springframework.boot.autoconfigure.cxf.CXFWSAdapter;

/**
 * Spring-boot CXF Web service example with ws adapter.
 * This extends the {@link CXFWSAdapter} abstract class and implement the setupWSEndpoint method.
 * User has to initilize service bean and the endpint address to configure the Webservice.
 *
 * @author Elan Thangamani
 */

@SpringBootApplication
@EnableAutoConfiguration
public class SampleWsApplication extends CXFWSAdapter {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleWsApplication.class, args);
	}
	
	@Override
	public Endpoint setupWSEndpoint(SpringBus springBus) {
	 	EndpointImpl endpoint = new EndpointImpl(springBus, helloService()); 
	 	endpoint.publish("/Hello");
	 	return endpoint;
	}	

	@Bean 
	public Hello helloService() { 
	    return new HelloPortImpl(); 
	} 
}
