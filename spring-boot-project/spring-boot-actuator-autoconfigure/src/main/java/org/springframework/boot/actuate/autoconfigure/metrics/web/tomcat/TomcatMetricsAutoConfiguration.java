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

package org.springframework.boot.actuate.autoconfigure.metrics.web.tomcat;

import java.util.Collections;

import io.micrometer.core.instrument.binder.tomcat.TomcatMetrics;
import org.apache.catalina.Context;
import org.apache.catalina.Manager;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link TomcatMetrics}.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass({ TomcatMetrics.class, Manager.class })
public class TomcatMetricsAutoConfiguration {

	private volatile Context context;

	@Bean
	@ConditionalOnMissingBean
	public TomcatMetrics tomcatMetrics() {
		return new TomcatMetrics((this.context != null) ? this.context.getManager() : null, Collections.emptyList());
	}

	@Bean
	@ConditionalOnWebApplication(type = Type.SERVLET)
	public WebServerFactoryCustomizer<TomcatServletWebServerFactory> contextCapturingServletTomcatCustomizer() {
		return (tomcatFactory) -> tomcatFactory.addContextCustomizers(this::setContext);
	}

	@Bean
	@ConditionalOnWebApplication(type = Type.REACTIVE)
	public WebServerFactoryCustomizer<TomcatReactiveWebServerFactory> contextCapturingReactiveTomcatCustomizer() {
		return (tomcatFactory) -> tomcatFactory.addContextCustomizers(this::setContext);
	}

	private void setContext(Context context) {
		this.context = context;
	}

}
