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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.classpath.ClassPathOverrides;

/**
 * Configure a test class to run its tests with Jetty 10 on the classpath.
 *
 * @author Andy Wilkinson
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnabledForJreRange(min = JRE.JAVA_11)
@ClassPathExclusions({ "jetty-*.jar", "tomcat-embed*.jar", "http2-*.jar" })
@ClassPathOverrides({ "org.slf4j:slf4j-api:1.7.25", "org.eclipse.jetty:jetty-client:10.0.2",
		"org.eclipse.jetty:jetty-io:10.0.2", "org.eclipse.jetty:jetty-server:10.0.2",
		"org.eclipse.jetty:jetty-servlet:10.0.2", "org.eclipse.jetty:jetty-util:10.0.2",
		"org.eclipse.jetty:jetty-webapp:10.0.2", "org.eclipse.jetty.http2:http2-common:10.0.2",
		"org.eclipse.jetty.http2:http2-hpack:10.0.2", "org.eclipse.jetty.http2:http2-http-client-transport:10.0.2",
		"org.eclipse.jetty.http2:http2-server:10.0.2", "org.mortbay.jasper:apache-jsp:8.5.40" })
@interface TestWithJetty10 {

}
