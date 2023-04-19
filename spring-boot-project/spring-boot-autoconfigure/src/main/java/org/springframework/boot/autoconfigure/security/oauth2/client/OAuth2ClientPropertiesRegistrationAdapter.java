/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.util.Map;

import org.springframework.security.oauth2.client.registration.ClientRegistration;

/**
 * Adapter class to convert {@link OAuth2ClientProperties} to a
 * {@link ClientRegistration}.
 *
 * @author Phillip Webb
 * @author Thiago Hirata
 * @author Madhura Bhave
 * @author MyeongHyeon Lee
 * @since 2.1.0
 * @deprecated since 3.1.0 for removal in 3.3.0 in favor of
 * {@link OAuth2ClientPropertiesMapper}
 */
@Deprecated(since = "3.1.0", forRemoval = true)
public final class OAuth2ClientPropertiesRegistrationAdapter {

	private OAuth2ClientPropertiesRegistrationAdapter() {
	}

	public static Map<String, ClientRegistration> getClientRegistrations(OAuth2ClientProperties properties) {
		return new OAuth2ClientPropertiesMapper(properties).asClientRegistrations();
	}

}
