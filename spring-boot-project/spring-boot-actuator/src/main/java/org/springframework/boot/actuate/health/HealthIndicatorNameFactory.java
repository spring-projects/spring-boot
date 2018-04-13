/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.util.Locale;
import java.util.function.Function;

/**
 * Generate a sensible health indicator name based on its bean name.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class HealthIndicatorNameFactory implements Function<String, String> {

	@Override
	public String apply(String name) {
		int index = name.toLowerCase(Locale.ENGLISH).indexOf("healthindicator");
		if (index > 0) {
			return name.substring(0, index);
		}
		return name;
	}

}
