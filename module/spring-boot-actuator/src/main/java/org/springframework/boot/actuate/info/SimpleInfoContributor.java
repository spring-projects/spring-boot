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

package org.springframework.boot.actuate.info;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * A simple {@link InfoContributor} that exposes a single detail.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public class SimpleInfoContributor implements InfoContributor {

	private final String prefix;

	private final @Nullable Object detail;

	public SimpleInfoContributor(String prefix, @Nullable Object detail) {
		Assert.notNull(prefix, "'prefix' must not be null");
		this.prefix = prefix;
		this.detail = detail;
	}

	@Override
	public void contribute(Info.Builder builder) {
		if (this.detail != null) {
			builder.withDetail(this.prefix, this.detail);
		}
	}

}
