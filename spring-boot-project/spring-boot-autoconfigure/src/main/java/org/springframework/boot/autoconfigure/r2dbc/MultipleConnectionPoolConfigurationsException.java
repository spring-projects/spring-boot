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

package org.springframework.boot.autoconfigure.r2dbc;

/**
 * Exception thrown when R2DBC connection pooling has been configured both by the URL
 * ({@code spring.r2dbc.url}) and the pool properties ({@code spring.r2dbc.pool.*}.
 *
 * @author Andy Wilkinson
 */
class MultipleConnectionPoolConfigurationsException extends RuntimeException {

	MultipleConnectionPoolConfigurationsException() {
		super("R2DBC connection pooling configuration should be provided by either the spring.r2dbc.pool.* "
				+ "properties or the spring.r2dbc.url property but both have been used.");
	}

}
