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

package org.springframework.boot.actuate.endpoint.mvc;

import java.util.Map;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.SessionEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Adapter to expose {@link SessionEndpoint} as an {@link MvcEndpoint}.
 *
 * @author Eddú Meléndez
 * @since 1.4.0
 */
@ConfigurationProperties(prefix = "endpoints.session")
public class SessionMvcEndpoint extends AbstractEndpointMvcAdapter<SessionEndpoint> {

	/**
	 * Create a new {@link EndpointMvcAdapter}.
	 *
	 * @param delegate the underlying {@link Endpoint} to adapt.
	 */
	public SessionMvcEndpoint(SessionEndpoint delegate) {
		super(delegate);
	}

	@RequestMapping(method = RequestMethod.GET)
	public Map<Object, Object> result(@RequestParam String username) {
		return getDelegate().result(username);
	}

	@RequestMapping(path = "/{sessionId}", method = RequestMethod.DELETE)
	public ResponseEntity delete(@PathVariable String sessionId) {
		boolean deleted = getDelegate().delete(sessionId);
		if (deleted) {
			return ResponseEntity.ok().build();
		}
		return ResponseEntity.notFound().build();
	}

}
