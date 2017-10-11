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

package org.springframework.boot.context.properties.bind;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.Validation;

import org.assertj.core.matcher.AssertionMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.matchers.ThrowableMessageMatcher;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.InOrder;

import org.springframework.boot.context.properties.bind.validation.ValidationBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.MockConfigurationPropertySource;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link Binder}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class BinderTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private List<ConfigurationPropertySource> sources = new ArrayList<>();

	private Binder binder;

	@Before
	public void setup() {
		this.binder = new Binder(this.sources);
	}

	@Test
	public void createWhenSourcesIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Sources must not be null");
		new Binder((Iterable<ConfigurationPropertySource>) null);
	}

	@Test
	public void bindWhenNameIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name must not be null");
		this.binder.bind((ConfigurationPropertyName) null, Bindable.of(String.class),
				BindHandler.DEFAULT);
	}

	@Test
	public void bindWhenTargetIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Target must not be null");
		this.binder.bind(ConfigurationPropertyName.of("foo"), null, BindHandler.DEFAULT);
	}

	@Test
	public void bindToValueWhenPropertyIsMissingShouldReturnUnbound() throws Exception {
		this.sources.add(new MockConfigurationPropertySource());
		BindResult<String> result = this.binder.bind("foo", Bindable.of(String.class));
		assertThat(result.isBound()).isFalse();
	}

	@Test
	public void bindToValueShouldReturnPropertyValue() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo", 123));
		BindResult<Integer> result = this.binder.bind("foo", Bindable.of(Integer.class));
		assertThat(result.get()).isEqualTo(123);
	}

	@Test
	public void bindToValueShouldReturnPropertyValueFromSecondSource() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo", 123));
		this.sources.add(new MockConfigurationPropertySource("bar", 234));
		BindResult<Integer> result = this.binder.bind("bar", Bindable.of(Integer.class));
		assertThat(result.get()).isEqualTo(234);
	}

	@Test
	public void bindToValueShouldReturnConvertedPropertyValue() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo", "123"));
		BindResult<Integer> result = this.binder.bind("foo", Bindable.of(Integer.class));
		assertThat(result.get()).isEqualTo(123);
	}

	@Test
	public void bindToValueWhenMultipleCandidatesShouldReturnFirst() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo", 123));
		this.sources.add(new MockConfigurationPropertySource("foo", 234));
		BindResult<Integer> result = this.binder.bind("foo", Bindable.of(Integer.class));
		assertThat(result.get()).isEqualTo(123);
	}

	@Test
	public void bindToValueWithPlaceholdersShouldResolve() throws Exception {
		StandardEnvironment environment = new StandardEnvironment();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(environment, "bar=23");
		this.sources.add(new MockConfigurationPropertySource("foo", "1${bar}"));
		this.binder = new Binder(this.sources,
				new PropertySourcesPlaceholdersResolver(environment));
		BindResult<Integer> result = this.binder.bind("foo", Bindable.of(Integer.class));
		assertThat(result.get()).isEqualTo(123);
	}

	@Test
	public void bindToValueWithMissingPlaceholdersShouldThrowException()
			throws Exception {
		StandardEnvironment environment = new StandardEnvironment();
		this.sources.add(new MockConfigurationPropertySource("foo", "${bar}"));
		this.binder = new Binder(this.sources,
				new PropertySourcesPlaceholdersResolver(environment));
		this.thrown.expect(BindException.class);
		this.thrown.expectCause(ThrowableMessageMatcher.hasMessage(containsString(
				"Could not resolve placeholder 'bar' in value \"${bar}\"")));
		this.binder.bind("foo", Bindable.of(Integer.class));
	}

	@Test
	public void bindToValueShouldTriggerOnSuccess() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo", "1", "line1"));
		BindHandler handler = mock(BindHandler.class,
				withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
		Bindable<Integer> target = Bindable.of(Integer.class);
		this.binder.bind("foo", target, handler);
		InOrder ordered = inOrder(handler);
		ordered.verify(handler).onSuccess(eq(ConfigurationPropertyName.of("foo")),
				eq(target), any(), eq(1));
	}

	@Test
	public void bindToJavaBeanShouldReturnPopulatedBean() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo.value", "bar"));
		JavaBean result = this.binder.bind("foo", Bindable.of(JavaBean.class)).get();
		assertThat(result.getValue()).isEqualTo("bar");
	}

	@Test
	public void bindToJavaBeanWhenNonIterableShouldReturnPopulatedBean()
			throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource(
				"foo.value", "bar");
		this.sources.add(source.nonIterable());
		JavaBean result = this.binder.bind("foo", Bindable.of(JavaBean.class)).get();
		assertThat(result.getValue()).isEqualTo("bar");
	}

	@Test
	public void bindToJavaBeanShouldTriggerOnSuccess() throws Exception {
		this.sources
				.add(new MockConfigurationPropertySource("foo.value", "bar", "line1"));
		BindHandler handler = mock(BindHandler.class,
				withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
		Bindable<JavaBean> target = Bindable.of(JavaBean.class);
		this.binder.bind("foo", target, handler);
		InOrder inOrder = inOrder(handler);
		inOrder.verify(handler).onSuccess(eq(ConfigurationPropertyName.of("foo.value")),
				eq(Bindable.of(String.class)), any(), eq("bar"));
		inOrder.verify(handler).onSuccess(eq(ConfigurationPropertyName.of("foo")),
				eq(target), any(), isA(JavaBean.class));
	}

	@Test
	public void bindWhenHasMalformedDateShouldThrowException() throws Exception {
		this.thrown.expectCause(instanceOf(ConversionFailedException.class));
		this.sources.add(new MockConfigurationPropertySource("foo",
				"2014-04-01T01:30:00.000-05:00"));
		this.binder.bind("foo", Bindable.of(LocalDate.class));
	}

	@Test
	public void bindWhenHasAnnotationsShouldChangeConvertedValue() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo",
				"2014-04-01T01:30:00.000-05:00"));
		DateTimeFormat annotation = AnnotationUtils.synthesizeAnnotation(
				Collections.singletonMap("iso", DateTimeFormat.ISO.DATE_TIME),
				DateTimeFormat.class, null);
		LocalDate result = this.binder
				.bind("foo", Bindable.of(LocalDate.class).withAnnotations(annotation))
				.get();
		assertThat(result.toString()).isEqualTo("2014-04-01");
	}

	@Test
	public void bindExceptionWhenBeanBindingFailsShouldHaveNullConfigurationProperty()
			throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value", "hello");
		source.put("foo.items", "bar,baz");
		this.sources.add(source);
		Bindable<JavaBean> target = Bindable.of(JavaBean.class);
		this.thrown.expect(BindException.class);
		this.thrown.expect(new AssertionMatcher<BindException>() {

			@Override
			public void assertion(BindException ex) throws AssertionError {
				assertThat(ex.getCause().getMessage())
						.isEqualTo("No setter found for property: items");
				assertThat(ex.getProperty()).isNull();
			}

		});
		this.binder.bind("foo", target);
	}

	@Test
	public void bindToValidatedBeanWithResourceAndNonEnumerablePropertySource() {
		ConfigurationPropertySources.from(new PropertySource<String>("test") {

			@Override
			public Object getProperty(String name) {
				return null;
			}

		}).forEach(this.sources::add);
		Validator validator = new SpringValidatorAdapter(Validation.byDefaultProvider()
				.configure().buildValidatorFactory().getValidator());
		this.binder.bind("foo", Bindable.of(ResourceBean.class),
				new ValidationBindHandler(validator));
	}

	@Test
	public void bindToBeanWithCycle() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		this.sources.add(source.nonIterable());
		Bindable<CycleBean1> target = Bindable.of(CycleBean1.class);
		this.binder.bind("foo", target);
	}

	public static class JavaBean {

		private String value;

		private List<String> items = Collections.emptyList();

		public String getValue() {
			return this.value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public List<String> getItems() {
			return this.items;
		}

	}

	public enum ExampleEnum {

		FOO_BAR, BAR_BAZ, BAZ_BOO

	}

	@Validated
	public static class ResourceBean {

		private Resource resource;

		public Resource getResource() {
			return this.resource;
		}

		public void setResource(Resource resource) {
			this.resource = resource;
		}

	}

	public static class CycleBean1 {

		private CycleBean2 two;

		public CycleBean2 getTwo() {
			return this.two;
		}

		public void setTwo(CycleBean2 two) {
			this.two = two;
		}

	}

	public static class CycleBean2 {

		private CycleBean1 one;

		public CycleBean1 getOne() {
			return this.one;
		}

		public void setOne(CycleBean1 one) {
			this.one = one;
		}

	}

}
