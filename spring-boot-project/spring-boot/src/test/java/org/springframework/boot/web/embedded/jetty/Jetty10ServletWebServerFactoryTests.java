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

package org.springframework.boot.web.embedded.jetty;

import org.eclipse.jetty.server.handler.ErrorHandler;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JettyServletWebServerFactory} with Jetty 10.
 *
 * @author Andy Wilkinson
 */
@TestWithJetty10
class Jetty10ServletWebServerFactoryTests extends JettyServletWebServerFactoryTests {

	@Test
	@Override
	protected void correctVersionOfJettyUsed() {
		String jettyVersion = ErrorHandler.class.getPackage().getImplementationVersion();
		assertThat(jettyVersion).startsWith("10.0");
	}

	@Test
	@Override
	@Disabled("Jetty 10 does not support User-Agent-based compression")
	protected void noCompressionForUserAgent() {

	}

	@Test
	@Override
	@Disabled("Jetty 10 adds methods to Configuration that we can't mock while compiling against 9")
	protected void jettyConfigurations() {
	}

}
