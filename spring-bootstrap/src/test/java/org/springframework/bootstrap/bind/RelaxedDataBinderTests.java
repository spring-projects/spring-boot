/*
 * Cloud Foundry 2012.02.03 Beta
 * Copyright (c) [2009-2012] VMware, Inc. All Rights Reserved.
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product includes a number of subcomponents with
 * separate copyright notices and license terms. Your use of these
 * subcomponents is subject to the terms and conditions of the
 * subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.bootstrap.bind;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.validation.constraints.NotNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.FieldError;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Tests for {@link RelaxedDataBinder}.
 * 
 * @author Dave Syer
 */
public class RelaxedDataBinderTests {

	@Rule
	public ExpectedException expected = ExpectedException.none();

	private ConversionService conversionService;

	@Test
	public void testBindString() throws Exception {
		VanillaTarget target = new VanillaTarget();
		bind(target, "foo: bar");
		assertEquals("bar", target.getFoo());
	}

	@Test
	public void testBindUnderscore() throws Exception {
		VanillaTarget target = new VanillaTarget();
		bind(target, "foo-bar: bar");
		assertEquals("bar", target.getFoo_bar());
	}

	@Test
	public void testBindCamelCase() throws Exception {
		VanillaTarget target = new VanillaTarget();
		bind(target, "foo-baz: bar");
		assertEquals("bar", target.getFooBaz());
	}

	@Test
	public void testBindNumber() throws Exception {
		VanillaTarget target = new VanillaTarget();
		bind(target, "foo: bar\n" + "value: 123");
		assertEquals(123, target.getValue());
	}

	@Test
	public void testSimpleValidation() throws Exception {
		ValidatedTarget target = new ValidatedTarget();
		BindingResult result = bind(target, "");
		assertEquals(1, result.getErrorCount());
	}

	@Test
	public void testRequiredFieldsValidation() throws Exception {
		TargetWithValidatedMap target = new TargetWithValidatedMap();
		BindingResult result = bind(target, "info[foo]: bar");
		assertEquals(2, result.getErrorCount());
		for (FieldError error : result.getFieldErrors()) {
			System.err.println(new StaticMessageSource().getMessage(error,
					Locale.getDefault()));
		}
	}

	@Test
	public void testBindNested() throws Exception {
		TargetWithNestedObject target = new TargetWithNestedObject();
		bind(target, "nested.foo: bar\n" + "nested.value: 123");
		assertEquals(123, target.getNested().getValue());
	}

	@Test
	public void testBindNestedList() throws Exception {
		TargetWithNestedList target = new TargetWithNestedList();
		bind(target, "nested: bar,foo");
		bind(target, "nested[0]: bar");
		bind(target, "nested[1]: foo");
		assertEquals("[bar, foo]", target.getNested().toString());
	}

	@Test
	public void testBindNestedListCommaDelimitedONly() throws Exception {
		TargetWithNestedList target = new TargetWithNestedList();
		this.conversionService = new DefaultConversionService();
		bind(target, "nested: bar,foo");
		assertEquals("[bar, foo]", target.getNested().toString());
	}

	@Test
	public void testBindNestedMap() throws Exception {
		TargetWithNestedMap target = new TargetWithNestedMap();
		bind(target, "nested.foo: bar\n" + "nested.value: 123");
		assertEquals("123", target.getNested().get("value"));
	}

