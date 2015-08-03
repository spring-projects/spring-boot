/*
 * Copyright 2013-2015 the original author or authors.
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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * {@link MvcEndpoint} to add hypermedia links.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
@ConfigurationProperties("endpoints.links")
public class LinksMvcEndpoint implements MvcEndpoint {

	private String path;
	private boolean sensitive = false;

	public LinksMvcEndpoint(String defaultPath) {
		this.path = defaultPath;
	}

	@RequestMapping(value = { "/", "" }, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResourceSupport links() {
		return new ResourceSupport();
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String getPath() {
		return this.path;
	}

	@Override
	public boolean isSensitive() {
		return this.sensitive;
	}

	public void setSensitive(boolean sensitive) {
		this.sensitive = sensitive;
	}

	@Override
	public Class<? extends Endpoint<?>> getEndpointType() {
		return null;
	}

}
