/*
 * Copyright 2014-2015 the original author or authors.
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

import org.springframework.security.oauth2.client.OAuth2RestTemplate;

/**
 * Callback for customizing the rest template used to fetch user details if authentication
 * is done via OAuth2 access tokens. The default should be fine for most providers, but
 * occasionally you might need to add additional interceptors, or change the request
 * authenticator (which is how the token gets attached to outgoing requests). The rest
 * template that is being customized here is <i>only</i> used internally to carry out
 * authentication (in the SSO or Resource Server use cases).
 * 
 * @author Dave Syer
 *
 */
public interface UserInfoRestTemplateCustomizer {

	/**
	 * Customize the rest template before it is initialized.
	 * 
	 * @param template the rest template
	 */
	void customize(OAuth2RestTemplate template);

}
