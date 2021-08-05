/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.mediatype.hal.HalConfiguration;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * Configuration for {@link HttpMessageConverter HttpMessageConverters} when hypermedia is
 * enabled.
 *
 * @author Andy Wilkinson
 * @since 1.3.0
 * @deprecated since 2.5.0 for removal in 2.7.0 in favor of a {@link HalConfiguration}
 * bean
 */
@Deprecated
@Configuration(proxyBeanMethods = false)
public class HypermediaHttpMessageConverterConfiguration {

	@Bean
	@ConditionalOnProperty(prefix = "spring.hateoas", name = "use-hal-as-default-json-media-type",
			matchIfMissing = true)
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
			implements BeanFactoryAware, InitializingBean {

		private volatile BeanFactory beanFactory;

		@Override
		public void afterPropertiesSet() {
			if (this.beanFactory instanceof ListableBeanFactory) {
				configureHttpMessageConverters(((ListableBeanFactory) this.beanFactory)
						.getBeansOfType(RequestMappingHandlerAdapter.class).values());
			}
		}

		private void configureHttpMessageConverters(Collection<RequestMappingHandlerAdapter> handlerAdapters) {
			for (RequestMappingHandlerAdapter handlerAdapter : handlerAdapters) {
				for (HttpMessageConverter<?> messageConverter : handlerAdapter.getMessageConverters()) {
					configureHttpMessageConverter(messageConverter);
				}
			}
		}

		private void configureHttpMessageConverter(HttpMessageConverter<?> converter) {
			if (converter instanceof TypeConstrainedMappingJackson2HttpMessageConverter) {
				List<MediaType> supportedMediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
				if (!supportedMediaTypes.contains(MediaType.APPLICATION_JSON)) {
					supportedMediaTypes.add(MediaType.APPLICATION_JSON);
				}
				((AbstractHttpMessageConverter<?>) converter).setSupportedMediaTypes(supportedMediaTypes);
			}
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

	}

}
