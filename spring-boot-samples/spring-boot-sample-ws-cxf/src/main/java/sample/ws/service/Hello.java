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
package sample.ws.service;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

/**
 * CXF JAX webservice interface example.
 *
 * @author Elan Thangamani
 */
@WebService(targetNamespace = "http://service.ws.sample/", name = "Hello")
public interface Hello {

	 @WebResult(name = "return", targetNamespace = "")
	    @RequestWrapper(localName = "sayHello", targetNamespace = "http://service.ws.sample/", className = "sample.ws.service.SayHello")
	    @WebMethod(action = "urn:SayHello")
	    @ResponseWrapper(localName = "sayHelloResponse", targetNamespace = "http://service.ws.sample/", className = "sample.ws.service.SayHelloResponse")
	    public java.lang.String sayHello(
	        @WebParam(name = "myname", targetNamespace = "")
	        java.lang.String myname
	    );
}
