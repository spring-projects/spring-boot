/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.web.exchanges;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * The recordable parts of an HTTP request used when creating an {@link HttpExchange}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.0.0
 * @see RecordableHttpResponse
 */
public interface RecordableHttpRequest {

	/**
	 * Returns the URI of the request.
	 * @return the URI
	 */
	URI getUri();

	/**
	 * Returns the remote address from which the request was sent, if available.
	 * @return the remote address or {@code null}
	 */
	String getRemoteAddress();

	/**
	 * Returns the method (GET, POST, etc.) of the request.
	 * @return the method
	 */
	String getMethod();

	/**
	 * Returns a modifiable copy of the headers of the request.
	 * @return the headers
	 */
	Map<String, List<String>> getHeaders();

}
