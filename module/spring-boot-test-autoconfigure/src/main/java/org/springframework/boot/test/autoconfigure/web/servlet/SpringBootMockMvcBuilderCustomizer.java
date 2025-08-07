/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.test.autoconfigure.web.servlet;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import jakarta.servlet.Filter;
import jakarta.servlet.annotation.WebInitParam;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.web.servlet.AbstractFilterRegistrationBean;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.boot.web.servlet.FilterRegistration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.RegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.result.PrintingResultHandler;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@link MockMvcBuilderCustomizer} for a typical Spring Boot application. Usually applied
 * automatically through {@link AutoConfigureMockMvc @AutoConfigureMockMvc}, but may also
 * be used directly.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @since 1.4.0
 */
public class SpringBootMockMvcBuilderCustomizer implements MockMvcBuilderCustomizer {

	private final WebApplicationContext context;

	/**
	 * Whether to add servlet filters.
	 */
	private boolean addFilters = true;

	/**
	 * MVC Print option.
	 */
	private MockMvcPrint print = MockMvcPrint.DEFAULT;

	/**
	 * Whether to only print failed exchanges.
	 */
	private boolean printOnlyOnFailure = true;

	/**
	 * Create a new {@link SpringBootMockMvcBuilderCustomizer} instance.
	 * @param context the source application context
	 */
	public SpringBootMockMvcBuilderCustomizer(WebApplicationContext context) {
		Assert.notNull(context, "'context' must not be null");
		this.context = context;
	}

	@Override
	public void customize(ConfigurableMockMvcBuilder<?> builder) {
		if (this.addFilters) {
			addFilters(builder);
		}
		ResultHandler printHandler = getPrintHandler();
		if (printHandler != null) {
			builder.alwaysDo(printHandler);
		}
	}

	private @Nullable ResultHandler getPrintHandler() {
		LinesWriter writer = getLinesWriter();
		if (writer == null) {
			return null;
		}
		if (this.printOnlyOnFailure) {
			writer = new DeferredLinesWriter(this.context, writer);
		}
		return new LinesWritingResultHandler(writer);
	}

	private @Nullable LinesWriter getLinesWriter() {
		if (this.print == MockMvcPrint.NONE) {
			return null;
		}
		if (this.print == MockMvcPrint.LOG_DEBUG) {
			return (LoggingLinesWriter.isDebugEnabled()) ? new LoggingLinesWriter() : null;
		}
		return new SystemLinesWriter(this.print);
	}

	private void addFilters(ConfigurableMockMvcBuilder<?> builder) {
		FilterRegistrationBeans registrations = new FilterRegistrationBeans(this.context);
		registrations.stream()
			.map(AbstractFilterRegistrationBean.class::cast)
			.filter(AbstractFilterRegistrationBean<?>::isEnabled)
			.forEach((registration) -> addFilter(builder, registration));
	}

	private void addFilter(ConfigurableMockMvcBuilder<?> builder, AbstractFilterRegistrationBean<?> registration) {
		Collection<String> urls = registration.getUrlPatterns();
		Filter filter = registration.getFilter();
		Assert.state(filter != null, "'filter' must not be null");
		builder.addFilter(filter, registration.getFilterName(), registration.getInitParameters(),
				registration.determineDispatcherTypes(), StringUtils.toStringArray(urls));
	}

	public void setAddFilters(boolean addFilters) {
		this.addFilters = addFilters;
	}

	public boolean isAddFilters() {
		return this.addFilters;
	}

	public void setPrint(MockMvcPrint print) {
		this.print = print;
	}

	public MockMvcPrint getPrint() {
		return this.print;
	}

	public void setPrintOnlyOnFailure(boolean printOnlyOnFailure) {
		this.printOnlyOnFailure = printOnlyOnFailure;
	}

	public boolean isPrintOnlyOnFailure() {
		return this.printOnlyOnFailure;
	}

	/**
	 * {@link ResultHandler} that prints {@link MvcResult} details to a given
	 * {@link LinesWriter}.
	 */
	private static class LinesWritingResultHandler implements ResultHandler {

		private final LinesWriter writer;

		LinesWritingResultHandler(LinesWriter writer) {
			this.writer = writer;
		}

		@Override
		public void handle(MvcResult result) throws Exception {
			LinesPrintingResultHandler delegate = new LinesPrintingResultHandler();
			delegate.handle(result);
			delegate.write(this.writer);
		}

		private static class LinesPrintingResultHandler extends PrintingResultHandler {

			protected LinesPrintingResultHandler() {
				super(new Printer());
			}

			void write(LinesWriter writer) {
				writer.write(((Printer) getPrinter()).getLines());
			}

			private static final class Printer implements ResultValuePrinter {

				private final List<String> lines = new ArrayList<>();

				@Override
				public void printHeading(String heading) {
					this.lines.add("");
					this.lines.add(String.format("%s:", heading));
				}

				@Override
				public void printValue(String label, @Nullable Object value) {
					if (value != null && value.getClass().isArray()) {
						value = CollectionUtils.arrayToList(value);
					}
					try {
						this.lines.add("%17s = %s".formatted(label, value));
					}
					catch (RuntimeException ex) {
						this.lines.add("%17s = << Exception '%s' occurred while formatting >>".formatted(label, ex));
					}
				}

