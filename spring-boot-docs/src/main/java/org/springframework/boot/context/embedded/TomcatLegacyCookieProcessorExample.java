/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.context.embedded;

import org.apache.catalina.Context;
import org.apache.tomcat.util.http.LegacyCookieProcessor;

import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Example configuration for configuring Tomcat with to use {@link LegacyCookieProcessor}.
 *
 * @author Andy Wilkinson
 */
public class TomcatLegacyCookieProcessorExample {

	/**
	 * Configuration class that declares the required
	 * {@link EmbeddedServletContainerCustomizer}.
	 */
	@Configuration
	static class LegacyCookieProcessorConfiguration {

		// tag::customizer[]
		@Bean
		public EmbeddedServletContainerCustomizer cookieProcessorCustomizer() {
			return new EmbeddedServletContainerCustomizer() {

				@Override
				public void customize(ConfigurableEmbeddedServletContainer container) {
					if (container instanceof TomcatEmbeddedServletContainerFactory) {
						((TomcatEmbeddedServletContainerFactory) container)
								.addContextCustomizers(new TomcatContextCustomizer() {

									@Override
									public void customize(Context context) {
										context.setCookieProcessor(
												new LegacyCookieProcessor());
									}

								});
					}
				}

			};
		}
		// end::customizer[]

	}

}
