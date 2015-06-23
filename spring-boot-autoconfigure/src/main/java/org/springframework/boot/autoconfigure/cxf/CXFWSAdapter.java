
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

package org.springframework.boot.autoconfigure.cxf;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.cxf.bus.spring.SpringBus;
/**
 * CXF Web service adapter.
 *
 * @author Elan Thangamani
 */

public abstract class CXFWSAdapter {

	@Autowired
	private ApplicationContext applicationContext;
	 
	@Bean(name="cxf") 
	public SpringBus springBus() { 
	    return new SpringBus(); 
	}
	
	@Bean 
	public Endpoint endpointPublish() { 
		Endpoint endpoint = setupWSEndpoint(springBus());
	    return endpoint; 
	}
	 public abstract Endpoint setupWSEndpoint(SpringBus springBus);

}
