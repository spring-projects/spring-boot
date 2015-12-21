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

package org.springframework.boot.bind;

import org.springframework.core.env.PropertySource;

/**
 * The origin of a property, specifically its source and its name before any prefix was
 * removed.
 *
 * @author Andy Wilkinson
 * @since 1.3.0
 */
public class PropertyOrigin {

	private final PropertySource<?> source;

	private final String name;

	PropertyOrigin(PropertySource<?> source, String name) {
		this.name = name;
		this.source = source;
	}

	public PropertySource<?> getSource() {
		return this.source;
	}

	public String getName() {
		return this.name;
	}

}
