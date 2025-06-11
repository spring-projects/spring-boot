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

package org.springframework.boot.jetty.autoconfigure.actuate.web.server;

import java.io.File;

import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.RequestLogWriter;
import org.eclipse.jetty.server.Server;

import org.springframework.boot.actuate.autoconfigure.web.server.AccessLogCustomizer;
import org.springframework.boot.jetty.ConfigurableJettyWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.util.StringUtils;

/**
 * {@link AccessLogCustomizer} for Jetty.
 *
 * @author Andy Wilkinson
 */
class JettyAccessLogCustomizer extends AccessLogCustomizer<ConfigurableJettyWebServerFactory>
		implements WebServerFactoryCustomizer<ConfigurableJettyWebServerFactory> {

	JettyAccessLogCustomizer(JettyManagementServerProperties properties) {
		super(properties.getAccesslog().getPrefix());
	}

	@Override
	public void customize(ConfigurableJettyWebServerFactory factory) {
		factory.addServerCustomizers(this::customizeServer);
	}

	private void customizeServer(Server server) {
		RequestLog requestLog = server.getRequestLog();
		if (requestLog instanceof CustomRequestLog customRequestLog) {
			customizeRequestLog(customRequestLog);
		}
	}

	private void customizeRequestLog(CustomRequestLog requestLog) {
		if (requestLog.getWriter() instanceof RequestLogWriter requestLogWriter) {
			customizeRequestLogWriter(requestLogWriter);
		}
	}

	private void customizeRequestLogWriter(RequestLogWriter writer) {
		String filename = writer.getFileName();
		if (StringUtils.hasLength(filename)) {
			File file = new File(filename);
			file = new File(file.getParentFile(), customizePrefix(file.getName()));
			writer.setFilename(file.getPath());
		}
	}

}
