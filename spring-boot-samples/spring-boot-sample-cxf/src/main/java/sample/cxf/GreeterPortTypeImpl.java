/*
 * Copyright 2012-2016 the original author or authors.
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

package sample.cxf;

import javax.jws.WebService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebService(targetNamespace = "http://sample/cxf",
		serviceName = "GreeterService", portName = "GreeterPort",
		wsdlLocation = "wsdl/GreeterService.wsdl",
		endpointInterface = "sample.cxf.GreeterPortType")
public class GreeterPortTypeImpl implements GreeterPortType {

	private static final Logger logger = LoggerFactory.getLogger(
			GreeterPortTypeImpl.class);

	@Override
	public String greet(String name) {
		logger.info("Greeting {}...", name);
		return "Hello " + name + "!";
	}

}
