/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.bind;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.boot.bind.RelaxedDataBinderTests.TargetWithNestedObject;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 */
public class BindingPreparationTests {

	@Test
	public void testBeanWrapperCreatesNewMaps() throws Exception {
		TargetWithNestedMap target = new TargetWithNestedMap();
		BeanWrapperImpl wrapper = new BeanWrapperImpl(target);
		wrapper.setAutoGrowNestedPaths(true);
		// For a nested map, you only have to get an element of it for it to be created
		wrapper.getPropertyValue("nested[foo]");
		assertThat(wrapper.getPropertyValue("nested")).isNotNull();
	}

	@Test
	public void testBeanWrapperCreatesNewMapEntries() throws Exception {
		TargetWithNestedMapOfBean target = new TargetWithNestedMapOfBean();
		BeanWrapperImpl wrapper = new BeanWrapperImpl(target);
		wrapper.setAutoGrowNestedPaths(true);
		// For a nested map, you only have to get an element of it for it to be created
		wrapper.getPropertyValue("nested[foo]");
		wrapper.setPropertyValue("nested[foo].foo", "bar");
		assertThat(wrapper.getPropertyValue("nested")).isNotNull();
		assertThat(wrapper.getPropertyValue("nested[foo]")).isNotNull();
	}

	@Test
	public void testListOfBeansWithList() throws Exception {
		TargetWithNestedListOfBeansWithList target = new TargetWithNestedListOfBeansWithList();
		BeanWrapperImpl wrapper = new BeanWrapperImpl(target);
		wrapper.setAutoGrowNestedPaths(true);
		RelaxedDataBinder binder = new RelaxedDataBinder(target);
		binder.normalizePath(wrapper, "nested[0].list[1]");
		assertThat(wrapper.getPropertyValue("nested")).isNotNull();
		assertThat(wrapper.getPropertyValue("nested[0].list[1]")).isNotNull();
	}

	@Test
	public void testListOfBeansWithListAndNoPeriod() throws Exception {
		TargetWithNestedListOfBeansWithList target = new TargetWithNestedListOfBeansWithList();
		BeanWrapperImpl wrapper = new BeanWrapperImpl(target);
		wrapper.setAutoGrowNestedPaths(true);
		RelaxedDataBinder binder = new RelaxedDataBinder(target);
		binder.normalizePath(wrapper, "nested[0]list[1]");
		assertThat(wrapper.getPropertyValue("nested")).isNotNull();
		assertThat(wrapper.getPropertyValue("nested[0].list[1]")).isNotNull();
	}

	@Test
	public void testAutoGrowWithFuzzyNameCapitals() throws Exception {
		TargetWithNestedMap target = new TargetWithNestedMap();
		BeanWrapperImpl wrapper = new BeanWrapperImpl(target);
		wrapper.setAutoGrowNestedPaths(true);
		RelaxedDataBinder binder = new RelaxedDataBinder(target);
		String result = binder.normalizePath(wrapper, "NESTED[foo][bar]");
		assertThat(wrapper.getPropertyValue("nested")).isNotNull();
		assertThat("nested[foo][bar]").isEqualTo(result);
		assertThat(wrapper.getPropertyValue("nested[foo][bar]")).isNotNull();
	}

	@Test
	public void testAutoGrowWithFuzzyNameUnderscores() throws Exception {
		TargetWithNestedMap target = new TargetWithNestedMap();
		BeanWrapperImpl wrapper = new BeanWrapperImpl(target);
		wrapper.setAutoGrowNestedPaths(true);
		RelaxedDataBinder binder = new RelaxedDataBinder(target);
		String result = binder.normalizePath(wrapper, "nes_ted[foo][bar]");
		assertThat(wrapper.getPropertyValue("nested")).isNotNull();
		assertThat("nested[foo][bar]").isEqualTo(result);
		assertThat(wrapper.getPropertyValue("nested[foo][bar]")).isNotNull();
	}

