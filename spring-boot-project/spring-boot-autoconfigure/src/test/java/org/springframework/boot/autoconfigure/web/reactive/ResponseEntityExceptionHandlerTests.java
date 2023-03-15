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

package org.springframework.boot.autoconfigure.web.reactive;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.springframework.boot.autoconfigure.SpringArchUnitTests;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

import static com.tngtech.archunit.base.DescribedPredicate.greaterThanOrEqualTo;

/**
 * Tests for {@link ProblemDetailsExceptionHandler} using {@link SpringBootTest}.
 *
 * @author Volkan Yazıcı
 */
@AnalyzeClasses(packages = "org.springframework.boot.autoconfigure.web.reactive",
		importOptions = SpringArchUnitTests.ExcludeTestsOption.class)
class ResponseEntityExceptionHandlerTests {

	@ArchTest
	void exceptionHandlerShouldNotBeFinal(JavaClasses classes) { // gh-34426
		int minMatchingClassCount = List.of(ProblemDetailsExceptionHandler.class).size();
		ArchRuleDefinition.classes()
			.that()
			.areAssignableTo(ResponseEntityExceptionHandler.class)
			.should()
			// Ensure we are not working against an empty set:
			.containNumberOfElements(greaterThanOrEqualTo(minMatchingClassCount))
			// Avoid `final` modifiers:
			.andShould()
			.notHaveModifier(JavaModifier.FINAL)
			.check(classes);
	}

}
