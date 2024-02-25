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

package org.springframework.boot.actuate.info;

import org.springframework.util.Assert;

/**
 * A simple {@link InfoContributor} that exposes a single detail.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public class SimpleInfoContributor implements InfoContributor {

	private final String prefix;

	private final Object detail;

	/**
     * Constructs a new SimpleInfoContributor with the specified prefix and detail.
     * 
     * @param prefix the prefix to be used for the contributor
     * @param detail the detail object to be associated with the contributor
     * @throws IllegalArgumentException if the prefix is null
     */
    public SimpleInfoContributor(String prefix, Object detail) {
		Assert.notNull(prefix, "Prefix must not be null");
		this.prefix = prefix;
		this.detail = detail;
	}

	/**
     * Contributes additional information to the provided Info.Builder object.
     * If the detail is not null, it adds the detail to the builder with the specified prefix.
     * 
     * @param builder the Info.Builder object to contribute to
     */
    @Override
	public void contribute(Info.Builder builder) {
		if (this.detail != null) {
			builder.withDetail(this.prefix, this.detail);
		}
	}

}
