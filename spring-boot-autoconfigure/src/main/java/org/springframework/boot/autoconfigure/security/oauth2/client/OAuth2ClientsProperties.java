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

package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.util.HashMap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationProperties;

/**
 * Configuration properties for OAuth 2.0 / OpenID Connect 1.0 client registrations.
 *
 * @author Joe Grandja
 * @since 2.0.0
 * @see ClientRegistrationProperties
 * @see ClientRegistration
 */
@ConfigurationProperties(prefix = OAuth2ClientsProperties.CLIENT_PROPERTY_PREFIX)
public class OAuth2ClientsProperties extends HashMap<String, ClientRegistrationProperties> {

	public static final String CLIENT_PROPERTY_PREFIX = "security.oauth2.client";

}
