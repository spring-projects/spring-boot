/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.jolokia;

import java.util.Map;
import java.util.function.Supplier;

import org.jolokia.http.AgentServlet;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.web.EndpointServlet;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpoint;

/**
 * {@link Endpoint} to expose a Jolokia {@link AgentServlet}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@ServletEndpoint(id = "jolokia")
public class JolokiaEndpoint implements Supplier<EndpointServlet> {

	private final Map<String, String> initParameters;

	public JolokiaEndpoint(Map<String, String> initParameters) {
		this.initParameters = initParameters;
	}

	@Override
	public EndpointServlet get() {
		return new EndpointServlet(AgentServlet.class)
				.withInitParameters(this.initParameters);
	}

}
