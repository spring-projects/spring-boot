/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.web.servlet.embeddedcontainer.customizing.samesite;

import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MySameSiteConfiguration class.
 */
@Configuration(proxyBeanMethods = false)
public class MySameSiteConfiguration {

	/**
	 * Returns a CookieSameSiteSupplier that sets the SameSite attribute of cookies to
	 * "Lax" when the cookie name matches the pattern "myapp.*".
	 * @return a CookieSameSiteSupplier that sets the SameSite attribute of cookies to
	 * "Lax" when the cookie name matches the pattern "myapp.*"
	 */
	@Bean
	public CookieSameSiteSupplier applicationCookieSameSiteSupplier() {
		return CookieSameSiteSupplier.ofLax().whenHasNameMatching("myapp.*");
	}

}
