/*
 * Copyright 2022-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.pulsar;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.pulsar.common.util.ObjectMapperFactory;

import org.springframework.util.CollectionUtils;

/**
 * Utility methods for Pulsar authentication parameters.
 *
 * @author Alexander Preuß
 */
final class AuthParameterUtils {

	private static final Pattern KEBAB_CASE_PATTERN = Pattern.compile("-(.)");

	private AuthParameterUtils() {

	}

	private static String convertKebabCaseToCamelCase(String kebabString) {
		return KEBAB_CASE_PATTERN.matcher(kebabString).replaceAll((mr) -> mr.group(1).toUpperCase());
	}

	private static Map<String, String> convertWellKnownLowerCaseKeysToCamelCase(Map<String, String> params) {
		return params.entrySet()
			.stream()
			.collect(Collectors.toMap((entry) -> WellKnownAuthParameters.toCamelCaseKey(entry.getKey()),
					Map.Entry::getValue));
	}

	private static Map<String, String> convertKebabCaseKeysToCamelCase(Map<String, String> params) {
		return params.entrySet()
			.stream()
			.collect(Collectors.toMap((entry) -> convertKebabCaseToCamelCase(entry.getKey()), Map.Entry::getValue));
	}

	static String maybeConvertToEncodedParamString(Map<String, String> params) {
		if (CollectionUtils.isEmpty(params)) {
			return null;
		}
		// env vars are bound like this ISSUER_ID -> issuerid, have to be camel-cased to
		// work
		params = convertWellKnownLowerCaseKeysToCamelCase(params);
		params = convertKebabCaseKeysToCamelCase(params);
		params = new TreeMap<>(params); // sort keys for testing and readability
		try {
			return ObjectMapperFactory.create().writeValueAsString(params);
		}
		catch (Exception ex) {
			throw new RuntimeException("Could not convert parameters to encoded string", ex);
		}
	}

}
