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

package org.springframework.boot.test.web.client;

import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.web.util.DefaultUriTemplateHandler;
import org.springframework.web.util.UriTemplateHandler;

/**
 * {@link UriTemplateHandler} will automatically prefix relative URLs with
 * <code>localhost:$&#123;local.server.port&#125;</code>.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
public class LocalHostUriTemplateHandler extends RootUriTemplateHandler {

	private final Environment environment;

	public LocalHostUriTemplateHandler(Environment environment) {
		super(new DefaultUriTemplateHandler());
		Assert.notNull(environment, "Environment must not be null");
		this.environment = environment;
	}

	@Override
	public String getRootUri() {
		String port = this.environment.getProperty("local.server.port", "8080");
		return "http://localhost:" + port;
	}

}
