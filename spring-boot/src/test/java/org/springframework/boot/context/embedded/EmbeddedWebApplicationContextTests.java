/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.context.embedded;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.SessionScope;
import org.springframework.web.filter.GenericFilterBean;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link EmbeddedWebApplicationContext}.
 *
 * @author Phillip Webb
 */
public class EmbeddedWebApplicationContextTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private EmbeddedWebApplicationContext context;

	@Before
	public void setup() {
		this.context = new EmbeddedWebApplicationContext();
	}

	@After
	public void cleanup() {
		this.context.close();
	}

	@Test
	public void startRegistrations() throws Exception {
		addEmbeddedServletContainerFactoryBean();
		this.context.refresh();

		MockEmbeddedServletContainerFactory escf = getEmbeddedServletContainerFactory();

		// Ensure that the context has been setup
		assertThat(this.context.getServletContext(), equalTo(escf.getServletContext()));
		verify(escf.getServletContext()).setAttribute(
				WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
				this.context);

		// Ensure WebApplicationContextUtils.registerWebApplicationScopes was called
		assertThat(
				this.context.getBeanFactory().getRegisteredScope(
						WebApplicationContext.SCOPE_SESSION),
				instanceOf(SessionScope.class));

		// Ensure WebApplicationContextUtils.registerEnvironmentBeans was called
		assertThat(
				this.context
						.containsBean(WebApplicationContext.SERVLET_CONTEXT_BEAN_NAME),
				equalTo(true));
	}

	@Test
	public void doesNotRegistersShutdownHook() throws Exception {
		// See gh-314 for background. We no longer register the shutdown hook
		// since it is really the callers responsibility. The shutdown hook could
		// also be problematic in a classic WAR deployment.
		addEmbeddedServletContainerFactoryBean();
		this.context.refresh();
		Field shutdownHookField = AbstractApplicationContext.class
				.getDeclaredField("shutdownHook");
		shutdownHookField.setAccessible(true);
		Object shutdownHook = shutdownHookField.get(this.context);
		assertThat(shutdownHook, nullValue());
	}

	@Test
	public void containerEventPublished() throws Exception {
		addEmbeddedServletContainerFactoryBean();
		this.context.registerBeanDefinition("listener", new RootBeanDefinition(
				MockListener.class));
		this.context.refresh();
		EmbeddedServletContainerInitializedEvent event = this.context.getBean(
				MockListener.class).getEvent();
		assertNotNull(event);
		assertTrue(event.getSource().getPort() >= 0);
		assertEquals(this.context, event.getApplicationContext());
	}

	@Test
	public void stopOnClose() throws Exception {
		addEmbeddedServletContainerFactoryBean();
		this.context.refresh();
		MockEmbeddedServletContainerFactory escf = getEmbeddedServletContainerFactory();
		this.context.close();
		verify(escf.getContainer()).stop();
	}

	@Test
	public void cannotSecondRefresh() throws Exception {
		addEmbeddedServletContainerFactoryBean();
		this.context.refresh();
		this.thrown.expect(IllegalStateException.class);
		this.context.refresh();
	}

	@Test
	public void servletContextAwareBeansAreInjected() throws Exception {
		addEmbeddedServletContainerFactoryBean();
		ServletContextAware bean = mock(ServletContextAware.class);
		this.context.registerBeanDefinition("bean", beanDefinition(bean));
		this.context.refresh();
		verify(bean).setServletContext(
				getEmbeddedServletContainerFactory().getServletContext());
	}

	@Test
	public void missingEmbeddedServletContainerFactory() throws Exception {
		this.thrown.expect(ApplicationContextException.class);
		this.thrown.expectMessage("Unable to start EmbeddedWebApplicationContext due to "
				+ "missing EmbeddedServletContainerFactory bean");
		this.context.refresh();
	}

	@Test
	public void tooManyEmbeddedServletContainerFactories() throws Exception {
		addEmbeddedServletContainerFactoryBean();
		this.context.registerBeanDefinition("embeddedServletContainerFactory2",
				new RootBeanDefinition(MockEmbeddedServletContainerFactory.class));
		this.thrown.expect(ApplicationContextException.class);
		this.thrown.expectMessage("Unable to start EmbeddedWebApplicationContext due to "
				+ "multiple EmbeddedServletContainerFactory beans");
		this.context.refresh();

	}

	@Test
	public void singleServletBean() throws Exception {
		addEmbeddedServletContainerFactoryBean();
		Servlet servlet = mock(Servlet.class);
		this.context.registerBeanDefinition("servletBean", beanDefinition(servlet));
		this.context.refresh();
		MockEmbeddedServletContainerFactory escf = getEmbeddedServletContainerFactory();
		verify(escf.getServletContext()).addServlet("servletBean", servlet);
		verify(escf.getRegisteredServlet(0).getRegistration()).addMapping("/");
	}

	@Test
	public void orderedBeanInsertedCorrectly() throws Exception {
		addEmbeddedServletContainerFactoryBean();
		OrderedFilter filter = new OrderedFilter();
		this.context.registerBeanDefinition("filterBean", beanDefinition(filter));
		FilterRegistrationBean registration = new FilterRegistrationBean();
		registration.setFilter(Mockito.mock(Filter.class));
		registration.setOrder(100);
		this.context.registerBeanDefinition("filterRegistrationBean",
				beanDefinition(registration));
		this.context.refresh();
		MockEmbeddedServletContainerFactory escf = getEmbeddedServletContainerFactory();
		verify(escf.getServletContext()).addFilter("filterBean", filter);
		verify(escf.getServletContext()).addFilter("object", registration.getFilter());
		assertEquals(filter, escf.getRegisteredFilter(0).getFilter());
	}

	@Test
	public void multipleServletBeans() throws Exception {
		addEmbeddedServletContainerFactoryBean();
		Servlet servlet1 = mock(Servlet.class,
				withSettings().extraInterfaces(Ordered.class));
		given(((Ordered) servlet1).getOrder()).willReturn(1);
		Servlet servlet2 = mock(Servlet.class,
				withSettings().extraInterfaces(Ordered.class));
		given(((Ordered) servlet2).getOrder()).willReturn(2);
		this.context.registerBeanDefinition("servletBean2", beanDefinition(servlet2));
		this.context.registerBeanDefinition("servletBean1", beanDefinition(servlet1));
		this.context.refresh();
		MockEmbeddedServletContainerFactory escf = getEmbeddedServletContainerFactory();
		ServletContext servletContext = escf.getServletContext();
		InOrder ordered = inOrder(servletContext);
		ordered.verify(servletContext).addServlet("servletBean1", servlet1);
		ordered.verify(servletContext).addServlet("servletBean2", servlet2);
		verify(escf.getRegisteredServlet(0).getRegistration()).addMapping(
				"/servletBean1/");
		verify(escf.getRegisteredServlet(1).getRegistration()).addMapping(
				"/servletBean2/");
	}

	@Test
	public void multipleServletBeansWithMainDispatcher() throws Exception {
		addEmbeddedServletContainerFactoryBean();
		Servlet servlet1 = mock(Servlet.class,
				withSettings().extraInterfaces(Ordered.class));
		given(((Ordered) servlet1).getOrder()).willReturn(1);
		Servlet servlet2 = mock(Servlet.class,
				withSettings().extraInterfaces(Ordered.class));
		given(((Ordered) servlet2).getOrder()).willReturn(2);
		this.context.registerBeanDefinition("servletBean2", beanDefinition(servlet2));
		this.context
				.registerBeanDefinition("dispatcherServlet", beanDefinition(servlet1));
		this.context.refresh();
		MockEmbeddedServletContainerFactory escf = getEmbeddedServletContainerFactory();
		ServletContext servletContext = escf.getServletContext();
		InOrder ordered = inOrder(servletContext);
		ordered.verify(servletContext).addServlet("dispatcherServlet", servlet1);
		ordered.verify(servletContext).addServlet("servletBean2", servlet2);
		verify(escf.getRegisteredServlet(0).getRegistration()).addMapping("/");
		verify(escf.getRegisteredServlet(1).getRegistration()).addMapping(
				"/servletBean2/");
	}

	@Test
	public void servletAndFilterBeans() throws Exception {
		addEmbeddedServletContainerFactoryBean();
		Servlet servlet = mock(Servlet.class);
		Filter filter1 = mock(Filter.class, withSettings().extraInterfaces(Ordered.class));
		given(((Ordered) filter1).getOrder()).willReturn(1);
		Filter filter2 = mock(Filter.class, withSettings().extraInterfaces(Ordered.class));
		given(((Ordered) filter2).getOrder()).willReturn(2);
		this.context.registerBeanDefinition("servletBean", beanDefinition(servlet));
		this.context.registerBeanDefinition("filterBean2", beanDefinition(filter2));
		this.context.registerBeanDefinition("filterBean1", beanDefinition(filter1));
		this.context.refresh();
		MockEmbeddedServletContainerFactory escf = getEmbeddedServletContainerFactory();
		ServletContext servletContext = escf.getServletContext();
		InOrder ordered = inOrder(servletContext);
		verify(escf.getServletContext()).addServlet("servletBean", servlet);
		verify(escf.getRegisteredServlet(0).getRegistration()).addMapping("/");
		ordered.verify(escf.getServletContext()).addFilter("filterBean1", filter1);
		ordered.verify(escf.getServletContext()).addFilter("filterBean2", filter2);
		verify(escf.getRegisteredFilter(0).getRegistration()).addMappingForUrlPatterns(
				FilterRegistrationBean.ASYNC_DISPATCHER_TYPES, false, "/*");
		verify(escf.getRegisteredFilter(1).getRegistration()).addMappingForUrlPatterns(
				FilterRegistrationBean.ASYNC_DISPATCHER_TYPES, false, "/*");
	}

	@Test
	public void servletContextInitializerBeans() throws Exception {
		addEmbeddedServletContainerFactoryBean();
		ServletContextInitializer initializer1 = mock(ServletContextInitializer.class,
				withSettings().extraInterfaces(Ordered.class));
		given(((Ordered) initializer1).getOrder()).willReturn(1);
		ServletContextInitializer initializer2 = mock(ServletContextInitializer.class,
				withSettings().extraInterfaces(Ordered.class));
		given(((Ordered) initializer2).getOrder()).willReturn(2);
		this.context.registerBeanDefinition("initializerBean2",
				beanDefinition(initializer2));
		this.context.registerBeanDefinition("initializerBean1",
				beanDefinition(initializer1));
		this.context.refresh();
		ServletContext servletContext = getEmbeddedServletContainerFactory()
				.getServletContext();
		InOrder ordered = inOrder(initializer1, initializer2);
		ordered.verify(initializer1).onStartup(servletContext);
		ordered.verify(initializer2).onStartup(servletContext);
	}

	@Test
	public void servletContextListenerBeans() throws Exception {
		addEmbeddedServletContainerFactoryBean();
		ServletContextListener initializer = mock(ServletContextListener.class);
		this.context.registerBeanDefinition("initializerBean",
				beanDefinition(initializer));
		this.context.refresh();
		ServletContext servletContext = getEmbeddedServletContainerFactory()
				.getServletContext();
		verify(servletContext).addListener(initializer);
	}

	@Test
	public void unorderedServletContextInitializerBeans() throws Exception {
		addEmbeddedServletContainerFactoryBean();
		ServletContextInitializer initializer1 = mock(ServletContextInitializer.class);
		ServletContextInitializer initializer2 = mock(ServletContextInitializer.class);
		this.context.registerBeanDefinition("initializerBean2",
				beanDefinition(initializer2));
		this.context.registerBeanDefinition("initializerBean1",
				beanDefinition(initializer1));
		this.context.refresh();
		ServletContext servletContext = getEmbeddedServletContainerFactory()
				.getServletContext();
		verify(initializer1).onStartup(servletContext);
		verify(initializer2).onStartup(servletContext);
	}

	@Test
	public void servletContextInitializerBeansDoesNotSkipServletsAndFilters()
			throws Exception {
		addEmbeddedServletContainerFactoryBean();
		ServletContextInitializer initializer = mock(ServletContextInitializer.class);
		Servlet servlet = mock(Servlet.class);
		Filter filter = mock(Filter.class);
		this.context.registerBeanDefinition("initializerBean",
				beanDefinition(initializer));
		this.context.registerBeanDefinition("servletBean", beanDefinition(servlet));
		this.context.registerBeanDefinition("filterBean", beanDefinition(filter));
		this.context.refresh();
		ServletContext servletContext = getEmbeddedServletContainerFactory()
				.getServletContext();
		verify(initializer).onStartup(servletContext);
		verify(servletContext).addServlet(anyString(), (Servlet) anyObject());
		verify(servletContext).addFilter(anyString(), (Filter) anyObject());
	}

	@Test
	public void servletContextInitializerBeansSkipsRegisteredServletsAndFilters()
			throws Exception {
		addEmbeddedServletContainerFactoryBean();
		Servlet servlet = mock(Servlet.class);
		Filter filter = mock(Filter.class);
		ServletRegistrationBean initializer = new ServletRegistrationBean(servlet, "/foo");
		this.context.registerBeanDefinition("initializerBean",
				beanDefinition(initializer));
		this.context.registerBeanDefinition("servletBean", beanDefinition(servlet));
		this.context.registerBeanDefinition("filterBean", beanDefinition(filter));
		this.context.refresh();
		ServletContext servletContext = getEmbeddedServletContainerFactory()
				.getServletContext();
		verify(servletContext, atMost(1)).addServlet(anyString(), (Servlet) anyObject());
		verify(servletContext, atMost(1)).addFilter(anyString(), (Filter) anyObject());
	}

	@Test
	public void filterReegistrationBeansSkipsRegisteredFilters() throws Exception {
		addEmbeddedServletContainerFactoryBean();
		Filter filter = mock(Filter.class);
		FilterRegistrationBean initializer = new FilterRegistrationBean(filter);
		this.context.registerBeanDefinition("initializerBean",
				beanDefinition(initializer));
		this.context.registerBeanDefinition("filterBean", beanDefinition(filter));
		this.context.refresh();
		ServletContext servletContext = getEmbeddedServletContainerFactory()
				.getServletContext();
		verify(servletContext, atMost(1)).addFilter(anyString(), (Filter) anyObject());
	}

	@Test
	public void postProcessEmbeddedServletContainerFactory() throws Exception {
		RootBeanDefinition bd = new RootBeanDefinition(
				MockEmbeddedServletContainerFactory.class);
		MutablePropertyValues pv = new MutablePropertyValues();
		pv.add("port", "${port}");
		bd.setPropertyValues(pv);
		this.context.registerBeanDefinition("embeddedServletContainerFactory", bd);

		PropertySourcesPlaceholderConfigurer propertySupport = new PropertySourcesPlaceholderConfigurer();
		Properties properties = new Properties();
		properties.put("port", 8080);
		propertySupport.setProperties(properties);
		this.context.registerBeanDefinition("propertySupport",
				beanDefinition(propertySupport));

		this.context.refresh();
		assertThat(getEmbeddedServletContainerFactory().getContainer().getPort(),
				equalTo(8080));
	}

	private void addEmbeddedServletContainerFactoryBean() {
		this.context.registerBeanDefinition("embeddedServletContainerFactory",
				new RootBeanDefinition(MockEmbeddedServletContainerFactory.class));
	}

	public MockEmbeddedServletContainerFactory getEmbeddedServletContainerFactory() {
		return this.context.getBean(MockEmbeddedServletContainerFactory.class);
	}

	private BeanDefinition beanDefinition(Object bean) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setBeanClass(getClass());
		beanDefinition.setFactoryMethodName("getBean");
		ConstructorArgumentValues constructorArguments = new ConstructorArgumentValues();
		constructorArguments.addGenericArgumentValue(bean);
		beanDefinition.setConstructorArgumentValues(constructorArguments);
		return beanDefinition;
	}

	public static <T> T getBean(T object) {
		return object;
	}

	public static class MockListener implements
			ApplicationListener<EmbeddedServletContainerInitializedEvent> {

		private EmbeddedServletContainerInitializedEvent event;

		@Override
		public void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) {
			this.event = event;
		}

		public EmbeddedServletContainerInitializedEvent getEvent() {
			return this.event;
		}

	}

	@Order(10)
	protected static class OrderedFilter extends GenericFilterBean {

		@Override
		public void doFilter(ServletRequest request, ServletResponse response,
				FilterChain chain) throws IOException, ServletException {
		}

	}
}
