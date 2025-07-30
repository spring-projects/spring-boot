/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.configurationsample.simple;

import org.springframework.boot.configurationsample.ConfigurationProperties;
import org.springframework.boot.configurationsample.DeprecatedConfigurationProperty;
import org.springframework.boot.configurationsample.Name;

/**
 * Configuration properties as record with deprecated property.
 *
 * @param alpha alpha property, deprecated
 * @param bravo bravo property
 * @param charlie charlie property, named, deprecated
 * @author Moritz Halbritter
 */
@ConfigurationProperties("deprecated-record")
public record DeprecatedRecord(String alpha, String bravo, @Name("named.charlie") String charlie) {

	@Deprecated
	@DeprecatedConfigurationProperty(reason = "some-reason")
	public String alpha() {
		return this.alpha;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(reason = "another-reason")
	public String charlie() {
		return this.charlie;
	}
}
