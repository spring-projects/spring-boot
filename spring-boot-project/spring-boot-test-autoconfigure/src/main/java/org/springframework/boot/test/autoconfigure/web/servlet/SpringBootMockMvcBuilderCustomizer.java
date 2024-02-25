/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.Collection;
import java.util.List;

import jakarta.servlet.Filter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.web.servlet.AbstractFilterRegistrationBean;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.RegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
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
 * @since 1.4.0
 */
public class SpringBootMockMvcBuilderCustomizer implements MockMvcBuilderCustomizer {

	private final WebApplicationContext context;

	private boolean addFilters = true;

	private MockMvcPrint print = MockMvcPrint.DEFAULT;

	private boolean printOnlyOnFailure = true;

	/**
	 * Create a new {@link SpringBootMockMvcBuilderCustomizer} instance.
	 * @param context the source application context
	 */
	public SpringBootMockMvcBuilderCustomizer(WebApplicationContext context) {
		Assert.notNull(context, "Context must not be null");
		this.context = context;
	}

	/**
     * Customize the ConfigurableMockMvcBuilder by adding filters and print handler if specified.
     * 
     * @param builder the ConfigurableMockMvcBuilder to be customized
     */
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

	/**
     * Returns a ResultHandler that handles printing the result of the request.
     * 
     * @return the ResultHandler for printing the result
     */
    private ResultHandler getPrintHandler() {
		LinesWriter writer = getLinesWriter();
		if (writer == null) {
			return null;
		}
		if (this.printOnlyOnFailure) {
			writer = new DeferredLinesWriter(this.context, writer);
		}
		return new LinesWritingResultHandler(writer);
	}

	/**
     * Returns a LinesWriter based on the current print configuration.
     * 
     * @return a LinesWriter object based on the current print configuration
     */
    private LinesWriter getLinesWriter() {
		if (this.print == MockMvcPrint.NONE) {
			return null;
		}
		if (this.print == MockMvcPrint.LOG_DEBUG) {
			return new LoggingLinesWriter();
		}
		return new SystemLinesWriter(this.print);
	}

	/**
     * Adds filters to the given {@link ConfigurableMockMvcBuilder}.
     * 
     * @param builder the {@link ConfigurableMockMvcBuilder} to add filters to
     */
    private void addFilters(ConfigurableMockMvcBuilder<?> builder) {
		FilterRegistrationBeans registrations = new FilterRegistrationBeans(this.context);
		registrations.stream()
			.map(AbstractFilterRegistrationBean.class::cast)
			.filter(AbstractFilterRegistrationBean<?>::isEnabled)
			.forEach((registration) -> addFilter(builder, registration));
	}

	/**
     * Adds a filter to the Spring Boot MockMvc builder.
     * 
     * @param builder the Spring Boot MockMvc builder to add the filter to
     * @param registration the filter registration bean containing the filter and its configuration
     */
    private void addFilter(ConfigurableMockMvcBuilder<?> builder, AbstractFilterRegistrationBean<?> registration) {
		Filter filter = registration.getFilter();
		Collection<String> urls = registration.getUrlPatterns();
		builder.addFilter(filter, registration.getFilterName(), registration.getInitParameters(),
				registration.determineDispatcherTypes(), StringUtils.toStringArray(urls));
	}

	/**
     * Sets whether to add filters.
     * 
     * @param addFilters true to add filters, false otherwise
     */
    public void setAddFilters(boolean addFilters) {
		this.addFilters = addFilters;
	}

	/**
     * Returns a boolean value indicating whether to add filters.
     *
     * @return true if filters should be added, false otherwise
     */
    public boolean isAddFilters() {
		return this.addFilters;
	}

	/**
     * Sets the print object for the SpringBootMockMvcBuilderCustomizer.
     * 
     * @param print the print object to be set
     */
    public void setPrint(MockMvcPrint print) {
		this.print = print;
	}

