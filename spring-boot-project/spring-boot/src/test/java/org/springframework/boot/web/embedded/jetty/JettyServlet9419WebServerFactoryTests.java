/*
 * Copyright 2012-2019 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.classpath.ClassPathOverrides;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JettyServletWebServerFactory} with Jetty 9.4.19.
 *
 * @author Phillip Webb
 */
@ClassPathExclusions({ "jetty-*.jar", "tomcat-embed*.jar" })
@ClassPathOverrides({ "org.eclipse.jetty:jetty-io:9.4.19.v20190610", "org.eclipse.jetty:jetty-server:9.4.19.v20190610",
		"org.eclipse.jetty:jetty-servlet:9.4.19.v20190610", "org.eclipse.jetty:jetty-util:9.4.19.v20190610",
		"org.eclipse.jetty:jetty-webapp:9.4.19.v20190610", "org.mortbay.jasper:apache-jsp:8.5.40" })
class JettyServlet9419WebServerFactoryTests extends AbstractJettyServletWebServerFactoryTests {

	@Test
	void correctVersionOfJettyUsed() {
		assertThat(JettyEmbeddedErrorHandler.ERROR_PAGE_FOR_METHOD_AVAILABLE).isFalse();
	}

}
