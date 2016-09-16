/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.resource;

import java.util.Map;

/**
 * Default implementation of {@link PrincipalExtractor}. Extracts the principal from the
 * map with well known keys.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
public class FixedPrincipalExtractor implements PrincipalExtractor {

	private static final String[] PRINCIPAL_KEYS = new String[] { "user", "username",
			"userid", "user_id", "login", "id", "name" };

	@Override
	public Object extractPrincipal(Map<String, Object> map) {
		for (String key : PRINCIPAL_KEYS) {
			if (map.containsKey(key)) {
				return map.get(key);
			}
		}
		return null;
	}

}
