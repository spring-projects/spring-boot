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

package org.springframework.boot.actuate.info;

import java.util.Map;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * An {@link InfoContributor} that provides all environment entries prefixed with info.
 *
 * @author Meang Akira Tanaka
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @since 1.4.0
 */
public class EnvironmentInfoContributor implements InfoContributor {

	private static final Bindable<Map<String, Object>> STRING_OBJECT_MAP = Bindable
			.mapOf(String.class, Object.class);

	private final ConfigurableEnvironment environment;

	public EnvironmentInfoContributor(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	@Override
	public void contribute(Info.Builder builder) {
		Binder binder = Binder.get(this.environment);
		binder.bind("info", STRING_OBJECT_MAP).ifBound(builder::withDetails);
	}

}
