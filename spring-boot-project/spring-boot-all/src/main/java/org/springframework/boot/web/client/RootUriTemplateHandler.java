/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.web.client;

import java.net.URI;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriTemplateHandler;

/**
 * {@link UriTemplateHandler} to set the root for URI that starts with {@code '/'}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 1.4.0
 */
public class RootUriTemplateHandler implements UriTemplateHandler {

	private final String rootUri;

	private final UriTemplateHandler handler;

	protected RootUriTemplateHandler(UriTemplateHandler handler) {
		Assert.notNull(handler, "'handler' must not be null");
		this.rootUri = null;
		this.handler = handler;
	}

	RootUriTemplateHandler(String rootUri, UriTemplateHandler handler) {
		Assert.notNull(rootUri, "'rootUri' must not be null");
		Assert.notNull(handler, "'handler' must not be null");
		this.rootUri = rootUri;
		this.handler = handler;
	}

	@Override
	public URI expand(String uriTemplate, Map<String, ?> uriVariables) {
		return this.handler.expand(apply(uriTemplate), uriVariables);
	}

	@Override
	public URI expand(String uriTemplate, Object... uriVariables) {
		return this.handler.expand(apply(uriTemplate), uriVariables);
	}

	String apply(String uriTemplate) {
		if (StringUtils.startsWithIgnoreCase(uriTemplate, "/")) {
			return getRootUri() + uriTemplate;
		}
		return uriTemplate;
	}

	public String getRootUri() {
		return this.rootUri;
	}

}
