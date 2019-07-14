/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.web.servlet.context;

import java.util.EnumSet;
import java.util.Properties;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.MockitoAnnotations;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.boot.web.context.ServerPortInfoApplicationContextInitializer;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.MockServletWebServerFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.SessionScope;
import org.springframework.web.filter.GenericFilterBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link ServletWebServerApplicationContext}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
@ExtendWith(OutputCaptureExtension.class)
class ServletWebServerApplicationContextTests {

	private ServletWebServerApplicationContext context;

	@Captor
	private ArgumentCaptor<Filter> filterCaptor;

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
		this.context = new ServletWebServerApplicationContext();
	}

	@AfterEach
	void cleanup() {
		this.context.close();
	}

	@Test
	void startRegistrations() {
		addWebServerFactoryBean();
		this.context.refresh();
		MockServletWebServerFactory factory = getWebServerFactory();
		// Ensure that the context has been setup
		assertThat(this.context.getServletContext()).isEqualTo(factory.getServletContext());
		verify(factory.getServletContext()).setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
				this.context);
		// Ensure WebApplicationContextUtils.registerWebApplicationScopes was called
		assertThat(this.context.getBeanFactory().getRegisteredScope(WebApplicationContext.SCOPE_SESSION))
				.isInstanceOf(SessionScope.class);
		// Ensure WebApplicationContextUtils.registerEnvironmentBeans was called
		assertThat(this.context.containsBean(WebApplicationContext.SERVLET_CONTEXT_BEAN_NAME)).isTrue();
	}

	@Test
	void doesNotRegistersShutdownHook() {
		// See gh-314 for background. We no longer register the shutdown hook
		// since it is really the callers responsibility. The shutdown hook could
		// also be problematic in a classic WAR deployment.
		addWebServerFactoryBean();
		this.context.refresh();
		assertThat(this.context).hasFieldOrPropertyWithValue("shutdownHook", null);
	}

	@Test
	void ServletWebServerInitializedEventPublished() {
		addWebServerFactoryBean();
		this.context.registerBeanDefinition("listener", new RootBeanDefinition(MockListener.class));
		this.context.refresh();
		ServletWebServerInitializedEvent event = this.context.getBean(MockListener.class).getEvent();
		assertThat(event).isNotNull();
		assertThat(event.getSource().getPort() >= 0).isTrue();
		assertThat(event.getApplicationContext()).isEqualTo(this.context);
	}

	@Test
	void localPortIsAvailable() {
		addWebServerFactoryBean();
		new ServerPortInfoApplicationContextInitializer().initialize(this.context);
		this.context.refresh();
		ConfigurableEnvironment environment = this.context.getEnvironment();
		assertThat(environment.containsProperty("local.server.port")).isTrue();
		assertThat(environment.getProperty("local.server.port")).isEqualTo("8080");
	}

	@Test
	void stopOnClose() {
		addWebServerFactoryBean();
		this.context.refresh();
		MockServletWebServerFactory factory = getWebServerFactory();
		this.context.close();
		verify(factory.getWebServer()).stop();
	}

	@Test
	void cannotSecondRefresh() {
		addWebServerFactoryBean();
		this.context.refresh();
		assertThatIllegalStateException().isThrownBy(() -> this.context.refresh());
	}

	@Test
	void servletContextAwareBeansAreInjected() {
		addWebServerFactoryBean();
		ServletContextAware bean = mock(ServletContextAware.class);
		this.context.registerBeanDefinition("bean", beanDefinition(bean));
		this.context.refresh();
		verify(bean).setServletContext(getWebServerFactory().getServletContext());
	}

	@Test
	void missingServletWebServerFactory() {
		assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(() -> this.context.refresh())
				.withMessageContaining("Unable to start ServletWebServerApplicationContext due to missing "
						+ "ServletWebServerFactory bean");
	}

	@Test
	void tooManyWebServerFactories() {
		addWebServerFactoryBean();
		this.context.registerBeanDefinition("webServerFactory2",
				new RootBeanDefinition(MockServletWebServerFactory.class));
		assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(() -> this.context.refresh())
				.withMessageContaining("Unable to start ServletWebServerApplicationContext due to "
						+ "multiple ServletWebServerFactory beans");

	}

	@Test
	void singleServletBean() {
		addWebServerFactoryBean();
		Servlet servlet = mock(Servlet.class);
		this.context.registerBeanDefinition("servletBean", beanDefinition(servlet));
		this.context.refresh();
		MockServletWebServerFactory factory = getWebServerFactory();
		verify(factory.getServletContext()).addServlet("servletBean", servlet);
		verify(factory.getRegisteredServlet(0).getRegistration()).addMapping("/");
	}

	@Test
	void orderedBeanInsertedCorrectly() {
		addWebServerFactoryBean();
		OrderedFilter filter = new OrderedFilter();
		this.context.registerBeanDefinition("filterBean", beanDefinition(filter));
		FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
		registration.setFilter(mock(Filter.class));
		registration.setOrder(100);
		this.context.registerBeanDefinition("filterRegistrationBean", beanDefinition(registration));
		this.context.refresh();
		MockServletWebServerFactory factory = getWebServerFactory();
		verify(factory.getServletContext()).addFilter("filterBean", filter);
		verify(factory.getServletContext()).addFilter("object", registration.getFilter());
		assertThat(factory.getRegisteredFilter(0).getFilter()).isEqualTo(filter);
	}

	@Test
	void multipleServletBeans() {
		addWebServerFactoryBean();
		Servlet servlet1 = mock(Servlet.class, withSettings().extraInterfaces(Ordered.class));
		given(((Ordered) servlet1).getOrder()).willReturn(1);
		Servlet servlet2 = mock(Servlet.class, withSettings().extraInterfaces(Ordered.class));
		given(((Ordered) servlet2).getOrder()).willReturn(2);
		this.context.registerBeanDefinition("servletBean2", beanDefinition(servlet2));
		this.context.registerBeanDefinition("servletBean1", beanDefinition(servlet1));
		this.context.refresh();
		MockServletWebServerFactory factory = getWebServerFactory();
		ServletContext servletContext = factory.getServletContext();
		InOrder ordered = inOrder(servletContext);
		ordered.verify(servletContext).addServlet("servletBean1", servlet1);
		ordered.verify(servletContext).addServlet("servletBean2", servlet2);
		verify(factory.getRegisteredServlet(0).getRegistration()).addMapping("/servletBean1/");
		verify(factory.getRegisteredServlet(1).getRegistration()).addMapping("/servletBean2/");
	}

	@Test
	void multipleServletBeansWithMainDispatcher() {
		addWebServerFactoryBean();
		Servlet servlet1 = mock(Servlet.class, withSettings().extraInterfaces(Ordered.class));
		given(((Ordered) servlet1).getOrder()).willReturn(1);
		Servlet servlet2 = mock(Servlet.class, withSettings().extraInterfaces(Ordered.class));
		given(((Ordered) servlet2).getOrder()).willReturn(2);
		this.context.registerBeanDefinition("servletBean2", beanDefinition(servlet2));
		this.context.registerBeanDefinition("dispatcherServlet", beanDefinition(servlet1));
		this.context.refresh();
		MockServletWebServerFactory factory = getWebServerFactory();
		ServletContext servletContext = factory.getServletContext();
		InOrder ordered = inOrder(servletContext);
		ordered.verify(servletContext).addServlet("dispatcherServlet", servlet1);
		ordered.verify(servletContext).addServlet("servletBean2", servlet2);
		verify(factory.getRegisteredServlet(0).getRegistration()).addMapping("/");
		verify(factory.getRegisteredServlet(1).getRegistration()).addMapping("/servletBean2/");
	}

	@Test
	void servletAndFilterBeans() {
		addWebServerFactoryBean();
		Servlet servlet = mock(Servlet.class);
		Filter filter1 = mock(Filter.class, withSettings().extraInterfaces(Ordered.class));
		given(((Ordered) filter1).getOrder()).willReturn(1);
		Filter filter2 = mock(Filter.class, withSettings().extraInterfaces(Ordered.class));
		given(((Ordered) filter2).getOrder()).willReturn(2);
		this.context.registerBeanDefinition("servletBean", beanDefinition(servlet));
		this.context.registerBeanDefinition("filterBean2", beanDefinition(filter2));
		this.context.registerBeanDefinition("filterBean1", beanDefinition(filter1));
		this.context.refresh();
		MockServletWebServerFactory factory = getWebServerFactory();
		ServletContext servletContext = factory.getServletContext();
		InOrder ordered = inOrder(servletContext);
		verify(factory.getServletContext()).addServlet("servletBean", servlet);
		verify(factory.getRegisteredServlet(0).getRegistration()).addMapping("/");
		ordered.verify(factory.getServletContext()).addFilter("filterBean1", filter1);
		ordered.verify(factory.getServletContext()).addFilter("filterBean2", filter2);
		verify(factory.getRegisteredFilter(0).getRegistration())
				.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
		verify(factory.getRegisteredFilter(1).getRegistration())
				.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
	}

	@Test
	void servletContextInitializerBeans() throws Exception {
		addWebServerFactoryBean();
		ServletContextInitializer initializer1 = mock(ServletContextInitializer.class,
				withSettings().extraInterfaces(Ordered.class));
		given(((Ordered) initializer1).getOrder()).willReturn(1);
		ServletContextInitializer initializer2 = mock(ServletContextInitializer.class,
				withSettings().extraInterfaces(Ordered.class));
		given(((Ordered) initializer2).getOrder()).willReturn(2);
		this.context.registerBeanDefinition("initializerBean2", beanDefinition(initializer2));
		this.context.registerBeanDefinition("initializerBean1", beanDefinition(initializer1));
		this.context.refresh();
		ServletContext servletContext = getWebServerFactory().getServletContext();
		InOrder ordered = inOrder(initializer1, initializer2);
		ordered.verify(initializer1).onStartup(servletContext);
		ordered.verify(initializer2).onStartup(servletContext);
	}

	@Test
	void servletContextListenerBeans() {
		addWebServerFactoryBean();
		ServletContextListener initializer = mock(ServletContextListener.class);
		this.context.registerBeanDefinition("initializerBean", beanDefinition(initializer));
		this.context.refresh();
		ServletContext servletContext = getWebServerFactory().getServletContext();
		verify(servletContext).addListener(initializer);
	}

	@Test
	void unorderedServletContextInitializerBeans() throws Exception {
		addWebServerFactoryBean();
		ServletContextInitializer initializer1 = mock(ServletContextInitializer.class);
		ServletContextInitializer initializer2 = mock(ServletContextInitializer.class);
		this.context.registerBeanDefinition("initializerBean2", beanDefinition(initializer2));
		this.context.registerBeanDefinition("initializerBean1", beanDefinition(initializer1));
		this.context.refresh();
		ServletContext servletContext = getWebServerFactory().getServletContext();
		verify(initializer1).onStartup(servletContext);
		verify(initializer2).onStartup(servletContext);
	}

	@Test
	void servletContextInitializerBeansDoesNotSkipServletsAndFilters() throws Exception {
		addWebServerFactoryBean();
		ServletContextInitializer initializer = mock(ServletContextInitializer.class);
		Servlet servlet = mock(Servlet.class);
		Filter filter = mock(Filter.class);
		this.context.registerBeanDefinition("initializerBean", beanDefinition(initializer));
		this.context.registerBeanDefinition("servletBean", beanDefinition(servlet));
		this.context.registerBeanDefinition("filterBean", beanDefinition(filter));
		this.context.refresh();
		ServletContext servletContext = getWebServerFactory().getServletContext();
		verify(initializer).onStartup(servletContext);
		verify(servletContext).addServlet(anyString(), any(Servlet.class));
		verify(servletContext).addFilter(anyString(), any(Filter.class));
	}

	@Test
	void servletContextInitializerBeansSkipsRegisteredServletsAndFilters() {
		addWebServerFactoryBean();
		Servlet servlet = mock(Servlet.class);
		Filter filter = mock(Filter.class);
		ServletRegistrationBean<Servlet> initializer = new ServletRegistrationBean<>(servlet, "/foo");
		this.context.registerBeanDefinition("initializerBean", beanDefinition(initializer));
		this.context.registerBeanDefinition("servletBean", beanDefinition(servlet));
		this.context.registerBeanDefinition("filterBean", beanDefinition(filter));
		this.context.refresh();
		ServletContext servletContext = getWebServerFactory().getServletContext();
		verify(servletContext, atMost(1)).addServlet(anyString(), any(Servlet.class));
		verify(servletContext, atMost(1)).addFilter(anyString(), any(Filter.class));
	}

	@Test
	void filterRegistrationBeansSkipsRegisteredFilters() {
		addWebServerFactoryBean();
		Filter filter = mock(Filter.class);
		FilterRegistrationBean<Filter> initializer = new FilterRegistrationBean<>(filter);
		this.context.registerBeanDefinition("initializerBean", beanDefinition(initializer));
		this.context.registerBeanDefinition("filterBean", beanDefinition(filter));
		this.context.refresh();
		ServletContext servletContext = getWebServerFactory().getServletContext();
		verify(servletContext, atMost(1)).addFilter(anyString(), any(Filter.class));
	}

	@Test
	void delegatingFilterProxyRegistrationBeansSkipsTargetBeanNames() throws Exception {
		addWebServerFactoryBean();
		DelegatingFilterProxyRegistrationBean initializer = new DelegatingFilterProxyRegistrationBean("filterBean");
		this.context.registerBeanDefinition("initializerBean", beanDefinition(initializer));
		BeanDefinition filterBeanDefinition = beanDefinition(new IllegalStateException("Create FilterBean Failure"));
		filterBeanDefinition.setLazyInit(true);
		this.context.registerBeanDefinition("filterBean", filterBeanDefinition);
		this.context.refresh();
		ServletContext servletContext = getWebServerFactory().getServletContext();
		verify(servletContext, atMost(1)).addFilter(anyString(), this.filterCaptor.capture());
		// Up to this point the filterBean should not have been created, calling
		// the delegate proxy will trigger creation and an exception
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() -> {
			this.filterCaptor.getValue().init(new MockFilterConfig());
			this.filterCaptor.getValue().doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
					new MockFilterChain());
		}).withMessageContaining("Create FilterBean Failure");
	}

	@Test
	void postProcessWebServerFactory() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(MockServletWebServerFactory.class);
		MutablePropertyValues pv = new MutablePropertyValues();
		pv.add("port", "${port}");
		beanDefinition.setPropertyValues(pv);
		this.context.registerBeanDefinition("webServerFactory", beanDefinition);
		PropertySourcesPlaceholderConfigurer propertySupport = new PropertySourcesPlaceholderConfigurer();
		Properties properties = new Properties();
		properties.put("port", 8080);
		propertySupport.setProperties(properties);
		this.context.registerBeanDefinition("propertySupport", beanDefinition(propertySupport));
		this.context.refresh();
		assertThat(getWebServerFactory().getWebServer().getPort()).isEqualTo(8080);
	}

	@Test
	void doesNotReplaceExistingScopes() {
		// gh-2082
		Scope scope = mock(Scope.class);
		ConfigurableListableBeanFactory factory = this.context.getBeanFactory();
		factory.registerScope(WebApplicationContext.SCOPE_REQUEST, scope);
		factory.registerScope(WebApplicationContext.SCOPE_SESSION, scope);
		addWebServerFactoryBean();
		this.context.refresh();
		assertThat(factory.getRegisteredScope(WebApplicationContext.SCOPE_REQUEST)).isSameAs(scope);
		assertThat(factory.getRegisteredScope(WebApplicationContext.SCOPE_SESSION)).isSameAs(scope);
	}

	@Test
	void servletRequestCanBeInjectedEarly(CapturedOutput output) throws Exception {
		// gh-14990
		int initialOutputLength = output.length();
		addWebServerFactoryBean();
		RootBeanDefinition beanDefinition = new RootBeanDefinition(WithAutowiredServletRequest.class);
		beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		this.context.registerBeanDefinition("withAutowiredServletRequest", beanDefinition);
		this.context.addBeanFactoryPostProcessor((beanFactory) -> {
			WithAutowiredServletRequest bean = beanFactory.getBean(WithAutowiredServletRequest.class);
			assertThat(bean.getRequest()).isNotNull();
		});
		this.context.refresh();
		assertThat(output.toString().substring(initialOutputLength)).doesNotContain("Replacing scope");
	}

	@Test
	void webApplicationScopeIsRegistered() throws Exception {
		addWebServerFactoryBean();
		this.context.refresh();
		assertThat(this.context.getBeanFactory().getRegisteredScope(WebApplicationContext.SCOPE_APPLICATION))
				.isNotNull();
	}

	private void addWebServerFactoryBean() {
		this.context.registerBeanDefinition("webServerFactory",
				new RootBeanDefinition(MockServletWebServerFactory.class));
	}

	MockServletWebServerFactory getWebServerFactory() {
		return this.context.getBean(MockServletWebServerFactory.class);
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

	static <T> T getBean(T object) {
		if (object instanceof RuntimeException) {
			throw (RuntimeException) object;
		}
		return object;
	}

	static class MockListener implements ApplicationListener<ServletWebServerInitializedEvent> {

		private ServletWebServerInitializedEvent event;

		@Override
		public void onApplicationEvent(ServletWebServerInitializedEvent event) {
			this.event = event;
		}

		ServletWebServerInitializedEvent getEvent() {
			return this.event;
		}

	}

	@Order(10)
	static class OrderedFilter extends GenericFilterBean {

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
		}

	}

	static class WithAutowiredServletRequest {

		private final ServletRequest request;

		WithAutowiredServletRequest(ServletRequest request) {
			this.request = request;
		}

		ServletRequest getRequest() {
			return this.request;
		}

	}

}
