/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.metrics.export.prometheus;

import io.prometheus.client.exporter.common.TextFormat;

import org.springframework.boot.actuate.endpoint.http.Producible;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * A {@link Producible} for Prometheus's {@link TextFormat}.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public enum ProducibleTextFormat implements Producible<ProducibleTextFormat> {

	/**
	 * Openmetrics text version 1.0.0.
	 */
	CONTENT_TYPE_OPENMETRICS_100(TextFormat.CONTENT_TYPE_OPENMETRICS_100),

	/**
	 * Prometheus text version 0.0.4.
	 */
	CONTENT_TYPE_004(TextFormat.CONTENT_TYPE_004);

	private final MimeType mimeType;

	ProducibleTextFormat(String mimeType) {
		this.mimeType = MimeTypeUtils.parseMimeType(mimeType);
	}

	@Override
	public MimeType getMimeType() {
		return this.mimeType;
	}

}
