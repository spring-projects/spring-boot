/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.hateoas;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.client.LinkDiscoverers;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.hateoas.config.HypermediaMappingInformation;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.plugin.core.Plugin;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring HATEOAS's
 * {@link EnableHypermediaSupport @EnableHypermediaSupport}.
 *
 * @author Roy Clarkson
 * @author Oliver Gierke
 * @author Andy Wilkinson
 * @author Greg Turnquist
 * @since 1.1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ EntityModel.class, RequestMapping.class, RequestMappingHandlerAdapter.class, Plugin.class })
@ConditionalOnWebApplication
@AutoConfigureAfter({ WebMvcAutoConfiguration.class, JacksonAutoConfiguration.class,
		HttpMessageConvertersAutoConfiguration.class, RepositoryRestMvcAutoConfiguration.class })
@EnableConfigurationProperties(HateoasProperties.class)
@Import(HypermediaHttpMessageConverterConfiguration.class)
public class HypermediaAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(LinkDiscoverers.class)
	@ConditionalOnClass(ObjectMapper.class)
	@EnableHypermediaSupport(type = HypermediaType.HAL)
	protected static class HypermediaConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(WebClient.class)
	protected static class WebClientHypermediaConfiguration {

		@Bean
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

	@Bean
	WebConverters springBootWebConverters(ObjectProvider<ObjectMapper> mapper,
			List<HypermediaMappingInformation> information) {
		return WebConverters.of(mapper.getIfUnique(ObjectMapper::new), information);
	}

	@Bean
	@ConditionalOnClass(RestTemplate.class)
	RestTemplateCustomizer hypermediaRestTemplatCustomizer(ObjectProvider<WebConverters> converters) {
		return restTemplate -> {
			restTemplate.setMessageConverters(converters.getObject().and(restTemplate.getMessageConverters()));
		};
	}

}
