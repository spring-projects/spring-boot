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

package org.springframework.boot.tomcat.autoconfigure;

import org.apache.catalina.authenticator.NonLoginAuthenticator;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;

import org.springframework.boot.autoconfigure.preinitialize.BackgroundPreinitializer;

/**
 * {@link BackgroundPreinitializer} for Tomcat.
 *
 * @author Phillip Webb
 */
final class TomcatBackgroundPreinitializer implements BackgroundPreinitializer {

	@Override
	public void preinitialize() throws Exception {
		new Rfc6265CookieProcessor();
		new NonLoginAuthenticator();
	}

}
