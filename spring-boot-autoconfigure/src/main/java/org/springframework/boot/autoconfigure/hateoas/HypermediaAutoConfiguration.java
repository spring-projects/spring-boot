/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.hateoas;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.LinkDiscoverers;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.config.EnableEntityLinks;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.plugin.core.Plugin;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring HATEOAS's
 * {@link EnableHypermediaSupport}.
 *
 * @author Roy Clarkson
 * @author Oliver Gierke
 * @author Andy Wilkinson
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass({ Resource.class, RequestMapping.class, Plugin.class })
@ConditionalOnWebApplication
@AutoConfigureAfter({ WebMvcAutoConfiguration.class, JacksonAutoConfiguration.class })
public class HypermediaAutoConfiguration {

	@Configuration
	@ConditionalOnMissingBean(LinkDiscoverers.class)
	@EnableHypermediaSupport(type = HypermediaType.HAL)
	protected static class HypermediaConfiguration {

		@ConditionalOnClass({ Jackson2ObjectMapperBuilder.class, ObjectMapper.class })
		protected static class HalObjectMapperConfiguration {

			@Autowired(required = false)
			private Jackson2ObjectMapperBuilder objectMapperBuilder;

			@Bean
			public BeanPostProcessor halObjectMapperConfigurer() {
				return new BeanPostProcessor() {

					@Override
					public Object postProcessAfterInitialization(Object bean,
							String beanName) throws BeansException {
						if (HalObjectMapperConfiguration.this.objectMapperBuilder != null
								&& bean instanceof ObjectMapper
								&& "_halObjectMapper".equals(beanName)) {
							HalObjectMapperConfiguration.this.objectMapperBuilder
									.configure((ObjectMapper) bean);
						}
						return bean;
					}

					@Override
					public Object postProcessBeforeInitialization(Object bean,
							String beanName) throws BeansException {
						return bean;
					}

				};
			}
		}
	}

	@Configuration
	@ConditionalOnMissingBean(EntityLinks.class)
	@EnableEntityLinks
	protected static class EntityLinksConfiguration {
	}

}
