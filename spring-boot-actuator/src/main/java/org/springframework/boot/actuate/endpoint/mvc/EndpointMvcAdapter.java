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

package org.springframework.boot.actuate.endpoint.mvc;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Adapter class to expose {@link Endpoint}s as {@link MvcEndpoint}s.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class EndpointMvcAdapter extends AbstractEndpointMvcAdapter<Endpoint<?>> {

	/**
	 * Create a new {@link EndpointMvcAdapter}.
	 * @param delegate the underlying {@link Endpoint} to adapt.
	 */
	public EndpointMvcAdapter(Endpoint<?> delegate) {
		super(delegate);
	}

	@Override
	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Object invoke() {
		return super.invoke();
	}

}
