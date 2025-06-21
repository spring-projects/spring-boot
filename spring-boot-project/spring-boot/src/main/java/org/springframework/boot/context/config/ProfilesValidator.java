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

package org.springframework.boot.context.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link BindHandler} that validates profile names.
 *
 * @author Sijun Yang
 * @author Phillip Webb
 */
final class ProfilesValidator implements BindHandler {

	private static final String ALLOWED_CHARS = "-_.+@";

	private final boolean validate;

	private ProfilesValidator(boolean validate) {
		this.validate = validate;
	}

	@Override
	public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
		validate(result);
		return result;
	}

	void validate(Object value, Supplier<String> wrappedExceptionMessage) {
		try {
			validate(value);
		}
		catch (IllegalStateException ex) {
			throw new IllegalStateException(wrappedExceptionMessage.get(), ex);
		}
	}

	private void validate(Object value) {
		if (!this.validate) {
			return;
		}
		if (value instanceof Collection<?> list) {
			list.forEach(this::validate);
			return;
		}
		if (value instanceof Map<?, ?> map) {
			map.forEach((k, v) -> validate(v));
			return;
		}
		String profile = (value != null) ? value.toString() : null;
		Assert.state(StringUtils.hasText(profile), "Invalid empty profile");
		for (int i = 0; i < profile.length(); i++) {
			int codePoint = profile.codePointAt(i);
			boolean isAllowedChar = ALLOWED_CHARS.indexOf(codePoint) != -1;
			Assert.state(isAllowedChar || Character.isLetterOrDigit(codePoint),
					() -> "Profile '%s' must contain a letter, digit or allowed char (%s)".formatted(profile,
							Arrays.stream(ALLOWED_CHARS.split("")).collect(Collectors.joining("', '", "'", "'"))));
			Assert.state((i > 0 && i < profile.length() - 1) || Character.isLetterOrDigit(codePoint),
					() -> "Profile '%s' must start and end with a letter or digit".formatted(profile));
		}

	}

	static ProfilesValidator get(Binder binder) {
		return new ProfilesValidator(binder.bind("spring.profiles.validate", Boolean.class).orElse(true));
	}

}