	@Test
	public void testAutoGrowNewNestedMapOfMaps() throws Exception {
		TargetWithNestedMap target = new TargetWithNestedMap();
		BeanWrapperImpl wrapper = new BeanWrapperImpl(target);
		wrapper.setAutoGrowNestedPaths(true);
		RelaxedDataBinder binder = new RelaxedDataBinder(target);
		String result = binder.normalizePath(wrapper, "nested[foo][bar]");
		assertThat(wrapper.getPropertyValue("nested")).isNotNull();
		assertThat("nested[foo][bar]").isEqualTo(result);
		assertThat(wrapper.getPropertyValue("nested[foo][bar]")).isNotNull();
	}

	@Test
	public void testAutoGrowNewNestedMapOfBeans() throws Exception {
		TargetWithNestedMapOfBean target = new TargetWithNestedMapOfBean();
		BeanWrapperImpl wrapper = new BeanWrapperImpl(target);
		wrapper.setAutoGrowNestedPaths(true);
		RelaxedDataBinder binder = new RelaxedDataBinder(target);
		String result = binder.normalizePath(wrapper, "nested[foo].foo");
		assertThat(wrapper.getPropertyValue("nested")).isNotNull();
		assertThat("nested[foo].foo").isEqualTo(result);
		assertThat(wrapper.getPropertyValue("nested[foo]")).isNotNull();
	}

	@Test
	public void testAutoGrowNewNestedMapOfBeansWithPeriod() throws Exception {
		TargetWithNestedMapOfBean target = new TargetWithNestedMapOfBean();
		BeanWrapperImpl wrapper = new BeanWrapperImpl(target);
		wrapper.setAutoGrowNestedPaths(true);
		RelaxedDataBinder binder = new RelaxedDataBinder(target);
		String result = binder.normalizePath(wrapper, "nested.foo.foo");
		assertThat(wrapper.getPropertyValue("nested")).isNotNull();
		assertThat("nested[foo].foo").isEqualTo(result);
	}

	@Test
	public void testAutoGrowNewNestedMapOfListOfString() throws Exception {
		TargetWithNestedMapOfListOfString target = new TargetWithNestedMapOfListOfString();
		BeanWrapperImpl wrapper = new BeanWrapperImpl(target);
		wrapper.setAutoGrowNestedPaths(true);
		RelaxedDataBinder binder = new RelaxedDataBinder(target);
		binder.normalizePath(wrapper, "nested[foo][0]");
		assertThat(wrapper.getPropertyValue("nested")).isNotNull();
		assertThat(wrapper.getPropertyValue("nested[foo]")).isNotNull();
	}

	@Test
	public void testAutoGrowListOfMaps() throws Exception {
		TargetWithNestedListOfMaps target = new TargetWithNestedListOfMaps();
		BeanWrapperImpl wrapper = new BeanWrapperImpl(target);
		wrapper.setAutoGrowNestedPaths(true);
		RelaxedDataBinder binder = new RelaxedDataBinder(target);
		binder.normalizePath(wrapper, "nested[0][foo]");
		assertThat(wrapper.getPropertyValue("nested")).isNotNull();
		assertThat(wrapper.getPropertyValue("nested[0]")).isNotNull();
	}

	@Test
	public void testAutoGrowListOfLists() throws Exception {
		TargetWithNestedListOfLists target = new TargetWithNestedListOfLists();
		BeanWrapperImpl wrapper = new BeanWrapperImpl(target);
		wrapper.setAutoGrowNestedPaths(true);
		RelaxedDataBinder binder = new RelaxedDataBinder(target);
		binder.normalizePath(wrapper, "nested[0][1]");
		assertThat(wrapper.getPropertyValue("nested")).isNotNull();
		assertThat(wrapper.getPropertyValue("nested[0][1]")).isNotNull();
	}

