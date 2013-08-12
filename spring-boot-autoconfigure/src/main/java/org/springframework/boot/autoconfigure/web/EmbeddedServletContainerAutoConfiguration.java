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

package org.springframework.boot.autoconfigure.web;

import java.util.Arrays;

import javax.servlet.Servlet;

import org.apache.catalina.startup.Tomcat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Loader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionLogUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration.EmbeddedServletContainerCustomizerBeanPostProcessorRegistrar;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
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

	/*
	 * The bean name for a DispatcherServlet that will be mapped to the root URL "/"
	 */
	public static final String DEFAULT_DISPATCHER_SERVLET_BEAN_NAME = "dispatcherServlet";

	/**
	 * Add the {@link DispatcherServlet} unless the user has defined their own
	 * {@link ServletContextInitializer}s.
	 */
	@ConditionalOnClass(DispatcherServlet.class)
	public static class DispatcherServletConfiguration {

		@Bean(name = DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
		@Conditional(DefaultServletCondition.class)
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

	private static class DefaultServletCondition implements Condition {

		private static Log logger = LogFactory.getLog(DefaultServletCondition.class);

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {

			String checking = ConditionLogUtils.getPrefix(logger, metadata);

			ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
			String[] beans = beanFactory.getBeanNamesForType(DispatcherServlet.class,
					false, false);
			if (beans.length == 0) {
				if (logger.isDebugEnabled()) {
					logger.debug(checking
							+ "No DispatcherServlet found (search terminated with matches=true)");
				}
				// No dispatcher servlet so no need to ask further questions
				return true;
			}
			if (Arrays.asList(beans).contains(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)) {
				if (logger.isDebugEnabled()) {
					logger.debug(checking + "DispatcherServlet found and named "
							+ DEFAULT_DISPATCHER_SERVLET_BEAN_NAME
							+ " (search terminated with matches=false)");
				}
				// An existing bean with the default name
				return false;
			}
			if (logger.isDebugEnabled()) {
				logger.debug(checking
						+ "Multiple DispatcherServlets found and none is named "
						+ DEFAULT_DISPATCHER_SERVLET_BEAN_NAME
						+ " (search terminated with matches=true)");
			}
			return true;
		}
	}

}
