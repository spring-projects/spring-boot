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

package org.springframework.boot.config.web;

import javax.servlet.Servlet;

import org.apache.catalina.startup.Tomcat;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Loader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.config.EnableAutoConfiguration;
import org.springframework.boot.config.web.EmbeddedServletContainerAutoConfiguration.EmbeddedServletContainerCustomizerBeanPostProcessorRegistrar;
import org.springframework.boot.strap.context.condition.ConditionalOnClass;
import org.springframework.boot.strap.context.condition.ConditionalOnMissingBean;
import org.springframework.boot.strap.context.condition.SearchStrategy;
import org.springframework.boot.strap.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.strap.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.strap.context.embedded.ServletContextInitializer;
import org.springframework.boot.strap.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.strap.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for an embedded servlet containers.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Configuration
@Import(EmbeddedServletContainerCustomizerBeanPostProcessorRegistrar.class)
public class EmbeddedServletContainerAutoConfiguration {

	/**
	 * Add the {@link DispatcherServlet} unless the user has defined their own
	 * {@link ServletContextInitializer}s.
	 */
	@ConditionalOnClass(DispatcherServlet.class)
	public static class DispatcherServletConfiguration {

		@Bean
		@ConditionalOnMissingBean(value = { ServletContextInitializer.class,
				Servlet.class }, search = SearchStrategy.CURRENT)
		public DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}
	}

	/**
	 * Nested configuration for if Tomcat is being used.
	 */
	@Configuration
	@ConditionalOnClass({ Servlet.class, Tomcat.class })
	@ConditionalOnMissingBean(value = EmbeddedServletContainerFactory.class, search = SearchStrategy.CURRENT)
	public static class EmbeddedTomcat {

		@Bean
		public TomcatEmbeddedServletContainerFactory tomcatEmbeddedServletContainerFactory() {
			return new TomcatEmbeddedServletContainerFactory();
		}

	}

	/**
	 * Nested configuration if Jetty is being used.
	 */
	@Configuration
	@ConditionalOnClass({ Servlet.class, Server.class, Loader.class })
	@ConditionalOnMissingBean(value = EmbeddedServletContainerFactory.class, search = SearchStrategy.CURRENT)
	public static class EmbeddedJetty {

		@Bean
		public JettyEmbeddedServletContainerFactory jettyEmbeddedServletContainerFactory() {
			return new JettyEmbeddedServletContainerFactory();
		}

	}

	/**
	 * Registers a {@link EmbeddedServletContainerCustomizerBeanPostProcessor}. Registered
	 * via {@link ImportBeanDefinitionRegistrar} for early registration.
	 */
	public static class EmbeddedServletContainerCustomizerBeanPostProcessorRegistrar
			implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

		private ConfigurableListableBeanFactory beanFactory;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			if (beanFactory instanceof ConfigurableListableBeanFactory) {
				this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
			}
		}

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			if (this.beanFactory != null
					&& this.beanFactory.getBeansOfType(
							EmbeddedServletContainerCustomizerBeanPostProcessor.class)
							.size() == 0) {
				BeanDefinition beanDefinition = new RootBeanDefinition(
						EmbeddedServletContainerCustomizerBeanPostProcessor.class);
				registry.registerBeanDefinition(
						"embeddedServletContainerCustomizerBeanPostProcessor",
						beanDefinition);
			}
		}
	}
}
