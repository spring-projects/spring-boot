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
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.classpath.ClassPathOverrides;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledForJreRange(min = JRE.JAVA_11)
@ClassPathExclusions({ "jetty-*.jar", "tomcat-embed*.jar" })
@ClassPathOverrides({ "org.slf4j:slf4j-api:1.7.25", "org.eclipse.jetty:jetty-io:10.0.0",
		"org.eclipse.jetty:jetty-server:10.0.0", "org.eclipse.jetty:jetty-servlet:10.0.0",
		"org.eclipse.jetty:jetty-util:10.0.0", "org.eclipse.jetty:jetty-webapp:10.0.0",
		"org.eclipse.jetty.http2:http2-common:10.0.0", "org.eclipse.jetty.http2:http2-hpack:10.0.0",
		"org.eclipse.jetty.http2:http2-server:10.0.0", "org.mortbay.jasper:apache-jsp:8.5.40" })
public class Jetty10ServletWebServerFactoryTests extends JettyServletWebServerFactoryTests {

	@Override
	@Test
	protected void correctVersionOfJettyUsed() {
		String jettyVersion = ErrorHandler.class.getPackage().getImplementationVersion();
		assertThat(jettyVersion).startsWith("10.0");
	}

	@Override
	@Disabled("Jetty 10 does not support User-Agent-based compression")
	protected void noCompressionForUserAgent() {

	}

	@Override
	@Disabled("Jetty 10 adds methods to Configuration that we can't mock while compiling against 9")
	protected void jettyConfigurations() throws Exception {
	}

}
