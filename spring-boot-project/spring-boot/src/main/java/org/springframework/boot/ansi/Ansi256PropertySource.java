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

package org.springframework.boot.ansi;

import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * {@link PropertyResolver} for {@link Ansi256Color.Background} and
 * {@link Ansi256Color.Foreground} elements. Supports properties of the form
 * {@code Ansi256Color.Foreground_N} and {@code Ansi256Color.Background_N} ({@code N} must
 * be between 0 and 255).
 *
 * @author Toshiaki Maki
 * @since 2.2.0
 */
public class Ansi256PropertySource extends PropertySource<AnsiElement> {

	private static final String PREFIX = "Ansi256Color.";

	private static final String FOREGROUND_PREFIX = PREFIX + "Foreground_";

	private static final String BACKGROUND_PREFIX = PREFIX + "Background_";

	/**
	 * Create a new {@link Ansi256PropertySource} instance.
	 * @param name the name of the property source
	 */
	public Ansi256PropertySource(String name) {
		super(name);
	}

	@Override
	public Object getProperty(String name) {
		if (StringUtils.hasLength(name)) {
			if (name.startsWith(FOREGROUND_PREFIX)) {
				final int colorCode = Integer.parseInt(name.substring(FOREGROUND_PREFIX.length()));
				return AnsiOutput.encode(new Ansi256Color.Foreground(colorCode));
			}
			else if (name.startsWith(BACKGROUND_PREFIX)) {
				final int colorCode = Integer.parseInt(name.substring(BACKGROUND_PREFIX.length()));
				return AnsiOutput.encode(new Ansi256Color.Background(colorCode));
			}
		}
		return null;
	}

}
