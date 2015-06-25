/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
 * @author Phillip webb
 * @since 1.2.1
 */
@ConfigurationProperties(prefix = "spring.hateoas")
public class HateoasProperties {

	/**
	 * Specify if HATEOAS support should be applied to the primary ObjectMapper.
	 */
	private boolean applyToPrimaryObjectMapper = true;

	public boolean isApplyToPrimaryObjectMapper() {
		return this.applyToPrimaryObjectMapper;
	}

	public void setApplyToPrimaryObjectMapper(boolean applyToPrimaryObjectMapper) {
		this.applyToPrimaryObjectMapper = applyToPrimaryObjectMapper;
	}

}
