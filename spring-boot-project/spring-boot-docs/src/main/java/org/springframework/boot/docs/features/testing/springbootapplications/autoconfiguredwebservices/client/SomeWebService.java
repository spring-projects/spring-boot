/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.docs.features.testing.springbootapplications.autoconfiguredwebservices.client;

import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * SomeWebService class.
 */
@Service
public class SomeWebService {

	private final WebServiceTemplate webServiceTemplate;

	/**
     * Constructs a new SomeWebService with the provided WebServiceTemplateBuilder.
     * 
     * @param builder the WebServiceTemplateBuilder used to build the WebServiceTemplate
     */
    public SomeWebService(WebServiceTemplateBuilder builder) {
		this.webServiceTemplate = builder.build();
	}

	/**
     * This method is used to test the web service by sending a request to the specified URL.
     * 
     * @return A Response object containing the response received from the web service.
     */
    public Response test() {
		return (Response) this.webServiceTemplate.marshalSendAndReceive("https://example.com", new Request());
	}

}
