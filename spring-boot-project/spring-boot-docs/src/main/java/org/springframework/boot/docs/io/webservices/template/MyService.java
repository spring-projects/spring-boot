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

package org.springframework.boot.docs.io.webservices.template;

import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;

/**
 * MyService class.
 */
@Service
public class MyService {

	private final WebServiceTemplate webServiceTemplate;

	/**
	 * Constructs a new instance of MyService with the provided WebServiceTemplateBuilder.
	 * @param webServiceTemplateBuilder the builder used to create the WebServiceTemplate
	 */
	public MyService(WebServiceTemplateBuilder webServiceTemplateBuilder) {
		this.webServiceTemplate = webServiceTemplateBuilder.build();
	}

	/**
	 * Makes a web service call with the given request details and returns the response.
	 * @param detailsReq the request details for the web service call
	 * @return the response received from the web service
	 */
	public SomeResponse someWsCall(SomeRequest detailsReq) {
		return (SomeResponse) this.webServiceTemplate.marshalSendAndReceive(detailsReq,
				new SoapActionCallback("https://ws.example.com/action"));
	}

}
