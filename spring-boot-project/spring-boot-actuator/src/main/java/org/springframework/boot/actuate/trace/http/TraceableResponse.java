/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.trace.http;

import java.util.List;
import java.util.Map;

/**
 * A representation of an HTTP response that is suitable for tracing.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 * @see HttpExchangeTracer
 */
public interface TraceableResponse {

	/**
	 * The status of the response.
	 * @return the status
	 */
	int getStatus();

	/**
	 * Returns a modifiable copy of the headers of the response.
	 * @return the headers
	 */
	Map<String, List<String>> getHeaders();

}
