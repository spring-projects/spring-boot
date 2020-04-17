package org.springframework.boot.autoconfigure.rsocket;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.util.RouteMatcher;

/**
 * Callback interface that can be used to customize a RSocketMessageHandler {@link Connector}.
 */
@FunctionalInterface
public interface RSocketMessageHandlerCustomizer {

	RSocketMessageHandler setRouteMatcher(RouteMatcher handler);

}
