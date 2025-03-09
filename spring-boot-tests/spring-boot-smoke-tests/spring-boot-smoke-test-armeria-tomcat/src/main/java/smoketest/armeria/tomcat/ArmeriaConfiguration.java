/*
 * Copyright 2025 the original author or authors.
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

package smoketest.armeria.tomcat;

import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.healthcheck.HealthChecker;
import com.linecorp.armeria.server.tomcat.TomcatService;
import com.linecorp.armeria.spring.ArmeriaServerConfigurator;
import org.apache.catalina.connector.Connector;

import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures an Armeria {@link Server} to redirect the incoming requests to the Tomcat
 * instance provided by Spring Boot. It also sets up a {@link HealthChecker} so that it
 * works well with a load balancer.
 *
 * @author Dogac Eldenk
 */
@Configuration
public class ArmeriaConfiguration {

	/**
	 * Extracts a Tomcat {@link Connector} from Spring webapp context.
	 */
	public static Connector getConnector(ServletWebServerApplicationContext applicationContext) {
		final TomcatWebServer container = (TomcatWebServer) applicationContext.getWebServer();

		// Start the container to make sure all connectors are available.
		container.start();
		return container.getTomcat().getConnector();
	}

	/**
	 * Returns a new {@link HealthChecker} that marks the server as unhealthy when Tomcat
	 * becomes unavailable.
	 */
	@Bean
	public HealthChecker tomcatConnectorHealthChecker(ServletWebServerApplicationContext applicationContext) {
		final Connector connector = getConnector(applicationContext);
		return () -> connector.getState().isAvailable();
	}

	/**
	 * Returns a new {@link TomcatService} that redirects the incoming requests to the
	 * Tomcat instance provided by Spring Boot.
	 */
	@Bean
	public TomcatService tomcatService(ServletWebServerApplicationContext applicationContext) {
		return TomcatService.of(getConnector(applicationContext));
	}

	/**
	 * Returns a new {@link ArmeriaServerConfigurator} that is responsible for configuring
	 * a {@link Server} using the given {@link ServerBuilder}.
	 */
	@Bean
	public ArmeriaServerConfigurator armeriaServiceInitializer(TomcatService tomcatService) {

		return sb -> sb.serviceUnder("/tomcat", tomcatService.decorate((delegate, ctx, req) -> {
			ctx.addAdditionalResponseHeader("armeria-forwarded", "true");
			return delegate.serve(ctx, req);
		})).serviceUnder("/armeria", (ctx, req) -> HttpResponse.of("Hello from Armeria!"));

	}

}
