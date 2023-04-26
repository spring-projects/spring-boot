/*
 * Copyright 2023 the original author or authors.
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

package org.springframework.boot.build.architecture;

import java.util.stream.Stream;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass.Predicates;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

/**
 * Architecture rules evaluated by {@link ArchitectureCheck}.
 *
 * @author Andy Wilkinson
 */
final class Rules {

	private Rules() {

	}

	static Stream<ArchRule> stream() {
		return Stream.of(allPackagesShouldBeFreeOfTangles(),
				allBeanPostProcessorBeanMethodsShouldBeStaticAndHaveParametersThatWillNotCausePrematureInitialization());
	}

	static ArchRule allPackagesShouldBeFreeOfTangles() {
		return SlicesRuleDefinition.slices().matching("(**)").should().beFreeOfCycles();
	}

	static ArchRule allBeanPostProcessorBeanMethodsShouldBeStaticAndHaveParametersThatWillNotCausePrematureInitialization() {
		return ArchRuleDefinition.methods()
			.that()
			.areAnnotatedWith("org.springframework.context.annotation.Bean")
			.and()
			.haveRawReturnType(Predicates.assignableTo("org.springframework.beans.factory.config.BeanPostProcessor"))
			.should()
			.beStatic()
			.andShould()
			.haveRawParameterTypes(DescribedPredicate
				.allElements(Predicates.assignableTo("org.springframework.beans.factory.ObjectProvider")
					.or(Predicates.assignableTo("org.springframework.context.ApplicationContext"))))
			.allowEmptyShould(true);
	}

}
