/*
 * Copyright 2012-2015 the original author or authors.
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

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * Configuration for {@link HttpMessageConverter HttpMessageConverters} when hypermedia is
 * enabled.
 *
 * @author Andy Wilkinson
 */
public class HypermediaHttpMessageConverterConfiguration {

	@Bean
	@ConditionalOnProperty(prefix = "spring.hateoas", name = "use-hal-as-default-json-media-type", matchIfMissing = true)
	public static HalMessageConverterSupportedMediaTypesCustomizer halMessageConverterSupportedMediaTypeCustomizer() {
		return new HalMessageConverterSupportedMediaTypesCustomizer();
	}

	/**
	 * Updates any {@link TypeConstrainedMappingJackson2HttpMessageConverter}s to support
	 * {@code application/json} in addition to {@code application/hal+json}. Cannot be a
	 * {@link BeanPostProcessor} as processing must be performed after
	 * {@code Jackson2ModuleRegisteringBeanPostProcessor} has registered the converter and
	 * it is unordered.
	 */
	private static class HalMessageConverterSupportedMediaTypesCustomizer
			implements BeanFactoryAware {

		private volatile BeanFactory beanFactory;

		@PostConstruct
		public void customizedSupportedMediaTypes() {
			if (this.beanFactory instanceof ListableBeanFactory) {
				Map<String, RequestMappingHandlerAdapter> handlerAdapters = ((ListableBeanFactory) this.beanFactory)
						.getBeansOfType(RequestMappingHandlerAdapter.class);
				for (Entry<String, RequestMappingHandlerAdapter> entry : handlerAdapters
						.entrySet()) {
					RequestMappingHandlerAdapter handlerAdapter = entry.getValue();
					for (HttpMessageConverter<?> converter : handlerAdapter
							.getMessageConverters()) {
						if (converter instanceof TypeConstrainedMappingJackson2HttpMessageConverter) {
							((TypeConstrainedMappingJackson2HttpMessageConverter) converter)
									.setSupportedMediaTypes(
											Arrays.asList(MediaTypes.HAL_JSON,
													MediaType.APPLICATION_JSON));
						}
					}

				}
			}
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

	}

}
