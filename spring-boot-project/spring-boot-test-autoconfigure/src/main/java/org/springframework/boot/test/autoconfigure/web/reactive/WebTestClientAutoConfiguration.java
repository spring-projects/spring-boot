/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.autoconfigure.web.reactive;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.codec.CodecsAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configuration for {@link WebTestClient}.
 *
 * @author Stephane Nicoll
 * @author Roman Zaynetdinov
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass({ WebClient.class, WebTestClient.class })
@AutoConfigureAfter(CodecsAutoConfiguration.class)
public class WebTestClientAutoConfiguration {

	private final ApplicationContext context;

	WebTestClientAutoConfiguration(ApplicationContext context) {
		this.context = context;
	}

	@Bean
	@ConditionalOnMissingBean
	public WebTestClient webTestClient(List<WebTestClientBuilderCustomizer> customizers) {
		WebTestClient.Builder builder = WebTestClient
				.bindToApplicationContext(this.context).configureClient();
		for (WebTestClientBuilderCustomizer customizer : customizers) {
			customizer.customize(builder);
		}
		return builder.build();
	}

	@Bean
	public SpringBootWebTestClientBuilderCustomizer springBootWebTestClientBuilderCustomizer() {
		return new SpringBootWebTestClientBuilderCustomizer(this.context);
	}

}
