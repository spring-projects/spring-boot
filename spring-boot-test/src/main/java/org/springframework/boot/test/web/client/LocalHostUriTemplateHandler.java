/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test.web.client;

import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.web.util.DefaultUriTemplateHandler;
import org.springframework.web.util.UriTemplateHandler;

/**
 * {@link UriTemplateHandler} will automatically prefix relative URIs with
 * <code>localhost:$&#123;local.server.port&#125;</code>.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @since 1.4.0
 */
public class LocalHostUriTemplateHandler extends RootUriTemplateHandler {

	private final Environment environment;

	private final String scheme;

	private RelaxedPropertyResolver serverPropertyResolver;

	/**
	 * Create a new {@code LocalHostUriTemplateHandler} that will generate {@code http}
	 * URIs using the given {@code environment} to determine the context path and port.
	 * @param environment the environment used to determine the port
	 */
	public LocalHostUriTemplateHandler(Environment environment) {
		this(environment, "http");
	}

	/**
	 * Create a new {@code LocalHostUriTemplateHandler} that will generate URIs with the
	 * given {@code scheme} and use the given {@code environment} to determine the
	 * context-path and port.
	 * @param environment the environment used to determine the port
	 * @param scheme the scheme of the root uri
	 * @since 1.4.1
	 */
	public LocalHostUriTemplateHandler(Environment environment, String scheme) {
		super(new DefaultUriTemplateHandler());
		Assert.notNull(environment, "Environment must not be null");
		Assert.notNull(scheme, "Scheme must not be null");
		this.environment = environment;
		this.scheme = scheme;
		this.serverPropertyResolver = new RelaxedPropertyResolver(environment, "server.");
	}

	@Override
	public String getRootUri() {
		String port = this.environment.getProperty("local.server.port", "8080");
		String contextPath = this.serverPropertyResolver.getProperty("context-path", "");
		return this.scheme + "://localhost:" + port + contextPath;
	}

}
