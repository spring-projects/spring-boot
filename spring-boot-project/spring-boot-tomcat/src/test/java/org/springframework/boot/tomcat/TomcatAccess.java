/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.tomcat;

import java.util.Map;

import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;

/**
 * Helper class to provide public access to package-private methods for testing purposes.
 *
 * @author Andy Wilkinson
 */
public final class TomcatAccess {

	private TomcatAccess() {

	}

	public static Map<Service, Connector[]> getServiceConnectors(TomcatWebServer tomcatWebServer) {
		return tomcatWebServer.getServiceConnectors();
	}

	public static String getStartedLogMessage(TomcatWebServer tomcatWebServer) {
		return tomcatWebServer.getStartedLogMessage();
	}

}
