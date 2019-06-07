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

package org.springframework.boot.test.context.assertj;

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
 * Tests for {@link ApplicationContextAssertProvider} and
 * {@link AssertProviderApplicationContextInvocationHandler}.
 *
 * @author Phillip Webb
 */
public class ApplicationContextAssertProviderTests {

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
	public void getWhenTypeIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Type must not be null");
		ApplicationContextAssertProvider.get(null, ApplicationContext.class, this.mockContextSupplier);
	}

	@Test
	public void getWhenTypeIsClassShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Type must not be null");
		ApplicationContextAssertProvider.get(null, ApplicationContext.class, this.mockContextSupplier);
	}

	@Test
	public void getWhenContextTypeIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Type must be an interface");
		ApplicationContextAssertProvider.get(TestAssertProviderApplicationContextClass.class, ApplicationContext.class,
				this.mockContextSupplier);
	}

	@Test
	public void getWhenContextTypeIsClassShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ContextType must not be null");
		ApplicationContextAssertProvider.get(TestAssertProviderApplicationContext.class, null,
				this.mockContextSupplier);
	}

	@Test
	public void getWhenSupplierIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ContextType must be an interface");
		ApplicationContextAssertProvider.get(TestAssertProviderApplicationContext.class, StaticApplicationContext.class,
				this.mockContextSupplier);
	}

	@Test
	public void getWhenContextStartsShouldReturnProxyThatCallsRealMethods() {
		ApplicationContextAssertProvider<ApplicationContext> context = get(this.mockContextSupplier);
		assertThat((Object) context).isNotNull();
		context.getBean("foo");
		verify(this.mockContext).getBean("foo");
	}

	@Test
	public void getWhenContextFailsShouldReturnProxyThatThrowsExceptions() {
		ApplicationContextAssertProvider<ApplicationContext> context = get(this.startupFailureSupplier);
		assertThat((Object) context).isNotNull();
		expectStartupFailure();
		context.getBean("foo");
	}

	@Test
	public void getSourceContextWhenContextStartsShouldReturnSourceContext() {
		ApplicationContextAssertProvider<ApplicationContext> context = get(this.mockContextSupplier);
		assertThat(context.getSourceApplicationContext()).isSameAs(this.mockContext);
	}

	@Test
	public void getSourceContextWhenContextFailsShouldThrowException() {
		ApplicationContextAssertProvider<ApplicationContext> context = get(this.startupFailureSupplier);
		expectStartupFailure();
		context.getSourceApplicationContext();
	}

	@Test
	public void getSourceContextOfTypeWhenContextStartsShouldReturnSourceContext() {
		ApplicationContextAssertProvider<ApplicationContext> context = get(this.mockContextSupplier);
		assertThat(context.getSourceApplicationContext(ApplicationContext.class)).isSameAs(this.mockContext);
	}

	@Test
	public void getSourceContextOfTypeWhenContextFailsToStartShouldThrowException() {
		ApplicationContextAssertProvider<ApplicationContext> context = get(this.startupFailureSupplier);
		expectStartupFailure();
		context.getSourceApplicationContext(ApplicationContext.class);
	}

	@Test
	public void getStartupFailureWhenContextStartsShouldReturnNull() {
		ApplicationContextAssertProvider<ApplicationContext> context = get(this.mockContextSupplier);
		assertThat(context.getStartupFailure()).isNull();
	}

	@Test
	public void getStartupFailureWhenContextFailsToStartShouldReturnException() {
		ApplicationContextAssertProvider<ApplicationContext> context = get(this.startupFailureSupplier);
		assertThat(context.getStartupFailure()).isEqualTo(this.startupFailure);
	}

	@Test
	public void assertThatWhenContextStartsShouldReturnAssertions() {
		ApplicationContextAssertProvider<ApplicationContext> context = get(this.mockContextSupplier);
		ApplicationContextAssert<ApplicationContext> contextAssert = assertThat(context);
		assertThat(contextAssert.getApplicationContext()).isSameAs(context);
		assertThat(contextAssert.getStartupFailure()).isNull();
	}

	@Test
	public void assertThatWhenContextFailsShouldReturnAssertions() {
		ApplicationContextAssertProvider<ApplicationContext> context = get(this.startupFailureSupplier);
		ApplicationContextAssert<ApplicationContext> contextAssert = assertThat(context);
		assertThat(contextAssert.getApplicationContext()).isSameAs(context);
		assertThat(contextAssert.getStartupFailure()).isSameAs(this.startupFailure);
	}

	@Test
	public void toStringWhenContextStartsShouldReturnSimpleString() {
		ApplicationContextAssertProvider<ApplicationContext> context = get(this.mockContextSupplier);
		assertThat(context.toString()).startsWith("Started application [ConfigurableApplicationContext.MockitoMock")
				.endsWith("id = [null], applicationName = [null], beanDefinitionCount = 0]");
	}

	@Test
	public void toStringWhenContextFailsToStartShouldReturnSimpleString() {
		ApplicationContextAssertProvider<ApplicationContext> context = get(this.startupFailureSupplier);
		assertThat(context.toString()).isEqualTo("Unstarted application context "
				+ "org.springframework.context.ApplicationContext" + "[startupFailure=java.lang.RuntimeException]");
	}

	@Test
	public void closeShouldCloseContext() {
		ApplicationContextAssertProvider<ApplicationContext> context = get(this.mockContextSupplier);
		context.close();
		verify(this.mockContext).close();
	}

	private void expectStartupFailure() {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("failed to start");
		this.thrown.expectCause(equalTo(this.startupFailure));
	}

	private ApplicationContextAssertProvider<ApplicationContext> get(Supplier<ApplicationContext> contextSupplier) {
		return ApplicationContextAssertProvider.get(TestAssertProviderApplicationContext.class,
				ApplicationContext.class, contextSupplier);
	}

	private interface TestAssertProviderApplicationContext
			extends ApplicationContextAssertProvider<ApplicationContext> {

	}

	private abstract static class TestAssertProviderApplicationContextClass
			implements TestAssertProviderApplicationContext {

	}

}
