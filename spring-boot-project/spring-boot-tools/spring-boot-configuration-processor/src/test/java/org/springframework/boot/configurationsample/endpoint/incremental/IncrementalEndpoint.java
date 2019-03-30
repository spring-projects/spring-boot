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

package org.springframework.boot.configurationsample.endpoint.incremental;

import org.springframework.boot.configurationsample.Endpoint;
import org.springframework.boot.configurationsample.ReadOperation;
import org.springframework.lang.Nullable;

/**
 * An endpoint that is enabled by default.
 *
 * @author Stephane Nicoll
 */
@Endpoint(id = "incremental")
public class IncrementalEndpoint {

	@ReadOperation
	public String invoke(@Nullable String param) {
		return "test";
	}

}