				List<String> getLines() {
					return this.lines;
				}

			}

		}

	}

	/**
	 * Strategy interface to write MVC result lines.
	 */
	interface LinesWriter {

		void write(List<String> lines);

	}

	/**
	 * {@link LinesWriter} used to defer writing until errors are detected.
	 *
	 * @see MockMvcPrintOnlyOnFailureTestExecutionListener
	 */
	static class DeferredLinesWriter implements LinesWriter {

		private static final String BEAN_NAME = DeferredLinesWriter.class.getName();

		private final LinesWriter delegate;

		private final ThreadLocal<List<String>> lines = ThreadLocal.withInitial(ArrayList::new);

		DeferredLinesWriter(WebApplicationContext context, LinesWriter delegate) {
			Assert.state(context instanceof ConfigurableApplicationContext,
					"A ConfigurableApplicationContext is required for printOnlyOnFailure");
			((ConfigurableApplicationContext) context).getBeanFactory().registerSingleton(BEAN_NAME, this);
			this.delegate = delegate;
		}

		@Override
		public void write(List<String> lines) {
			this.lines.get().addAll(lines);
		}

		void writeDeferredResult() {
			this.delegate.write(this.lines.get());
		}

		static @Nullable DeferredLinesWriter get(ApplicationContext applicationContext) {
			try {
				return applicationContext.getBean(BEAN_NAME, DeferredLinesWriter.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				return null;
			}
		}

		void clear() {
			this.lines.remove();
		}

	}

	/**
	 * {@link LinesWriter} to output results to the log.
	 */
	private static final class LoggingLinesWriter implements LinesWriter {

		private static final Log logger = LogFactory.getLog("org.springframework.test.web.servlet.result");

		@Override
		public void write(List<String> lines) {
			if (logger.isDebugEnabled()) {
				StringWriter stringWriter = new StringWriter();
				PrintWriter printWriter = new PrintWriter(stringWriter);
				for (String line : lines) {
					printWriter.println(line);
				}
				logger.debug("MvcResult details:\n" + stringWriter);
			}
		}

		static boolean isDebugEnabled() {
			return logger.isDebugEnabled();
		}

	}

	/**
	 * {@link LinesWriter} to output results to {@code System.out} or {@code System.err}.
	 */
	private static class SystemLinesWriter implements LinesWriter {

		private final MockMvcPrint print;

		SystemLinesWriter(MockMvcPrint print) {
			this.print = print;
		}

		@Override
		public void write(List<String> lines) {
			PrintStream printStream = getPrintStream();
			for (String line : lines) {
				printStream.println(line);
			}
		}

		private PrintStream getPrintStream() {
			if (this.print == MockMvcPrint.SYSTEM_ERR) {
				return System.err;
			}
			return System.out;
		}

	}

	private static class FilterRegistrationBeans extends ServletContextInitializerBeans {

		FilterRegistrationBeans(ListableBeanFactory beanFactory) {
			super(beanFactory, FilterRegistrationBean.class, DelegatingFilterProxyRegistrationBean.class);
		}

		@Override
		protected void addAdaptableBeans(ListableBeanFactory beanFactory) {
			addAsRegistrationBean(beanFactory, Filter.class, new FilterRegistrationBeanAdapter(beanFactory));
		}

		private static final class FilterRegistrationBeanAdapter implements RegistrationBeanAdapter<Filter> {

			private final ListableBeanFactory beanFactory;

			private FilterRegistrationBeanAdapter(ListableBeanFactory beanFactory) {
				this.beanFactory = beanFactory;
			}

			@Override
			public RegistrationBean createRegistrationBean(String beanName, Filter source,
					int totalNumberOfSourceBeans) {
				FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>(source);
				bean.setName(beanName);
				FilterRegistration registrationAnnotation = this.beanFactory.findAnnotationOnBean(beanName,
						FilterRegistration.class);
				if (registrationAnnotation != null) {
					// Supports @Order annotation on @Bean methods
					Order orderAnnotation = this.beanFactory.findAnnotationOnBean(beanName, Order.class);
					Assert.state(orderAnnotation != null, "'orderAnnotation' must not be null");
					configureFromAnnotation(bean, registrationAnnotation, orderAnnotation);
				}
				return bean;
			}

			private void configureFromAnnotation(FilterRegistrationBean<Filter> bean,
					FilterRegistration registrationAnnotation, Order orderAnnotation) {
				bean.setEnabled(registrationAnnotation.enabled());
				bean.setOrder(orderAnnotation.value());
				if (StringUtils.hasText(registrationAnnotation.name())) {
					bean.setName(registrationAnnotation.name());
				}
				if (registrationAnnotation.dispatcherTypes().length > 0) {
					bean.setDispatcherTypes(EnumSet.copyOf(Arrays.asList(registrationAnnotation.dispatcherTypes())));
				}
				for (WebInitParam param : registrationAnnotation.initParameters()) {
					bean.addInitParameter(param.name(), param.value());
				}
				bean.setUrlPatterns(Arrays.asList(registrationAnnotation.urlPatterns()));
			}

		}

	}

}
