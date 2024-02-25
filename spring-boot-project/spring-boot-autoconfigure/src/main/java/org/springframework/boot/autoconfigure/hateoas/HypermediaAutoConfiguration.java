/*
 * Copyright 2012-2022 the original author or authors.
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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.client.LinkDiscoverers;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.hateoas.mediatype.hal.HalConfiguration;
import org.springframework.http.MediaType;
import org.springframework.plugin.core.Plugin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring HATEOAS's
 * {@link EnableHypermediaSupport @EnableHypermediaSupport}.
 *
 * @author Roy Clarkson
 * @author Oliver Gierke
 * @author Andy Wilkinson
 * @since 1.1.0
 */
@AutoConfiguration(after = { WebMvcAutoConfiguration.class, JacksonAutoConfiguration.class,
		HttpMessageConvertersAutoConfiguration.class, RepositoryRestMvcAutoConfiguration.class })
@ConditionalOnClass({ EntityModel.class, RequestMapping.class, RequestMappingHandlerAdapter.class, Plugin.class })
@ConditionalOnWebApplication
@EnableConfigurationProperties(HateoasProperties.class)
public class HypermediaAutoConfiguration {

	/**
     * Creates a HalConfiguration bean if no other bean of the same type is present in the application context.
     * This bean is conditionally created based on the presence of the com.fasterxml.jackson.databind.ObjectMapper class
     * and the value of the spring.hateoas.use-hal-as-default-json-media-type property.
     * If the property is not present, the bean is created by default.
     * The created bean is configured with the MediaType.APPLICATION_JSON media type.
     *
     * @return the created HalConfiguration bean
     */
    @Bean
	@ConditionalOnMissingBean
	@ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
	@ConditionalOnProperty(prefix = "spring.hateoas", name = "use-hal-as-default-json-media-type",
			matchIfMissing = true)
	HalConfiguration applicationJsonHalConfiguration() {
		return new HalConfiguration().withMediaType(MediaType.APPLICATION_JSON);
	}

	/**
     * HypermediaConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(LinkDiscoverers.class)
	@ConditionalOnClass(ObjectMapper.class)
	@EnableHypermediaSupport(type = HypermediaType.HAL)
	protected static class HypermediaConfiguration {

	}

}
