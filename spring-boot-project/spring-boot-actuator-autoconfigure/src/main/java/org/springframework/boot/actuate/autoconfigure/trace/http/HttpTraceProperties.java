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

package org.springframework.boot.actuate.autoconfigure.trace.http;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.actuate.trace.http.Include;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for HTTP tracing.
 *
 * @author Wallace Wadge
 * @author Phillip Webb
 * @author Venil Noronha
 * @author Madhura Bhave
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.trace.http")
public class HttpTraceProperties {

	/**
	 * Items to be included in the trace. Defaults to request headers (excluding
	 * Authorization but including Cookie), response headers (including Set-Cookie), and
	 * time taken.
	 */
	private Set<Include> include = new HashSet<>(Include.defaultIncludes());

	public Set<Include> getInclude() {
		return this.include;
	}

	public void setInclude(Set<Include> include) {
		this.include = include;
	}

}
