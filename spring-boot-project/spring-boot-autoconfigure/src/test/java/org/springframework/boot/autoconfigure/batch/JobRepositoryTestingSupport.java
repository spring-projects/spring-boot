/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.batch;

import java.util.function.Consumer;

import org.assertj.core.api.InstanceOfAssertFactories;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.test.util.AopTestUtils;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

final class JobRepositoryTestingSupport {

	private JobRepositoryTestingSupport() {

	}

	static Consumer<JobRepository> isolationLevelRequirements(String isolationLevel) {
		return (jobRepository) -> {
			// Target object of jobRepository is itself an AOP proxy with two advisors,
			// the second one is advising transaction attributes
			Object targetProxy = AopTestUtils.getTargetObject(jobRepository);
			assertThat(targetProxy).isInstanceOf(Advised.class);
			Advisor[] advisors = ((Advised) targetProxy).getAdvisors();
			assertThat(advisors.length).isEqualTo(2);
			assertThat(advisors[1].getAdvice()).extracting("transactionAttributeSource")
					.extracting(Object::toString, as(InstanceOfAssertFactories.STRING))
					.contains("create*=PROPAGATION_REQUIRES_NEW," + isolationLevel)
					.contains("getLastJobExecution*=PROPAGATION_REQUIRES_NEW," + isolationLevel);
		};
	}

}
