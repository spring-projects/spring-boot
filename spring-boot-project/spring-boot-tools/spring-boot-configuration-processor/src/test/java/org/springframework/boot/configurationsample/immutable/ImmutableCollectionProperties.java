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

package org.springframework.boot.configurationsample.immutable;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.configurationsample.DefaultValue;

/**
 * Simple immutable properties with collections types and defaults.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("unused")
public class ImmutableCollectionProperties {

	private final List<String> names;

	private final List<Boolean> flags;

	private final List<Duration> durations;

	public ImmutableCollectionProperties(List<String> names, @DefaultValue({ "true", "false" }) List<Boolean> flags,
			@DefaultValue({ "10s", "1m", "1h" }) List<Duration> durations) {
		this.names = names;
		this.flags = flags;
		this.durations = durations;
	}

}
