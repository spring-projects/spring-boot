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

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.aopalliance.aop.Advice;
import org.assertj.core.api.InstanceOfAssertFactories;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

final class JobRepositoryTestingSupport {

	private JobRepositoryTestingSupport() {

	}

	static Consumer<JobRepository> isolationLevelRequirements(String isolationLevel) {
		return (jobRepository) ->
				// jobRepository is proxied twice, the inner proxy has the transaction advice.
				// This logic does not assume anything about proxy hierarchy, but it does about
				// the advice itself.
				assertThat(getTransactionAdvices(jobRepository))
						.anySatisfy((advice) -> assertThat(advice).extracting("transactionAttributeSource")
								.extracting(Object::toString, as(InstanceOfAssertFactories.STRING))
								.contains("create*=PROPAGATION_REQUIRES_NEW," + isolationLevel)
								.contains("getLastJobExecution*=PROPAGATION_REQUIRES_NEW," + isolationLevel));
	}

	private static Stream<Advice> getTransactionAdvices(Object candidate) {
		Builder<Advice> builder = Stream.builder();
		getTransactionAdvices(candidate, builder);
		return builder.build();
	}

	private static void getTransactionAdvices(Object candidate, Builder<Advice> builder) {
		try {
			if (AopUtils.isAopProxy(candidate) && candidate instanceof Advised) {
				Arrays.stream(((Advised) candidate).getAdvisors())
						.map(Advisor::getAdvice)
						.filter(TransactionAspectSupport.class::isInstance)
						.forEach(builder::add);
				Object target = ((Advised) candidate).getTargetSource().getTarget();
				if (target != null) {
					getTransactionAdvices(target, builder);
				}
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to unwrap proxied object", ex);
		}
	}

}