	@Test
	public void testBeanWrapperCreatesNewNestedMaps() throws Exception {
		TargetWithNestedMap target = new TargetWithNestedMap();
		BeanWrapperImpl wrapper = new BeanWrapperImpl(target);
		wrapper.setAutoGrowNestedPaths(true);
		// For a nested map, you only have to get an element of it for it to be created
		wrapper.getPropertyValue("nested[foo]");
		// To decide what type to create for nested[foo] we need to look ahead and see
		// what the user is trying to bind it to, e.g. if nested[foo][bar] then it's a map
		wrapper.setPropertyValue("nested[foo]", new LinkedHashMap<String, Object>());
		// But it might equally well be a collection, if nested[foo][0]
		wrapper.setPropertyValue("nested[foo]", new ArrayList<Object>());
		// Then it would have to be actually bound to get the list to auto-grow
		wrapper.setPropertyValue("nested[foo][0]", "bar");
		assertThat(wrapper.getPropertyValue("nested[foo][0]")).isNotNull();
	}

	@Test
	public void testBeanWrapperCreatesNewObjects() throws Exception {
		TargetWithNestedObject target = new TargetWithNestedObject();
		BeanWrapperImpl wrapper = new BeanWrapperImpl(target);
		wrapper.setAutoGrowNestedPaths(true);
		// For a nested object, you have to set a property for it to be created
		wrapper.setPropertyValue("nested.foo", "bar");
		wrapper.getPropertyValue("nested");
		assertThat(wrapper.getPropertyValue("nested")).isNotNull();
	}

	@Test
	public void testBeanWrapperLists() throws Exception {
		TargetWithNestedMapOfListOfString target = new TargetWithNestedMapOfListOfString();
		BeanWrapperImpl wrapper = new BeanWrapperImpl(target);
		wrapper.setAutoGrowNestedPaths(true);
		TypeDescriptor descriptor = wrapper.getPropertyTypeDescriptor("nested");
		assertThat(descriptor.isMap()).isTrue();
		wrapper.getPropertyValue("nested[foo]");
		assertThat(wrapper.getPropertyValue("nested")).isNotNull();
		// You also need to bind to a value here
		wrapper.setPropertyValue("nested[foo][0]", "bar");
		wrapper.getPropertyValue("nested[foo][0]");
		assertThat(wrapper.getPropertyValue("nested[foo]")).isNotNull();
	}

	@Test
	@Ignore("Work in progress")
	public void testExpressionLists() throws Exception {
		TargetWithNestedMapOfListOfString target = new TargetWithNestedMapOfListOfString();
		LinkedHashMap<String, List<String>> map = new LinkedHashMap<String, List<String>>();
		// map.put("foo", Arrays.asList("bar"));
		target.setNested(map);
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext(target);
		context.addPropertyAccessor(new MapAccessor());
		Expression expression = parser.parseExpression("nested.foo");
		assertThat(expression.getValue(context)).isNotNull();
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

	public static class TargetWithNestedMapOfListOfString {

		private Map<String, List<String>> nested;

		public Map<String, List<String>> getNested() {
			return this.nested;
		}

		public void setNested(Map<String, List<String>> nested) {
			this.nested = nested;
		}

	}

	public static class TargetWithNestedListOfMaps {

		private List<Map<String, String>> nested;

		public List<Map<String, String>> getNested() {
			return this.nested;
		}

		public void setNested(List<Map<String, String>> nested) {
			this.nested = nested;
		}

	}

	public static class TargetWithNestedListOfLists {

		private List<List<String>> nested;

		public List<List<String>> getNested() {
			return this.nested;
		}

		public void setNested(List<List<String>> nested) {
			this.nested = nested;
		}

	}

	public static class TargetWithNestedListOfBeansWithList {

		private List<TargetWithList> nested;

		public List<TargetWithList> getNested() {
			return this.nested;
		}

		public void setNested(List<TargetWithList> nested) {
			this.nested = nested;
		}

	}

	public static class TargetWithList {

		private List<VanillaTarget> list;

		public List<VanillaTarget> getList() {
			return this.list;
		}

		public void setList(List<VanillaTarget> list) {
			this.list = list;
		}

	}

	public static class TargetWithNestedMapOfBean {

		private Map<String, VanillaTarget> nested;

		public Map<String, VanillaTarget> getNested() {
			return this.nested;
		}

		public void setNested(Map<String, VanillaTarget> nested) {
			this.nested = nested;
		}

	}

	public static class VanillaTarget {

		private String foo;

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

	}

}
