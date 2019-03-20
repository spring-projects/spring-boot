/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.hateoas;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties properties} for Spring HATEOAS.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.2.1
 */
@ConfigurationProperties(prefix = "spring.hateoas")
public class HateoasProperties {

	/**
	 * Whether application/hal+json responses should be sent to requests that accept
	 * application/json.
	 */
	private boolean useHalAsDefaultJsonMediaType = true;

	public boolean getUseHalAsDefaultJsonMediaType() {
		return this.useHalAsDefaultJsonMediaType;
	}

	public void setUseHalAsDefaultJsonMediaType(boolean useHalAsDefaultJsonMediaType) {
		this.useHalAsDefaultJsonMediaType = useHalAsDefaultJsonMediaType;
	}

}
