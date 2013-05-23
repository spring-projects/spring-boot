/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.bootstrap.autoconfigure.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.Servlet;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.bootstrap.context.annotation.ConditionalOnClass;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.bootstrap.context.embedded.ConfigurableEmbeddedServletContainerFactory;
import org.springframework.bootstrap.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link EmbeddedServletContainerCustomizer}.
 * 
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ Servlet.class, EmbeddedServletContainerCustomizer.class })
public class EmbeddedContainerCustomizerConfiguration {

	@Bean
	public BeanPostProcessor embeddedContainerCustomizerBeanPostProcessor(
			ListableBeanFactory beanFactory) {
		// Look these up, not autowired because we don't want the ones from the parent
		// context
		Collection<EmbeddedServletContainerCustomizer> customizers = beanFactory
				.getBeansOfType(EmbeddedServletContainerCustomizer.class).values();
		return new EmbeddedContainerCustomizerBeanPostProcessor(customizers);
	}

	private static final class EmbeddedContainerCustomizerBeanPostProcessor implements
			BeanPostProcessor {

		private List<EmbeddedServletContainerCustomizer> customizers;

		public EmbeddedContainerCustomizerBeanPostProcessor(
				Collection<EmbeddedServletContainerCustomizer> customizers) {
			final List<EmbeddedServletContainerCustomizer> list = new ArrayList<EmbeddedServletContainerCustomizer>(
					customizers);
			Collections.sort(list, new AnnotationAwareOrderComparator());
			this.customizers = list;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean instanceof ConfigurableEmbeddedServletContainerFactory) {
				ConfigurableEmbeddedServletContainerFactory factory = (ConfigurableEmbeddedServletContainerFactory) bean;
				for (EmbeddedServletContainerCustomizer customizer : this.customizers) {
					customizer.customize(factory);
				}
			}
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {
			return bean;
		}
	}

}
