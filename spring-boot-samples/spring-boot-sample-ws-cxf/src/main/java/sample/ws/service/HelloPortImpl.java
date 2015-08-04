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
package sample.ws.service;

import java.util.logging.Logger;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

/**
 * demo CXF webservice implementation.
 *
 * @author Elan Thangamani
 */


@javax.jws.WebService(
                      serviceName = "HelloService",
                      portName = "HelloPort",
                      targetNamespace = "http://service.ws.sample/",
                      endpointInterface = "sample.ws.service.Hello")
                      
public class HelloPortImpl implements Hello {

    private static final Logger LOG = Logger.getLogger(HelloPortImpl.class.getName());

    public java.lang.String sayHello(java.lang.String myname) { 
        LOG.info("Executing operation sayHello" + myname);
        try {
    		return "Welcome to CXF Spring boot "+myname + "!!!";

        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

}
