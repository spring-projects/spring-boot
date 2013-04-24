/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.bootstrap.context.embedded;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.Resource;
import org.springframework.web.ServletContextInitializer;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.context.support.ServletContextAwareProcessor;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.context.support.WebApplicationContextServletContextAwareProcessor;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * A {@link WebApplicationContext} that can be used to bootstrap itself from a contained
 * {@link EmbeddedServletContainerFactory} bean.
 * 
 * <p>
 * This context will create, initialize and run an {@link EmbeddedServletContainer} by
 * searching for a single {@link EmbeddedServletContainerFactory} bean within the
 * {@link ApplicationContext} itself. The {@link EmbeddedServletContainerFactory} is free
 * to use standard Spring concepts (such as dependency injection, lifecycle callbacks and
 * property placeholder variables).
 * 
 * <p>
 * In addition, any {@link Servlet} or {@link Filter} beans defined in the context will be
 * automatically registered with the embedded Servlet container. In the case of a single
 * Servlet bean, the '/*' mapping will be used. If multiple Servlet beans are found then
 * the lowercase bean name will be used as a mapping prefix. Filter beans will be mapped
 * to all URLs ('/*').
 * 
 * <p>
 * For more advanced configuration, the context can instead define beans that implement
 * the {@link ServletContextInitializer} interface (most often
 * {@link ServletRegistrationBean}s and/or {@link FilterRegistrationBean}s). To prevent
 * double registration, the use of {@link ServletContextInitializer} beans will disable
 * automatic Servlet and Filter bean registration.
 * 
 * <p>
 * Although this context can be used directly, most developers should consider using the
 * {@link AnnotationConfigEmbeddedWebApplicationContext} or
 * {@link XmlEmbeddedWebApplicationContext} variants.
 * 
 * @author Phillip Webb
 * @since 4.0
 * @see AnnotationConfigEmbeddedWebApplicationContext
 * @see XmlEmbeddedWebApplicationContext
 * @see EmbeddedServletContainerFactory
 */
public class EmbeddedWebApplicationContext extends GenericWebApplicationContext {

	private EmbeddedServletContainer embeddedServletContainer;

	private ServletConfig servletConfig;

	private String namespace;

