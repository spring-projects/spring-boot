/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.context;

import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link AssertProviderApplicationContext} and
 * {@link AssertProviderApplicationContextInvocationHandler}.
 *
 * @author Phillip Webb
 */
public class AssertProviderApplicationContextTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	private ConfigurableApplicationContext mockContext;

	private RuntimeException startupFailure;

	private Supplier<ApplicationContext> mockContextSupplier;

	private Supplier<ApplicationContext> startupFailureSupplier;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.startupFailure = new RuntimeException();
		this.mockContextSupplier = () -> this.mockContext;
		this.startupFailureSupplier = () -> {
			throw this.startupFailure;
		};
	}

	@Test
	public void getWhenTypeIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Type must not be null");
		AssertProviderApplicationContext.get(null, ApplicationContext.class,
				this.mockContextSupplier);
	}

	@Test
	public void getWhenTypeIsClassShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Type must not be null");
		AssertProviderApplicationContext.get(null, ApplicationContext.class,
				this.mockContextSupplier);
	}

	@Test
	public void getWhenContextTypeIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Type must be an interface");
		AssertProviderApplicationContext.get(
				TestAssertProviderApplicationContextClass.class, ApplicationContext.class,
				this.mockContextSupplier);
	}

	@Test
	public void getWhenContextTypeIsClassShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ContextType must not be null");
		AssertProviderApplicationContext.get(TestAssertProviderApplicationContext.class,
				null, this.mockContextSupplier);
	}

	@Test
	public void getWhenSupplierIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ContextType must be an interface");
		AssertProviderApplicationContext.get(TestAssertProviderApplicationContext.class,
				StaticApplicationContext.class, this.mockContextSupplier);
	}

	@Test
	public void getWhenContextStartsShouldReturnProxyThatCallsRealMethods()
			throws Exception {
		AssertProviderApplicationContext<ApplicationContext> context = get(
				this.mockContextSupplier);
		assertThat((Object) context).isNotNull();
		context.getBean("foo");
		verify(this.mockContext).getBean("foo");
	}

	@Test
	public void getWhenContextFailsShouldReturnProxyThatThrowsExceptions()
			throws Exception {
		AssertProviderApplicationContext<ApplicationContext> context = get(
				this.startupFailureSupplier);
		assertThat((Object) context).isNotNull();
		expectStartupFailure();
		context.getBean("foo");
	}

	@Test
	public void getSourceContextWhenContextStartsShouldReturnSourceContext()
			throws Exception {
		AssertProviderApplicationContext<ApplicationContext> context = get(
				this.mockContextSupplier);
		assertThat(context.getSourceApplicationContext()).isSameAs(this.mockContext);
	}

	@Test
	public void getSourceContextWhenContextFailsShouldThrowException() throws Exception {
		AssertProviderApplicationContext<ApplicationContext> context = get(
				this.startupFailureSupplier);
		expectStartupFailure();
		context.getSourceApplicationContext();
	}

	@Test
	public void getSourceContextOfTypeWhenContextStartsShouldReturnSourceContext()
			throws Exception {
		AssertProviderApplicationContext<ApplicationContext> context = get(
				this.mockContextSupplier);
		assertThat(context.getSourceApplicationContext(ApplicationContext.class))
				.isSameAs(this.mockContext);
	}

	@Test
	public void getSourceContextOfTypeWhenContextFailsToStartShouldThrowException()
			throws Exception {
		AssertProviderApplicationContext<ApplicationContext> context = get(
				this.startupFailureSupplier);
		expectStartupFailure();
		context.getSourceApplicationContext(ApplicationContext.class);
	}

	@Test
	public void getStartupFailureWhenContextStartsShouldReturnNull() throws Exception {
		AssertProviderApplicationContext<ApplicationContext> context = get(
				this.mockContextSupplier);
		assertThat(context.getStartupFailure()).isNull();
	}

	@Test
	public void getStartupFailureWhenContextFailsToStartShouldReturnException()
			throws Exception {
		AssertProviderApplicationContext<ApplicationContext> context = get(
				this.startupFailureSupplier);
		assertThat(context.getStartupFailure()).isEqualTo(this.startupFailure);
	}

	@Test
	public void assertThatWhenContextStartsShouldReturnAssertions() throws Exception {
		AssertProviderApplicationContext<ApplicationContext> context = get(
				this.mockContextSupplier);
		ApplicationContextAssert<ApplicationContext> contextAssert = assertThat(context);
		assertThat(contextAssert.getApplicationContext()).isSameAs(context);
		assertThat(contextAssert.getStartupFailure()).isNull();
	}

	@Test
	public void assertThatWhenContextFailsShouldReturnAssertions() throws Exception {
		AssertProviderApplicationContext<ApplicationContext> context = get(
				this.startupFailureSupplier);
		ApplicationContextAssert<ApplicationContext> contextAssert = assertThat(context);
		assertThat(contextAssert.getApplicationContext()).isSameAs(context);
		assertThat(contextAssert.getStartupFailure()).isSameAs(this.startupFailure);
	}

	@Test
	public void toStringWhenContextStartsShouldReturnSimpleString() throws Exception {
		AssertProviderApplicationContext<ApplicationContext> context = get(
				this.mockContextSupplier);
		assertThat(context.toString())
				.startsWith(
						"Started application org.springframework.context.ConfigurableApplicationContext$MockitoMock")
				.endsWith("[id=<null>,applicationName=<null>,beanDefinitionCount=0]");
	}

	@Test
	public void toStringWhenContextFailsToStartShouldReturnSimpleString()
			throws Exception {
		AssertProviderApplicationContext<ApplicationContext> context = get(
				this.startupFailureSupplier);
		assertThat(context.toString()).isEqualTo("Unstarted application context "
				+ "org.springframework.context.ApplicationContext"
				+ "[startupFailure=java.lang.RuntimeException]");
	}

	@Test
	public void closeShouldCloseContext() throws Exception {
		AssertProviderApplicationContext<ApplicationContext> context = get(
				this.mockContextSupplier);
		context.close();
		verify(this.mockContext).close();
	}

	private void expectStartupFailure() {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("failed to start");
		this.thrown.expectCause(equalTo(this.startupFailure));
	}

	private AssertProviderApplicationContext<ApplicationContext> get(
			Supplier<ApplicationContext> contextSupplier) {
		return AssertProviderApplicationContext.get(
				TestAssertProviderApplicationContext.class, ApplicationContext.class,
				contextSupplier);
	}

	private interface TestAssertProviderApplicationContext
			extends AssertProviderApplicationContext<ApplicationContext> {

	}

	private abstract static class TestAssertProviderApplicationContextClass
			implements TestAssertProviderApplicationContext {

	}

}
