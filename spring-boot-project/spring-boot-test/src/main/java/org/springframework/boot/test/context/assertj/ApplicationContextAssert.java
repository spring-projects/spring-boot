/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.test.context.assertj;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractObjectArrayAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.MapAssert;
import org.assertj.core.error.BasicErrorMessageFactory;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AssertJ {@link org.assertj.core.api.Assert assertions} that can be applied to an
 * {@link ApplicationContext}.
 *
 * @param <C> the application context type
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 2.0.0
 * @see ApplicationContextRunner
 * @see AssertableApplicationContext
 */
public class ApplicationContextAssert<C extends ApplicationContext>
		extends AbstractAssert<ApplicationContextAssert<C>, C> {

	private final Throwable startupFailure;

	/**
	 * Create a new {@link ApplicationContextAssert} instance.
	 * @param applicationContext the source application context
	 * @param startupFailure the startup failure or {@code null}
	 */
	ApplicationContextAssert(C applicationContext, Throwable startupFailure) {
		super(applicationContext, ApplicationContextAssert.class);
		Assert.notNull(applicationContext, "ApplicationContext must not be null");
		this.startupFailure = startupFailure;
	}

	/**
	 * Verifies that the application context contains a bean with the given name.
	 * <p>
	 * Example: <pre class="code">
	 * assertThat(context).hasBean("fooBean"); </pre>
	 * @param name the name of the bean
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the application context did not start
	 * @throws AssertionError if the application context does not contain a bean with the
	 * given name
	 */
	public ApplicationContextAssert<C> hasBean(String name) {
		if (this.startupFailure != null) {
			throwAssertionError(contextFailedToStartWhenExpecting("to have bean named:%n <%s>", name));
		}
		if (findBean(name) == null) {
			throwAssertionError(new BasicErrorMessageFactory(
					"%nExpecting:%n <%s>%nto have bean named:%n <%s>%nbut found no such bean", getApplicationContext(),
					name));
		}
		return this;
	}

	/**
	 * Verifies that the application context (or ancestors) contains a single bean with
	 * the given type.
	 * <p>
	 * Example: <pre class="code">
	 * assertThat(context).hasSingleBean(Foo.class); </pre>
	 * @param type the bean type
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the application context did not start
	 * @throws AssertionError if the application context does no beans of the given type
	 * @throws AssertionError if the application context contains multiple beans of the
	 * given type
	 */
	public ApplicationContextAssert<C> hasSingleBean(Class<?> type) {
		return hasSingleBean(type, Scope.INCLUDE_ANCESTORS);
	}

	/**
	 * Verifies that the application context contains a single bean with the given type.
	 * <p>
	 * Example: <pre class="code">
	 * assertThat(context).hasSingleBean(Foo.class, Scope.NO_ANCESTORS); </pre>
	 * @param type the bean type
	 * @param scope the scope of the assertion
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the application context did not start
	 * @throws AssertionError if the application context does no beans of the given type
	 * @throws AssertionError if the application context contains multiple beans of the
	 * given type
	 */
	public ApplicationContextAssert<C> hasSingleBean(Class<?> type, Scope scope) {
		Assert.notNull(scope, "Scope must not be null");
		if (this.startupFailure != null) {
			throwAssertionError(contextFailedToStartWhenExpecting("to have a single bean of type:%n <%s>", type));
		}
		String[] names = scope.getBeanNamesForType(getApplicationContext(), type);
		if (names.length == 0) {
			throwAssertionError(new BasicErrorMessageFactory(
					"%nExpecting:%n <%s>%nto have a single bean of type:%n <%s>%nbut found no beans of that type",
					getApplicationContext(), type));
		}
		if (names.length > 1) {
			throwAssertionError(new BasicErrorMessageFactory(
					"%nExpecting:%n <%s>%nto have a single bean of type:%n <%s>%nbut found:%n <%s>",
					getApplicationContext(), type, names));
		}
		return this;
	}

	/**
	 * Verifies that the application context (or ancestors) does not contain any beans of
	 * the given type.
	 * <p>
	 * Example: <pre class="code">
	 * assertThat(context).doesNotHaveBean(Foo.class); </pre>
	 * @param type the bean type
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the application context did not start
	 * @throws AssertionError if the application context contains any beans of the given
	 * type
	 */
	public ApplicationContextAssert<C> doesNotHaveBean(Class<?> type) {
		return doesNotHaveBean(type, Scope.INCLUDE_ANCESTORS);
	}

	/**
	 * Verifies that the application context does not contain any beans of the given type.
	 * <p>
	 * Example: <pre class="code">
	 * assertThat(context).doesNotHaveBean(Foo.class, Scope.NO_ANCESTORS); </pre>
	 * @param type the bean type
	 * @param scope the scope of the assertion
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the application context did not start
	 * @throws AssertionError if the application context contains any beans of the given
	 * type
	 */
	public ApplicationContextAssert<C> doesNotHaveBean(Class<?> type, Scope scope) {
		Assert.notNull(scope, "Scope must not be null");
		if (this.startupFailure != null) {
			throwAssertionError(contextFailedToStartWhenExpecting("not to have any beans of type:%n <%s>", type));
		}
		String[] names = scope.getBeanNamesForType(getApplicationContext(), type);
		if (names.length > 0) {
			throwAssertionError(new BasicErrorMessageFactory(
					"%nExpecting:%n <%s>%nnot to have any beans of type:%n <%s>%nbut found:%n <%s>",
					getApplicationContext(), type, names));
		}
		return this;
	}

	/**
	 * Verifies that the application context does not contain a beans of the given name.
	 * <p>
	 * Example: <pre class="code">
	 * assertThat(context).doesNotHaveBean("fooBean"); </pre>
	 * @param name the name of the bean
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the application context did not start
	 * @throws AssertionError if the application context contains a beans of the given
	 * name
	 */
	public ApplicationContextAssert<C> doesNotHaveBean(String name) {
		if (this.startupFailure != null) {
			throwAssertionError(contextFailedToStartWhenExpecting("not to have any beans of name:%n <%s>", name));
		}
		try {
			Object bean = getApplicationContext().getBean(name);
			throwAssertionError(new BasicErrorMessageFactory(
					"%nExpecting:%n <%s>%nnot to have a bean of name:%n <%s>%nbut found:%n <%s>",
					getApplicationContext(), name, bean));
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Ignore
		}
		return this;
	}

	/**
	 * Obtain the beans names of the given type from the application context, the names
	 * becoming the object array under test.
	 * <p>
	 * Example: <pre class="code">
	 * assertThat(context).getBeanNames(Foo.class).containsOnly("fooBean"); </pre>
	 * @param <T> the bean type
	 * @param type the bean type
	 * @return array assertions for the bean names
	 * @throws AssertionError if the application context did not start
	 */
	public <T> AbstractObjectArrayAssert<?, String> getBeanNames(Class<T> type) {
		if (this.startupFailure != null) {
			throwAssertionError(contextFailedToStartWhenExpecting("to get beans names with type:%n <%s>", type));
		}
		return Assertions.assertThat(getApplicationContext().getBeanNamesForType(type))
			.as("Bean names of type <%s> from <%s>", type, getApplicationContext());
	}

	/**
	 * Obtain a single bean of the given type from the application context (or ancestors),
	 * the bean becoming the object under test. If no beans of the specified type can be
	 * found an assert on {@code null} is returned.
	 * <p>
	 * Example: <pre class="code">
	 * assertThat(context).getBean(Foo.class).isInstanceOf(DefaultFoo.class);
	 * assertThat(context).getBean(Bar.class).isNull();</pre>
	 * @param <T> the bean type
	 * @param type the bean type
	 * @return bean assertions for the bean, or an assert on {@code null} if the no bean
	 * is found
	 * @throws AssertionError if the application context did not start
	 * @throws AssertionError if the application context contains multiple beans of the
	 * given type
	 */
	public <T> AbstractObjectAssert<?, T> getBean(Class<T> type) {
		return getBean(type, Scope.INCLUDE_ANCESTORS);
	}

	/**
	 * Obtain a single bean of the given type from the application context, the bean
	 * becoming the object under test. If no beans of the specified type can be found an
	 * assert on {@code null} is returned.
	 * <p>
	 * Example: <pre class="code">
	 * assertThat(context).getBean(Foo.class, Scope.NO_ANCESTORS).isInstanceOf(DefaultFoo.class);
	 * assertThat(context).getBean(Bar.class, Scope.NO_ANCESTORS).isNull();</pre>
	 * @param <T> the bean type
	 * @param type the bean type
	 * @param scope the scope of the assertion
	 * @return bean assertions for the bean, or an assert on {@code null} if the no bean
	 * is found
	 * @throws AssertionError if the application context did not start
	 * @throws AssertionError if the application context contains multiple beans of the
	 * given type
	 */
	public <T> AbstractObjectAssert<?, T> getBean(Class<T> type, Scope scope) {
		Assert.notNull(scope, "Scope must not be null");
		if (this.startupFailure != null) {
			throwAssertionError(contextFailedToStartWhenExpecting("to contain bean of type:%n <%s>", type));
		}
		String[] names = scope.getBeanNamesForType(getApplicationContext(), type);
		String name = (names.length > 0) ? getPrimary(names, scope) : null;
		if (names.length > 1 && name == null) {
			throwAssertionError(
					new BasicErrorMessageFactory("%nExpecting:%n <%s>%nsingle bean of type:%n <%s>%nbut found:%n <%s>",
							getApplicationContext(), type, names));
		}
		T bean = (name != null) ? getApplicationContext().getBean(name, type) : null;
		return Assertions.assertThat(bean).as("Bean of type <%s> from <%s>", type, getApplicationContext());
	}

	/**
	 * Returns the primary name from the given array of names based on the specified
	 * scope. If there is only one name in the array, that name is considered as the
	 * primary name. If multiple names are found to be primary based on the specified
	 * scope, null is returned.
	 * @param names the array of names to search for the primary name
	 * @param scope the scope to determine the primary name
	 * @return the primary name if found, null otherwise
	 */
	private String getPrimary(String[] names, Scope scope) {
		if (names.length == 1) {
			return names[0];
		}
		String primary = null;
		for (String name : names) {
			if (isPrimary(name, scope)) {
				if (primary != null) {
					return null;
				}
				primary = name;
			}
		}
		return primary;
	}

	/**
	 * Checks if a bean with the given name is marked as primary in the application
	 * context.
	 * @param name the name of the bean to check
	 * @param scope the scope of the search for the bean (NO_ANCESTORS or ALL_ANCESTORS)
	 * @return true if the bean is marked as primary, false otherwise
	 */
	private boolean isPrimary(String name, Scope scope) {
		ApplicationContext context = getApplicationContext();
		while (context != null) {
			if (context instanceof ConfigurableApplicationContext configurableContext) {
				ConfigurableListableBeanFactory factory = configurableContext.getBeanFactory();
				if (factory.containsBean(name) && factory.getMergedBeanDefinition(name).isPrimary()) {
					return true;
				}
			}
			context = (scope != Scope.NO_ANCESTORS) ? context.getParent() : null;
		}
		return false;
	}

	/**
	 * Obtain a single bean of the given name from the application context, the bean
	 * becoming the object under test. If no bean of the specified name can be found an
	 * assert on {@code null} is returned.
	 * <p>
	 * Example: <pre class="code">
	 * assertThat(context).getBean("foo").isInstanceOf(Foo.class);
	 * assertThat(context).getBean("foo").isNull();</pre>
	 * @param name the name of the bean
	 * @return bean assertions for the bean, or an assert on {@code null} if the no bean
	 * is found
	 * @throws AssertionError if the application context did not start
	 */
	public AbstractObjectAssert<?, Object> getBean(String name) {
		if (this.startupFailure != null) {
			throwAssertionError(contextFailedToStartWhenExpecting("to contain a bean of name:%n <%s>", name));
		}
		Object bean = findBean(name);
		return Assertions.assertThat(bean).as("Bean of name <%s> from <%s>", name, getApplicationContext());
	}

	/**
	 * Obtain a single bean of the given name and type from the application context, the
	 * bean becoming the object under test. If no bean of the specified name can be found
	 * an assert on {@code null} is returned.
	 * <p>
	 * Example: <pre class="code">
	 * assertThat(context).getBean("foo", Foo.class).isInstanceOf(DefaultFoo.class);
	 * assertThat(context).getBean("foo", Foo.class).isNull();</pre>
	 * @param <T> the bean type
	 * @param name the name of the bean
	 * @param type the bean type
	 * @return bean assertions for the bean, or an assert on {@code null} if the no bean
	 * is found
	 * @throws AssertionError if the application context did not start
	 * @throws AssertionError if the application context contains a bean with the given
	 * name but a different type
	 */
	@SuppressWarnings("unchecked")
	public <T> AbstractObjectAssert<?, T> getBean(String name, Class<T> type) {
		if (this.startupFailure != null) {
			throwAssertionError(
					contextFailedToStartWhenExpecting("to contain a bean of name:%n <%s> (%s)", name, type));
		}
		Object bean = findBean(name);
		if (bean != null && type != null && !type.isInstance(bean)) {
			throwAssertionError(new BasicErrorMessageFactory(
					"%nExpecting:%n <%s>%nto contain a bean of name:%n <%s> (%s)%nbut found:%n <%s> of type <%s>",
					getApplicationContext(), name, type, bean, bean.getClass()));
		}
		return Assertions.assertThat((T) bean)
			.as("Bean of name <%s> and type <%s> from <%s>", name, type, getApplicationContext());
	}

	/**
	 * Finds a bean with the given name in the application context.
	 * @param name the name of the bean to find
	 * @return the bean object if found, or null if not found
	 */
	private Object findBean(String name) {
		try {
			return getApplicationContext().getBean(name);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return null;
		}
	}

	/**
	 * Obtain a map bean names and instances of the given type from the application
	 * context (or ancestors), the map becoming the object under test. If no bean of the
	 * specified type can be found an assert on an empty {@code map} is returned.
	 * <p>
	 * Example: <pre class="code">
	 * assertThat(context).getBeans(Foo.class).containsKey("foo");
	 * </pre>
	 * @param <T> the bean type
	 * @param type the bean type
	 * @return bean assertions for the beans, or an assert on an empty {@code map} if the
	 * no beans are found
	 * @throws AssertionError if the application context did not start
	 */
	public <T> MapAssert<String, T> getBeans(Class<T> type) {
		return getBeans(type, Scope.INCLUDE_ANCESTORS);
	}

	/**
	 * Obtain a map bean names and instances of the given type from the application
	 * context, the map becoming the object under test. If no bean of the specified type
	 * can be found an assert on an empty {@code map} is returned.
	 * <p>
	 * Example: <pre class="code">
	 * assertThat(context).getBeans(Foo.class, Scope.NO_ANCESTORS).containsKey("foo");
	 * </pre>
	 * @param <T> the bean type
	 * @param type the bean type
	 * @param scope the scope of the assertion
	 * @return bean assertions for the beans, or an assert on an empty {@code map} if the
	 * no beans are found
	 * @throws AssertionError if the application context did not start
	 */
	public <T> MapAssert<String, T> getBeans(Class<T> type, Scope scope) {
		Assert.notNull(scope, "Scope must not be null");
		if (this.startupFailure != null) {
			throwAssertionError(contextFailedToStartWhenExpecting("to get beans of type:%n <%s>", type));
		}
		return Assertions.assertThat(scope.getBeansOfType(getApplicationContext(), type))
			.as("Beans of type <%s> from <%s>", type, getApplicationContext());
	}

	/**
	 * Obtain the failure that stopped the application context from running, the failure
	 * becoming the object under test.
	 * <p>
	 * Example: <pre class="code">
	 * assertThat(context).getFailure().containsMessage("missing bean");
	 * </pre>
	 * @return assertions on the cause of the failure
	 * @throws AssertionError if the application context started without a failure
	 */
	public AbstractThrowableAssert<?, ? extends Throwable> getFailure() {
		hasFailed();
		return assertThat(this.startupFailure);
	}

	/**
	 * Verifies that the application has failed to start.
	 * <p>
	 * Example: <pre class="code"> assertThat(context).hasFailed();
	 * </pre>
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the application context started without a failure
	 */
	public ApplicationContextAssert<C> hasFailed() {
		if (this.startupFailure == null) {
			throwAssertionError(new BasicErrorMessageFactory(
					"%nExpecting:%n <%s>%nto have failed%nbut context started successfully", getApplicationContext()));
		}
		return this;
	}

	/**
	 * Verifies that the application has not failed to start.
	 * <p>
	 * Example: <pre class="code"> assertThat(context).hasNotFailed();
	 * </pre>
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the application context failed to start
	 */
	public ApplicationContextAssert<C> hasNotFailed() {
		if (this.startupFailure != null) {
			throwAssertionError(contextFailedToStartWhenExpecting("to have not failed"));
		}
		return this;
	}

	/**
	 * Returns the application context.
	 * @return the application context
	 */
	protected final C getApplicationContext() {
		return this.actual;
	}

	/**
	 * Returns the startup failure exception that occurred during the application context
	 * startup.
	 * @return the startup failure exception, or {@code null} if no exception occurred
	 */
	protected final Throwable getStartupFailure() {
		return this.startupFailure;
	}

	/**
	 * Creates a new instance of {@link ContextFailedToStart} with the application
	 * context, startup failure, and expectation format.
	 * @param expectationFormat the format string for the expectation
	 * @param arguments the arguments to be formatted in the expectation format string
	 * @return a new instance of {@link ContextFailedToStart}
	 */
	private ContextFailedToStart<C> contextFailedToStartWhenExpecting(String expectationFormat, Object... arguments) {
		return new ContextFailedToStart<>(getApplicationContext(), this.startupFailure, expectationFormat, arguments);
	}

	/**
	 * The scope of an assertion.
	 */
	public enum Scope {

		/**
		 * Limited to the current context.
		 */
		NO_ANCESTORS {

			/**
			 * Retrieves the names of all beans of the specified type from the given
			 * application context.
			 * @param applicationContext the application context from which to retrieve
			 * the bean names
			 * @param type the type of beans to retrieve
			 * @return an array of bean names of the specified type
			 */
			@Override
			String[] getBeanNamesForType(ApplicationContext applicationContext, Class<?> type) {
				return applicationContext.getBeanNamesForType(type);
			}

			/**
			 * Retrieves all beans of the specified type from the given application
			 * context.
			 * @param applicationContext the application context from which to retrieve
			 * the beans
			 * @param type the type of beans to retrieve
			 * @param <T> the generic type of the beans
			 * @return a map of bean names to bean instances of the specified type
			 */
			@Override
			<T> Map<String, T> getBeansOfType(ApplicationContext applicationContext, Class<T> type) {
				return applicationContext.getBeansOfType(type);
			}

		},

		/**
		 * Consider the ancestor contexts as well as the current context.
		 */
		INCLUDE_ANCESTORS {

			/**
			 * Retrieves the names of all beans of the specified type, including beans
			 * defined in ancestor contexts.
			 * @param applicationContext the ApplicationContext to retrieve the bean names
			 * from
			 * @param type the type of beans to retrieve
			 * @return an array of bean names of the specified type
			 */
			@Override
			String[] getBeanNamesForType(ApplicationContext applicationContext, Class<?> type) {
				return BeanFactoryUtils.beanNamesForTypeIncludingAncestors(applicationContext, type);
			}

			/**
			 * Retrieves all beans of the specified type from the given application
			 * context, including beans from ancestor contexts.
			 * @param applicationContext the application context from which to retrieve
			 * the beans
			 * @param type the type of beans to retrieve
			 * @param <T> the generic type of the beans
			 * @return a map of bean names to bean instances of the specified type
			 */
			@Override
			<T> Map<String, T> getBeansOfType(ApplicationContext applicationContext, Class<T> type) {
				return BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, type);
			}

		};

		/**
		 * Retrieve the names of all beans of the specified type or subtype in the given
		 * application context.
		 * @param applicationContext the application context to retrieve the bean names
		 * from
		 * @param type the type or subtype of beans to retrieve
		 * @return an array of bean names matching the specified type or subtype
		 */
		abstract String[] getBeanNamesForType(ApplicationContext applicationContext, Class<?> type);

		/**
		 * Retrieves all beans of the specified type from the given ApplicationContext.
		 * @param applicationContext the ApplicationContext from which to retrieve the
		 * beans
		 * @param type the type of beans to retrieve
		 * @param <T> the generic type of the beans
		 * @return a Map containing the beans of the specified type, with the bean names
		 * as keys
		 */
		abstract <T> Map<String, T> getBeansOfType(ApplicationContext applicationContext, Class<T> type);

	}

	/**
	 * ContextFailedToStart class.
	 */
	private static final class ContextFailedToStart<C extends ApplicationContext> extends BasicErrorMessageFactory {

		/**
		 * Constructs a new ContextFailedToStart exception with the specified context,
		 * throwable, expectation format, and arguments.
		 * @param context the context that failed to start
		 * @param ex the throwable that caused the failure
		 * @param expectationFormat the format string for the expectation
		 * @param arguments the arguments to be formatted in the expectation format
		 */
		private ContextFailedToStart(C context, Throwable ex, String expectationFormat, Object... arguments) {
			super("%nExpecting:%n <%s>%n" + expectationFormat + ":%nbut context failed to start:%n%s",
					combineArguments(context.toString(), ex, arguments));
		}

		/**
		 * Combines the given context, exception, and arguments into a single array.
		 * @param context the context to be combined as the first element of the array
		 * @param ex the exception to be combined as the last element of the array
		 * @param arguments the arguments to be combined in between the context and
		 * exception
		 * @return the combined array of context, arguments, and exception
		 */
		private static Object[] combineArguments(String context, Throwable ex, Object[] arguments) {
			Object[] combinedArguments = new Object[arguments.length + 2];
			combinedArguments[0] = unquotedString(context);
			System.arraycopy(arguments, 0, combinedArguments, 1, arguments.length);
			combinedArguments[combinedArguments.length - 1] = unquotedString(getIndentedStackTraceAsString(ex));
			return combinedArguments;
		}

		/**
		 * Returns the indented stack trace as a string for the given Throwable object.
		 * @param ex the Throwable object for which the stack trace is to be retrieved
		 * @return the indented stack trace as a string
		 */
		private static String getIndentedStackTraceAsString(Throwable ex) {
			String stackTrace = getStackTraceAsString(ex);
			return indent(stackTrace);
		}

		/**
		 * Converts the stack trace of a given Throwable object into a string
		 * representation.
		 * @param ex the Throwable object whose stack trace needs to be converted
		 * @return a string representation of the stack trace
		 */
		private static String getStackTraceAsString(Throwable ex) {
			StringWriter writer = new StringWriter();
			PrintWriter printer = new PrintWriter(writer);
			ex.printStackTrace(printer);
			return writer.toString();
		}

		/**
		 * Indents the given input string by adding a space at the beginning of each line.
		 * @param input the input string to be indented
		 * @return the indented string
		 */
		private static String indent(String input) {
			BufferedReader reader = new BufferedReader(new StringReader(input));
			StringWriter writer = new StringWriter();
			PrintWriter printer = new PrintWriter(writer);
			reader.lines().forEach((line) -> {
				printer.print(" ");
				printer.println(line);
			});
			return writer.toString();
		}

	}

}