	/**
	 * Register ServletContextAwareProcessor.
	 * @see ServletContextAwareProcessor
	 */
	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		beanFactory
				.addBeanPostProcessor(new WebApplicationContextServletContextAwareProcessor(
						this));
		beanFactory.ignoreDependencyInterface(ServletContextAware.class);
	}

	@Override
	protected void onRefresh() {
		super.onRefresh();
		registerShutdownHook();
		createAndStartEmbeddedServletContainer();
	}

	@Override
	protected void doClose() {
		super.doClose();
		stopAndReleaseEmbeddedServletContainer();
	}

	private synchronized void createAndStartEmbeddedServletContainer() {
		if (this.embeddedServletContainer == null) {
			EmbeddedServletContainerFactory containerFactory = getEmbeddedServletContainerFactory();
			this.embeddedServletContainer = containerFactory
					.getEmbdeddedServletContainer(getServletContextInitializers());
			WebApplicationContextUtils.registerWebApplicationScopes(getBeanFactory(),
					getServletContext());
			WebApplicationContextUtils.registerEnvironmentBeans(getBeanFactory(),
					getServletContext());
			initPropertySources();
		}
	}

	/**
	 * Returns the {@link EmbeddedServletContainerFactory} that should be used to create
	 * the embedded servlet container. By default this method searches for a suitable bean
	 * in the context itself.
	 * @return a {@link EmbeddedServletContainerFactory} (never {@code null})
	 */
	protected EmbeddedServletContainerFactory getEmbeddedServletContainerFactory() {
		try {
			return getBeanFactory().getBean(EmbeddedServletContainerFactory.class);
		} catch (NoUniqueBeanDefinitionException ex) {
			throw new ApplicationContextException(
					"Unable to start EmbeddedWebApplicationContext due to multiple "
							+ "EmbeddedServletContainerFactory beans.", ex);
		} catch (NoSuchBeanDefinitionException ex) {
			throw new ApplicationContextException(
					"Unable to start EmbeddedWebApplicationContext due to missing "
							+ "EmbeddedServletContainerFactory bean.", ex);
		}
	}

	/**
	 * Returns all {@link ServletContextInitializer}s that should be applied.
	 */
	private ServletContextInitializer[] getServletContextInitializers() {
		List<ServletContextInitializer> initializers = new ArrayList<ServletContextInitializer>();
		initializers.add(getSelfInitializer());
		initializers.addAll(getServletContextInitializerBeans());
		return initializers.toArray(new ServletContextInitializer[initializers.size()]);
	}

	/**
	 * Returns the {@link ServletContextInitializer} that will be used to complete the
	 * setup of this {@link WebApplicationContext}.
	 * @see #prepareEmbeddedWebApplicationContext(ServletContext)
	 */
	private ServletContextInitializer getSelfInitializer() {
		return new ServletContextInitializer() {
			@Override
			public void onStartup(ServletContext servletContext) throws ServletException {
				prepareEmbeddedWebApplicationContext(servletContext);
			}
		};
	}

	/**
	 * Returns {@link ServletContextInitializer}s that should be used with the embedded
	 * Servlet context. By default this method will first attempt to find
	 * {@link ServletContextInitializer} beans, if none are found it will instead search
	 * for {@link Servlet} and {@link Filter} beans.
	 */
	protected Collection<ServletContextInitializer> getServletContextInitializerBeans() {

		Set<ServletContextInitializer> initializers = new LinkedHashSet<ServletContextInitializer>();

		for (Entry<String, ServletContextInitializer> initializerBean : getOrderedBeansOfType(ServletContextInitializer.class)) {
			initializers.add(initializerBean.getValue());
		}

		if (initializers.isEmpty()) {

			SortedSet<Entry<String, Servlet>> servletBeans = getOrderedBeansOfType(Servlet.class);
			for (Entry<String, Servlet> servletBean : servletBeans) {
				String url = (servletBeans.size() == 1 ? "/" : "/"
						+ servletBean.getKey().toLowerCase() + "/*");
				ServletRegistrationBean registration = new ServletRegistrationBean(
						servletBean.getValue(), url);
				registration.setName(servletBean.getKey());
				initializers.add(registration);
			}

			for (Entry<String, Filter> filterBean : getOrderedBeansOfType(Filter.class)) {
				FilterRegistrationBean registration = new FilterRegistrationBean(
						filterBean.getValue());
				registration.setName(filterBean.getKey());
				initializers.add(registration);
			}
		}

		return initializers;
	}

	/**
	 * Prepare the {@link WebApplicationContext} with the given fully loaded
	 * {@link ServletContext}. This method is usually called from
	 * {@link ServletContextInitializer#onStartup(ServletContext)} and is similar to the
	 * functionality usually provided by a {@link ContextLoaderListener}.
	 * @param servletContext the operational servlet context
	 */
	protected void prepareEmbeddedWebApplicationContext(ServletContext servletContext) {
		if (servletContext
				.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null) {
			throw new IllegalStateException(
					"Cannot initialize context because there is already a root application context present - "
							+ "check whether you have multiple ServletContextInitializer!");
		}
		Log logger = LogFactory.getLog(ContextLoader.class);
		servletContext.log("Initializing Spring embedded WebApplicationContext");
		try {
			servletContext.setAttribute(
					WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this);
			if (logger.isDebugEnabled()) {
				logger.debug("Published root WebApplicationContext as ServletContext attribute with name ["
						+ WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
						+ "]");
			}
			setServletContext(servletContext);
			if (logger.isInfoEnabled()) {
				long elapsedTime = System.currentTimeMillis() - getStartupDate();
				logger.info("Root WebApplicationContext: initialization completed in "
						+ elapsedTime + " ms");
			}
		} catch (RuntimeException ex) {
			logger.error("Context initialization failed", ex);
			servletContext.setAttribute(
					WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ex);
			throw ex;
		} catch (Error err) {
			logger.error("Context initialization failed", err);
			servletContext.setAttribute(
					WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, err);
			throw err;
		}
	}

	private <T> SortedSet<Entry<String, T>> getOrderedBeansOfType(Class<T> type) {
		SortedSet<Entry<String, T>> beans = new TreeSet<Entry<String, T>>(
				new Comparator<Entry<String, T>>() {
					@Override
					public int compare(Entry<String, T> o1, Entry<String, T> o2) {
						return AnnotationAwareOrderComparator.INSTANCE.compare(
								o1.getValue(), o2.getValue());
					}
				});
		beans.addAll(getBeanFactory().getBeansOfType(type, true, true).entrySet());
		return beans;
	}

	private synchronized void stopAndReleaseEmbeddedServletContainer() {
		if (this.embeddedServletContainer != null) {
			try {
				this.embeddedServletContainer.stop();
				this.embeddedServletContainer = null;
			} catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

	@Override
	protected Resource getResourceByPath(String path) {
		if (getServletContext() == null) {
			return new ClassPathContextResource(path, getClassLoader());
		}
		return new ServletContextResource(getServletContext(), path);
	}

	@Override
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	@Override
	public String getNamespace() {
		return this.namespace;
	}

	@Override
	public void setServletConfig(ServletConfig servletConfig) {
		this.servletConfig = servletConfig;
	}

	@Override
	public ServletConfig getServletConfig() {
		return this.servletConfig;
	}

	/**
	 * Returns the {@link EmbeddedServletContainer} that was created by the context or
	 * {@code null} if the container has not yet been created.
	 */
	public EmbeddedServletContainer getEmbeddedServletContainer() {
		return this.embeddedServletContainer;
	}
}