	/**
     * Returns the MockMvcPrint object associated with this SpringBootMockMvcBuilderCustomizer.
     *
     * @return the MockMvcPrint object associated with this SpringBootMockMvcBuilderCustomizer
     */
    public MockMvcPrint getPrint() {
		return this.print;
	}

	/**
     * Sets the flag to determine whether to print only on failure.
     * 
     * @param printOnlyOnFailure the flag indicating whether to print only on failure
     */
    public void setPrintOnlyOnFailure(boolean printOnlyOnFailure) {
		this.printOnlyOnFailure = printOnlyOnFailure;
	}

	/**
     * Returns a boolean value indicating whether the printOnlyOnFailure flag is enabled or not.
     * 
     * @return true if the printOnlyOnFailure flag is enabled, false otherwise
     */
    public boolean isPrintOnlyOnFailure() {
		return this.printOnlyOnFailure;
	}

	/**
	 * {@link ResultHandler} that prints {@link MvcResult} details to a given
	 * {@link LinesWriter}.
	 */
	private static class LinesWritingResultHandler implements ResultHandler {

		private final LinesWriter writer;

		/**
         * Constructs a new LinesWritingResultHandler with the specified LinesWriter.
         * 
         * @param writer the LinesWriter to be used for writing lines
         */
        LinesWritingResultHandler(LinesWriter writer) {
			this.writer = writer;
		}

		/**
         * Handles the given MvcResult by delegating the handling to a LinesPrintingResultHandler.
         * After handling the result, writes the result to the writer.
         * 
         * @param result the MvcResult to be handled
         * @throws Exception if an error occurs during handling
         */
        @Override
		public void handle(MvcResult result) throws Exception {
			LinesPrintingResultHandler delegate = new LinesPrintingResultHandler();
			delegate.handle(result);
			delegate.write(this.writer);
		}

		/**
         * LinesPrintingResultHandler class.
         */
        private static class LinesPrintingResultHandler extends PrintingResultHandler {

			/**
             * Constructs a new LinesPrintingResultHandler with a default Printer.
             */
            protected LinesPrintingResultHandler() {
				super(new Printer());
			}

			/**
             * Writes the lines from the printer to the specified writer.
             * 
             * @param writer the LinesWriter object to write the lines to
             */
            void write(LinesWriter writer) {
				writer.write(((Printer) getPrinter()).getLines());
			}

			/**
             * Printer class.
             */
            private static final class Printer implements ResultValuePrinter {

				private final List<String> lines = new ArrayList<>();

				/**
                 * Prints a heading with the specified text.
                 * 
                 * @param heading the text of the heading to be printed
                 */
                @Override
				public void printHeading(String heading) {
					this.lines.add("");
					this.lines.add(String.format("%s:", heading));
				}

				/**
                 * Prints the value with the given label.
                 * If the value is an array, it converts it to a list before printing.
                 *
                 * @param label the label to be printed
                 * @param value the value to be printed
                 */
                @Override
				public void printValue(String label, Object value) {
					if (value != null && value.getClass().isArray()) {
						value = CollectionUtils.arrayToList(value);
					}
					this.lines.add(String.format("%17s = %s", label, value));
				}

				/**
                 * Returns the list of lines stored in the Printer object.
                 *
                 * @return the list of lines
                 */
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

		/**
         * Constructs a new DeferredLinesWriter with the specified WebApplicationContext and LinesWriter delegate.
         * 
         * @param context the WebApplicationContext to be used
         * @param delegate the LinesWriter delegate to be used
         * @throws IllegalArgumentException if the context is not an instance of ConfigurableApplicationContext
         * @throws NullPointerException if the context or delegate is null
         */
        DeferredLinesWriter(WebApplicationContext context, LinesWriter delegate) {
			Assert.state(context instanceof ConfigurableApplicationContext,
					"A ConfigurableApplicationContext is required for printOnlyOnFailure");
			((ConfigurableApplicationContext) context).getBeanFactory().registerSingleton(BEAN_NAME, this);
			this.delegate = delegate;
		}

