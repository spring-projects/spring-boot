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

package org.springframework.boot.test.web.htmlunit;

import java.io.IOException;
import java.net.MalformedURLException;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;

import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * {@link WebClient} will automatically prefix relative URLs with
 * <code>localhost:$&#123;local.server.port&#125;</code>.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
public class LocalHostWebClient extends WebClient {

	private final Environment environment;

	public LocalHostWebClient(Environment environment) {
		Assert.notNull(environment, "Environment must not be null");
		this.environment = environment;
	}

	@Override
	public <P extends Page> P getPage(String url)
			throws IOException, FailingHttpStatusCodeException, MalformedURLException {
		if (url.startsWith("/")) {
			String port = this.environment.getProperty("local.server.port", "8080");
			url = "http://localhost:" + port + url;
		}
		return super.getPage(url);
	}

}