	@Test
	public void testBindNestedMapBracketReferenced() throws Exception {
		TargetWithNestedMap target = new TargetWithNestedMap();
		bind(target, "nested[foo]: bar\n" + "nested[value]: 123");
		assertEquals("123", target.getNested().get("value"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testBindDoubleNestedMap() throws Exception {
		TargetWithNestedMap target = new TargetWithNestedMap();
		bind(target, "nested.foo: bar\n" + "nested.bar.spam: bucket\n"
				+ "nested.bar.value: 123\nnested.bar.foo: crap");
		assertEquals(2, target.getNested().size());
		assertEquals(3, ((Map<String, Object>) target.getNested().get("bar")).size());
		assertEquals("123",
				((Map<String, Object>) target.getNested().get("bar")).get("value"));
		assertEquals("bar", target.getNested().get("foo"));
		assertFalse(target.getNested().containsValue(target.getNested()));
	}

	@Test
	public void testBindErrorTypeMismatch() throws Exception {
		VanillaTarget target = new VanillaTarget();
		BindingResult result = bind(target, "foo: bar\n" + "value: foo");
		assertEquals(1, result.getErrorCount());
	}

	@Test
	public void testBindErrorNotWritable() throws Exception {
		this.expected.expectMessage("property 'spam'");
		this.expected.expectMessage("not writable");
		VanillaTarget target = new VanillaTarget();
		BindingResult result = bind(target, "spam: bar\n" + "value: 123");
		assertEquals(1, result.getErrorCount());
	}

	@Test
	public void testBindErrorNotWritableWithPrefix() throws Exception {
		VanillaTarget target = new VanillaTarget();
		BindingResult result = bind(target, "spam: bar\n" + "vanilla.value: 123",
				"vanilla");
		assertEquals(0, result.getErrorCount());
		assertEquals(123, target.getValue());
	}

	@Test
	public void testBindMap() throws Exception {
		Map<String, Object> target = new LinkedHashMap<String, Object>();
		BindingResult result = bind(target, "spam: bar\n" + "vanilla.value: 123",
				"vanilla");
		assertEquals(0, result.getErrorCount());
		assertEquals("123", target.get("value"));
	}

	@Test
	public void testBindMapNestedInMap() throws Exception {
		Map<String, Object> target = new LinkedHashMap<String, Object>();
		BindingResult result = bind(target, "spam: bar\n" + "vanilla.foo.value: 123",
				"vanilla");
		assertEquals(0, result.getErrorCount());
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) target.get("foo");
		assertEquals("123", map.get("value"));
	}

	private BindingResult bind(Object target, String values) throws Exception {
		return bind(target, values, null);
	}

	private BindingResult bind(Object target, String values, String namePrefix)
			throws Exception {
		Properties properties = PropertiesLoaderUtils
				.loadProperties(new ByteArrayResource(values.getBytes()));
		DataBinder binder = new RelaxedDataBinder(target, namePrefix);
		binder.setIgnoreUnknownFields(false);
		LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();
		validatorFactoryBean.afterPropertiesSet();
		binder.setValidator(validatorFactoryBean);
		binder.setConversionService(this.conversionService);
		binder.bind(new MutablePropertyValues(properties));
		binder.validate();

		return binder.getBindingResult();
	}

	@Documented
	@Target({ ElementType.FIELD })
	@Retention(RUNTIME)
	@Constraint(validatedBy = RequiredKeysValidator.class)
	public @interface RequiredKeys {

		String[] value();

		String message() default "Required fields are not provided for field ''{0}''";

		Class<?>[] groups() default {};

		Class<? extends Payload>[] payload() default {};

	}

	public static class RequiredKeysValidator implements
			ConstraintValidator<RequiredKeys, Map<String, Object>> {

		private String[] fields;

		@Override
		public void initialize(RequiredKeys constraintAnnotation) {
			this.fields = constraintAnnotation.value();
		}

		@Override
		public boolean isValid(Map<String, Object> value,
				ConstraintValidatorContext context) {
			boolean valid = true;
			for (String field : this.fields) {
				if (!value.containsKey(field)) {
					context.buildConstraintViolationWithTemplate(
							"Missing field ''" + field + "''").addConstraintViolation();
					valid = false;
				}
			}
			return valid;
		}

	}

	public static class TargetWithValidatedMap {

		@RequiredKeys({ "foo", "value" })
		private Map<String, Object> info;

		public Map<String, Object> getInfo() {
			return this.info;
		}

		public void setInfo(Map<String, Object> nested) {
			this.info = nested;
		}
	}

	public static class TargetWithNestedMap {
		private Map<String, Object> nested;

		public Map<String, Object> getNested() {
			return this.nested;
		}

		public void setNested(Map<String, Object> nested) {
			this.nested = nested;
		}
	}

	public static class TargetWithNestedList {
		private List<String> nested;

		public List<String> getNested() {
			return this.nested;
		}

		public void setNested(List<String> nested) {
			this.nested = nested;
		}
	}

	public static class TargetWithNestedObject {
		private VanillaTarget nested;

		public VanillaTarget getNested() {
			return this.nested;
		}

		public void setNested(VanillaTarget nested) {
			this.nested = nested;
		}
	}

	public static class VanillaTarget {

		private String foo;

		private int value;

		private String foo_bar;

		private String fooBaz;

		public int getValue() {
			return this.value;
		}

		public void setValue(int value) {
			this.value = value;
		}

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		public String getFoo_bar() {
			return this.foo_bar;
		}

		public void setFoo_bar(String foo_bar) {
			this.foo_bar = foo_bar;
		}

		public String getFooBaz() {
			return this.fooBaz;
		}

		public void setFooBaz(String fooBaz) {
			this.fooBaz = fooBaz;
		}
	}

	public static class ValidatedTarget {

		@NotNull
		private String foo;

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

	}
}
