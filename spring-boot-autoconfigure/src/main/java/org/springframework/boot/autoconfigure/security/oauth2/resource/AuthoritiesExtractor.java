/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.resource;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;

/**
 * Strategy used by {@link UserInfoTokenServices} to extract authorities from the resource
 * server's response.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
public interface AuthoritiesExtractor {

	/**
	 * Extract the authorities from the resource server's response.
	 * @param map the response
	 * @return the extracted authorities
	 */
	List<GrantedAuthority> extractAuthorities(Map<String, Object> map);

}
