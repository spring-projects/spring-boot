/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * Configuration for {@link HttpMessageConverter HttpMessageConverters} when hypermedia is
 * enabled.
 *
 * @author Andy Wilkinson
 */
@Configuration
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
		public void configureHttpMessageConverters() {
			if (this.beanFactory instanceof ListableBeanFactory) {
				configureHttpMessageConverters(((ListableBeanFactory) this.beanFactory)
						.getBeansOfType(RequestMappingHandlerAdapter.class).values());
			}
		}

		private void configureHttpMessageConverters(
				Collection<RequestMappingHandlerAdapter> handlerAdapters) {
			for (RequestMappingHandlerAdapter handlerAdapter : handlerAdapters) {
				for (HttpMessageConverter<?> messageConverter : handlerAdapter
						.getMessageConverters()) {
					configureHttpMessageConverter(messageConverter);
				}
			}
		}

		private void configureHttpMessageConverter(HttpMessageConverter<?> converter) {
			if (converter instanceof TypeConstrainedMappingJackson2HttpMessageConverter) {
				List<MediaType> supportedMediaTypes = new ArrayList<>(
						converter.getSupportedMediaTypes());
				if (!supportedMediaTypes.contains(MediaType.APPLICATION_JSON)) {
					supportedMediaTypes.add(MediaType.APPLICATION_JSON);
				}
				((AbstractHttpMessageConverter<?>) converter)
						.setSupportedMediaTypes(supportedMediaTypes);
			}
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

	}

}
