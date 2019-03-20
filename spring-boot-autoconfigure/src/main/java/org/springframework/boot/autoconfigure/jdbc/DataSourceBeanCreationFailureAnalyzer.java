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

package org.springframework.boot.autoconfigure.jdbc;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties.DataSourceBeanCreationException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} for failures caused by a
 * {@link DataSourceBeanCreationException}.
 *
 * @author Andy Wilkinson
 */
class DataSourceBeanCreationFailureAnalyzer
		extends AbstractFailureAnalyzer<DataSourceBeanCreationException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure,
			DataSourceBeanCreationException cause) {
		String message = cause.getMessage();
		String description = message.substring(0, message.indexOf(".")).trim();
		String action = message.substring(message.indexOf(".") + 1).trim();
		return new FailureAnalysis(description, action, cause);
	}

}
