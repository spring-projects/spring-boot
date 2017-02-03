/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.env;

import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A canonical property name composed of elements separated in dots. The last dot
 * separates the prefix from the property name. Names are alpha-numeric ({@code a-z}
 * {@code 0-9}) and must be lowercase, the only other characters permitted are {@code [}
 * and {@code]} which are used to indicate indexes. A property name must start with a
 * letter.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public final class CanonicalPropertyName {

	private static final Pattern NAME_PATTERN = Pattern.compile("[a-z]([a-z0-9\\[\\]])*");

	private final String name;

	public CanonicalPropertyName(String name) {
		Assert.hasLength(name, "Name must not be empty");
		Assert.isTrue(isValid(name), "Name \"" + name + "\" is not canonical");
		this.name = name;
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		return this.name.equals(((CanonicalPropertyName) obj).name);
	}

	@Override
	public String toString() {
		return this.name;
	}

	public static boolean isValid(String name) {
		if (!StringUtils.hasLength(name) || name.startsWith(".") || name.endsWith(".")
				|| name.contains("..")) {
			return false;
		}
		return isValid(new StringTokenizer(name, "."));
	}

	private static boolean isValid(StringTokenizer tokens) {
		while (tokens.hasMoreTokens()) {
			if (!isTokenValid(tokens.nextToken())) {
				return false;
			}
		}
		return true;
	}

	private static boolean isTokenValid(String token) {
		return !StringUtils.isEmpty(token) && NAME_PATTERN.matcher(token).matches();
	}

}
