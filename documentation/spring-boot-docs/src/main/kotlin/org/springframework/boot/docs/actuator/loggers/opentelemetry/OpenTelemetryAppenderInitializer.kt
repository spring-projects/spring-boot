/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.loggers.opentelemetry

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component

@Component
class OpenTelemetryAppenderInitializer(
	private val openTelemetry: OpenTelemetry
) : InitializingBean {

	override fun afterPropertiesSet() = OpenTelemetryAppender.install(openTelemetry)

}
