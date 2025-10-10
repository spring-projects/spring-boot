/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.webservices.test.autoconfigure.server;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

/**
 * Example web service {@code @Endpoint} used with
 * {@link WebServiceServerTest @WebServiceServerTest} tests.
 *
 * @author Daniil Razorenov
 */
@Endpoint
public class ExampleWebServiceEndpoint {

	@PayloadRoot(localPart = "request")
	@ResponsePayload
	Response payloadMethod(@RequestPayload Request request) {
		return new Response(42);
	}

}
