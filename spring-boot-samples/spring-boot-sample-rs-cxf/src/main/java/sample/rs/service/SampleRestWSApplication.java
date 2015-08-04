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
 
 package sample.rs.service;

import org.springframework.context.annotation.Bean;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.cxf.CXFRestWSAdapter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * Spring-boot CXF rest Web service example with rest adapter.
 * This extends the {@link CXFRestAdapter} abstract class and implement the setupRestWSServer method.
 * User has to initilize service bean and the address to configure the rest service.
 *
 * @author Elan Thangamani
 */

@Configuration
@EnableAutoConfiguration
@ImportResource({ "classpath:META-INF/cxf/cxf.xml" })
public class SampleRestWSApplication extends CXFRestWSAdapter  {
 

   
    public static void main(String[] args) {
        SpringApplication.run(SampleRestWSApplication.class, args);
    }
    
    @Override
	 public void setupRestWSServer(JAXRSServerFactoryBean endpoint) {
		 endpoint.setServiceBean(helloService());
		 endpoint.setAddress("/helloservice");
	 }
    
	@Bean 
	public HelloService helloService() { 
	    return new HelloService(); 
	} 
}