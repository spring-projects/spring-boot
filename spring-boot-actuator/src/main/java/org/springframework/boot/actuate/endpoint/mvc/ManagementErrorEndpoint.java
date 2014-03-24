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

package org.springframework.boot.actuate.endpoint.mvc;

import java.util.Map;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.web.ErrorController;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Special {@link MvcEndpoint} for handling "/error" path when the management servlet is
 * in a child context. The regular {@link ErrorController} should be available there but
 * because of the way the handler mappings are set up it will not be detected.
 * 
 * @author Dave Syer
 */
@ConfigurationProperties(prefix = "error")
public class ManagementErrorEndpoint implements MvcEndpoint {

	private final ErrorController controller;

	private final String path;

	public ManagementErrorEndpoint(String path, ErrorController controller) {
		Assert.notNull(controller, "Controller must not be null");
		this.path = path;
		this.controller = controller;
	}

	@RequestMapping
	@ResponseBody
	public Map<String, Object> invoke() {
		RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		return this.controller.extract(attributes, false, true);
	}

	@Override
	public String getPath() {
		return this.path;
	}

	@Override
	public boolean isSensitive() {
		return false;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Class<? extends Endpoint> getEndpointType() {
		return null;
	}
}
