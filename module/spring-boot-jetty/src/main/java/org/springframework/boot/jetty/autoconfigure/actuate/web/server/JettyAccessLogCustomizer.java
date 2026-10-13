/*
 * Copyright 2012-present the original author or authors.
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
