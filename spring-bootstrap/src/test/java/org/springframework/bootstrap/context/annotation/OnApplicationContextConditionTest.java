/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.bootstrap.context.annotation;

import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.ObjectUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link OnApplicationContextCondition}.
 * 
 * @author Phillip Webb
 */
@SuppressWarnings("resource")
public class OnApplicationContextConditionTest {

	@Test
	public void forContextById() throws Exception {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.setId("parent");
		parent.register(ForContextByIdConf.class);

		AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();
		child.setId("child");
		child.setParent(parent);
		child.register(ForContextByIdConf.class);

		parent.refresh();
		child.refresh();

		assertThat(parent.containsLocalBean("inParent"), equalTo(true));
		assertThat(parent.containsLocalBean("inChild"), equalTo(false));

		assertThat(child.containsLocalBean("inParent"), equalTo(false));
		assertThat(child.containsLocalBean("inChild"), equalTo(true));
	}

	@Test
	@Ignore
	public void createContext() throws Exception {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		ApplicationContextCollector collector = new ApplicationContextCollector();
		parent.addApplicationListener(collector);
		parent.register(CreateContext.class);
		parent.refresh();
		assertThat(collector.get("child").containsLocalBean("inChild"), equalTo(true));
	}

	// FIXME
	// createContextOnBeanMethod
	// createContextComponent

	private static class ApplicationContextCollector implements
			ApplicationListener<ContextRefreshedEvent> {

		private Set<ApplicationContext> contexts = new HashSet<ApplicationContext>();

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			this.contexts.add(event.getApplicationContext());
		}

		public ApplicationContext get(String id) {
			for (ApplicationContext context : this.contexts) {
				if (ObjectUtils.nullSafeEquals(context.getId(), id)) {
					return context;
				}
			}
			throw new IllegalArgumentException("No such ID " + id);
		}
	}

	@Configuration
	public static class ForContextByIdConf {

		@Bean
		@ConditionalOnApplicationContext("parent")
		public String inParent() {
			return "inParent";
		}

		@Bean
		@ConditionalOnApplicationContext("child")
		public String inChild() {
			return "inChild";
		}
	}

	@Configuration
	@ConditionalOnApplicationContext(value = "child", createIfMissing = true)
	public static class CreateContext {

		@Bean
		public String inChild() {
			return "inChild";
		}
	}

}