		/**
         * Writes the given list of lines to the deferred lines writer.
         * 
         * @param lines the list of lines to be written
         */
        @Override
		public void write(List<String> lines) {
			this.lines.get().addAll(lines);
		}

		/**
         * Writes the deferred result to the delegate.
         * 
         * This method retrieves the lines from the DeferredLinesWriter and writes them to the delegate.
         * 
         * @throws NullPointerException if the delegate is null
         */
        void writeDeferredResult() {
			this.delegate.write(this.lines.get());
		}

		/**
         * Retrieves an instance of DeferredLinesWriter from the given ApplicationContext.
         * 
         * @param applicationContext the ApplicationContext from which to retrieve the DeferredLinesWriter instance
         * @return the DeferredLinesWriter instance retrieved from the ApplicationContext, or null if it does not exist
         */
        static DeferredLinesWriter get(ApplicationContext applicationContext) {
			try {
				return applicationContext.getBean(BEAN_NAME, DeferredLinesWriter.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				return null;
			}
		}

		/**
         * Removes all lines from the DeferredLinesWriter.
         */
        void clear() {
			this.lines.remove();
		}

	}

	/**
	 * {@link LinesWriter} to output results to the log.
	 */
	private static final class LoggingLinesWriter implements LinesWriter {

		private static final Log logger = LogFactory.getLog("org.springframework.test.web.servlet.result");

		/**
         * Writes the given list of lines to the logger in debug mode.
         * 
         * @param lines the list of lines to be written
         */
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

	}

	/**
	 * {@link LinesWriter} to output results to {@code System.out} or {@code System.err}.
	 */
	private static class SystemLinesWriter implements LinesWriter {

		private final MockMvcPrint print;

		/**
         * Constructs a new SystemLinesWriter with the specified MockMvcPrint.
         *
         * @param print the MockMvcPrint to be used by the SystemLinesWriter
         */
        SystemLinesWriter(MockMvcPrint print) {
			this.print = print;
		}

		/**
         * Writes the given list of lines to the output stream.
         * 
         * @param lines the list of lines to be written
         */
        @Override
		public void write(List<String> lines) {
			PrintStream printStream = getPrintStream();
			for (String line : lines) {
				printStream.println(line);
			}
		}

		/**
         * Returns the PrintStream to be used for writing output.
         * 
         * @return the PrintStream to be used for writing output
         */
        private PrintStream getPrintStream() {
			if (this.print == MockMvcPrint.SYSTEM_ERR) {
				return System.err;
			}
			return System.out;
		}

	}

	/**
     * FilterRegistrationBeans class.
     */
    private static class FilterRegistrationBeans extends ServletContextInitializerBeans {

		/**
         * Constructs a new FilterRegistrationBeans object with the specified ListableBeanFactory.
         * 
         * @param beanFactory the ListableBeanFactory used to retrieve the filter registration beans
         */
        FilterRegistrationBeans(ListableBeanFactory beanFactory) {
			super(beanFactory, FilterRegistrationBean.class, DelegatingFilterProxyRegistrationBean.class);
		}

		/**
         * Adds adaptable beans to the given bean factory.
         * 
         * @param beanFactory the bean factory to add the adaptable beans to
         */
        @Override
		protected void addAdaptableBeans(ListableBeanFactory beanFactory) {
			addAsRegistrationBean(beanFactory, Filter.class, new FilterRegistrationBeanAdapter());
		}

		/**
         * FilterRegistrationBeanAdapter class.
         */
        private static final class FilterRegistrationBeanAdapter implements RegistrationBeanAdapter<Filter> {

			/**
             * Creates a new RegistrationBean with the specified name, source Filter, and total number of source beans.
             * 
             * @param name The name of the RegistrationBean.
             * @param source The source Filter for the RegistrationBean.
             * @param totalNumberOfSourceBeans The total number of source beans.
             * @return The created RegistrationBean.
             */
            @Override
			public RegistrationBean createRegistrationBean(String name, Filter source, int totalNumberOfSourceBeans) {
				FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>(source);
				bean.setName(name);
				return bean;
			}

		}

	}

}
