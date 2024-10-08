/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.tracing.otlp;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Details required to establish a connection to an OpenTelemetry service.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 * @since 3.2.0
 */
public interface OtlpTracingConnectionDetails extends ConnectionDetails {

	/**
	 * Address to where tracing will be published.
	 * @return the address to where tracing will be published
	 * @deprecated since 3.4.0 for removal in 3.6.0 in favor of {@link #getUrl(Transport)}
	 */
	@Deprecated(since = "3.4.0", forRemoval = true)
	default String getUrl() {
		return getUrl(Transport.HTTP);
	}

	/**
	 * Address to where tracing will be published.
	 * @param transport the transport to use
	 * @return the address to where tracing will be published
	 * @since 3.4.0
	 */
	String getUrl(Transport transport);

}
