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

package smoketest.armeria.jetty;

import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.healthcheck.HealthChecker;
import com.linecorp.armeria.server.jetty.JettyService;
import com.linecorp.armeria.spring.ArmeriaServerConfigurator;
import org.eclipse.jetty.server.Server;

import org.springframework.boot.web.embedded.jetty.JettyWebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures an Armeria server to redirect the incoming requests to the Jetty instance
 * provided by Spring Boot. It also sets up a {@link HealthChecker} so that it works well
 * with a load balancer.
 */
@Configuration
public class ArmeriaConfiguration {

	/**
	 * Returns a new {@link HealthChecker} that marks the server as unhealthy when Tomcat
	 * becomes unavailable.
	 */
	@Bean
	public HealthChecker jettyHealthChecker(ServletWebServerApplicationContext applicationContext) {
		final Server server = jettyServer(applicationContext).getServer();
		return server::isRunning;
	}

	/**
	 * Returns a new {@link JettyService} that redirects the incoming requests to the
	 * Jetty instance provided by Spring Boot.
	 */
	@Bean
	public JettyService jettyService(ServletWebServerApplicationContext applicationContext) {
		final JettyWebServer jettyWebServer = jettyServer(applicationContext);
		return JettyService.of(jettyWebServer.getServer(), null);
	}

	/**
	 * Returns a new {@link ArmeriaServerConfigurator} that is responsible for configuring
	 * a {@link Server} using the given {@link ServerBuilder}.
	 */
	@Bean
	public ArmeriaServerConfigurator armeriaServiceInitializer(JettyService jettyService) {
		return sb -> sb.serviceUnder("/jetty", jettyService.decorate((delegate, ctx, req) -> {
			ctx.addAdditionalResponseHeader("armeria-forwarded", "true");
			return delegate.serve(ctx, req);
		})).serviceUnder("/armeria", (ctx, req) -> HttpResponse.of("Hello from Armeria!"));
	}

	/**
	 * Extracts a Jetty {@link Server} from Spring webapp context.
	 */
	private static JettyWebServer jettyServer(ServletWebServerApplicationContext applicationContext) {
		return (JettyWebServer) applicationContext.getWebServer();
	}

}
