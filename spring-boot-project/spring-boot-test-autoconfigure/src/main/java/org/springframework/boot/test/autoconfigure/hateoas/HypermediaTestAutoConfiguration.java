/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.boot.test.autoconfigure.hateoas;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.codec.CodecsAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.HypermediaMappingInformation;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Greg Turnquist
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter({ CodecsAutoConfiguration.class })
@EnableConfigurationProperties
public class HypermediaTestAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ WebTestClient.class })
	protected static class WebTestClientConfiguration {

		@Bean
		@ConditionalOnMissingBean
		CodecCustomizer hypermediaCodecCustomizer(ObjectProvider<ObjectMapper> mapperProvider,
				List<HypermediaMappingInformation> hypermediaTypes) {
			return codecConfigurer -> {
				Assert.notNull(hypermediaTypes, "HypermediaMappingInformations must not be null!");

				hypermediaTypes.forEach(hypermedia -> {

					ObjectMapper objectMapper = hypermedia
							.configureObjectMapper(mapperProvider.getIfAvailable(ObjectMapper::new).copy());
					MimeType[] mimeTypes = hypermedia.getMediaTypes().toArray(new MimeType[0]);

					codecConfigurer.customCodecs()
							.registerWithDefaultConfig(new Jackson2JsonEncoder(objectMapper, mimeTypes));
					codecConfigurer.customCodecs()
							.registerWithDefaultConfig(new Jackson2JsonDecoder(objectMapper, mimeTypes));
				});
			};
		}

	}

}
