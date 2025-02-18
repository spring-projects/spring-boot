/*
 * Copyright 2012-2023 the original author or authors.
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

package smoketest.jetty.service;

import smoketest.jetty.util.StringUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpHeaderService {

	@Value("${server.jetty.max-http-response-header-size}")
	private int maxHttpResponseHeaderSize;

	/**
	 * Generates a header value, which is longer than
	 * 'server.jetty.max-http-response-header-size'.
	 * @return the header value
	 */
	public String getHeaderValue() {
		return StringUtil.repeat('A', this.maxHttpResponseHeaderSize + 1);
	}

}
