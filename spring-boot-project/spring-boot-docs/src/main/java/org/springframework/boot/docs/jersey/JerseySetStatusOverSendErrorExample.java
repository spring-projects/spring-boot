/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.docs.jersey;

import java.util.Collections;

import javax.servlet.http.HttpServletResponse;

import org.glassfish.jersey.server.ResourceConfig;

import org.springframework.stereotype.Component;

/**
 * Example configuration for a Jersey {@link ResourceConfig} configured to use
 * {@link HttpServletResponse#setStatus(int)} rather than
 * {@link HttpServletResponse#sendError(int)}.
 *
 * @author Andy Wilkinson
 */
public class JerseySetStatusOverSendErrorExample {

	// tag::resource-config[]
	@Component
	public class JerseyConfig extends ResourceConfig {

		public JerseyConfig() {
			register(Endpoint.class);
			setProperties(Collections.singletonMap("jersey.config.server.response.setStatusOverSendError", true));
		}

	}
	// end::resource-config[]

	static class Endpoint {

	}

}
