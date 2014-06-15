/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Tests for {@link SpringBootCondition}.
 * 
 * @author Phillip Webb
 */
@SuppressWarnings("resource")
public class SpringBootConditionTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void sensibleClassException() {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Error processing condition on "
				+ ErrorOnClass.class.getName());
		new AnnotationConfigApplicationContext(ErrorOnClass.class);
	}

	@Test
	public void sensibleMethodException() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Error processing condition on "
				+ ErrorOnMethod.class.getName() + ".myBean");
		new AnnotationConfigApplicationContext(ErrorOnMethod.class);
	}

	@Configuration
	@Conditional(AlwaysThrowsCondition.class)
	public static class ErrorOnClass {

	}

	@Configuration
	public static class ErrorOnMethod {

		@Bean
		@Conditional(AlwaysThrowsCondition.class)
		public String myBean() {
			return "bean";
		}

	}

	public static class AlwaysThrowsCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			throw new RuntimeException("Oh no!");
		}

	}

}
