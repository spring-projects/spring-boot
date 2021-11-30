package org.springframework.boot.autoconfigure.batch;

import java.util.function.Consumer;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.test.util.AopTestUtils;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

class JobRepositoryTestingSupport {

	static Consumer<JobRepository> isolationLevelRequirements(String isolationLevel) {
		return jobRepository -> {
			assertThat((Object) AopTestUtils.getTargetObject(jobRepository)).isInstanceOf(Advised.class);
			Advised target = AopTestUtils.getTargetObject(jobRepository);
			Advisor[] advisors = target.getAdvisors();
			assertThat(advisors.length).isEqualTo(2);
			assertThat(advisors[1].getAdvice())
					.extracting("transactionAttributeSource")
					.extracting(Object::toString, as(STRING))
					.contains("create*=PROPAGATION_REQUIRES_NEW," + isolationLevel)
					.contains("getLastJobExecution*=PROPAGATION_REQUIRES_NEW," + isolationLevel);
		};
	}

}
