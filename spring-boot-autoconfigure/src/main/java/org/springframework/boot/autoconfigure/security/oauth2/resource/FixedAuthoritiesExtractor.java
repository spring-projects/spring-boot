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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link AuthoritiesExtractor}. Extracts the authorities from
 * the map with the key {@code authorities}. If no such value exists, a single
 * {@code ROLE_USER} authority is returned.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
public class FixedAuthoritiesExtractor implements AuthoritiesExtractor {

	private static final String AUTHORITIES = "authorities";

	private static final String[] AUTHORITY_KEYS = { "authority", "role", "value" };

	@Override
	public List<GrantedAuthority> extractAuthorities(Map<String, Object> map) {
		String authorities = "ROLE_USER";
		if (map.containsKey(AUTHORITIES)) {
			authorities = asAuthorities(map.get(AUTHORITIES));
		}
		return AuthorityUtils.commaSeparatedStringToAuthorityList(authorities);
	}

	private String asAuthorities(Object object) {
		List<Object> authorities = new ArrayList<Object>();
		if (object instanceof Collection) {
			Collection<?> collection = (Collection<?>) object;
			object = collection.toArray(new Object[0]);
		}
		if (ObjectUtils.isArray(object)) {
			Object[] array = (Object[]) object;
			for (Object value : array) {
				if (value instanceof String) {
					authorities.add(value);
				}
				else if (value instanceof Map) {
					authorities.add(asAuthority((Map<?, ?>) value));
				}
				else {
					authorities.add(value);
				}
			}
			return StringUtils.collectionToCommaDelimitedString(authorities);
		}
		return object.toString();
	}

	private Object asAuthority(Map<?, ?> map) {
		if (map.size() == 1) {
			return map.values().iterator().next();
		}
		for (String key : AUTHORITY_KEYS) {
			if (map.containsKey(key)) {
				return map.get(key);
			}
		}
		return map;
	}

}
