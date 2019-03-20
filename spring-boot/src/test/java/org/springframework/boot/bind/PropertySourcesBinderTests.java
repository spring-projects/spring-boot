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

import java.util.Map;

import org.junit.Test;

import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertySourcesBinder}.
 *
 * @author Stephane Nicoll
 */
public class PropertySourcesBinderTests {

	private StandardEnvironment env = new StandardEnvironment();

	@Test
	public void extractAllWithPrefix() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.env, "foo.first=1",
				"foo.second=2");
		Map<String, Object> content = new PropertySourcesBinder(this.env)
				.extractAll("foo");
		assertThat(content.get("first")).isEqualTo("1");
		assertThat(content.get("second")).isEqualTo("2");
		assertThat(content).hasSize(2);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void extractNoPrefix() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.env,
				"foo.ctx.first=1", "foo.ctx.second=2");
		Map<String, Object> content = new PropertySourcesBinder(this.env).extractAll("");
		assertThat(content.get("foo")).isInstanceOf(Map.class);
		Map<String, Object> foo = (Map<String, Object>) content.get("foo");
		assertThat(content.get("foo")).isInstanceOf(Map.class);
		Map<String, Object> ctx = (Map<String, Object>) foo.get("ctx");
		assertThat(ctx.get("first")).isEqualTo("1");
		assertThat(ctx.get("second")).isEqualTo("2");
		assertThat(ctx).hasSize(2);
		assertThat(foo).hasSize(1);
	}

	@Test
	public void bindToSimplePojo() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.env,
				"test.name=foo", "test.counter=42");
		TestBean bean = new TestBean();
		new PropertySourcesBinder(this.env).bindTo("test", bean);
		assertThat(bean.getName()).isEqualTo("foo");
		assertThat(bean.getCounter()).isEqualTo(42);
	}

	@SuppressWarnings("unused")
	private static class TestBean {

		private String name;

		private Integer counter;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Integer getCounter() {
			return this.counter;
		}

		public void setCounter(Integer counter) {
			this.counter = counter;
		}

	}

}
