/*
 * Copyright 2012-2022 the original author or authors.
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

package smoketest.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * SampleBatchApplication class.
 */
@SpringBootApplication
public class SampleBatchApplication {

	/**
	 * Returns a Tasklet that will always return RepeatStatus.FINISHED.
	 * @return the Tasklet object
	 */
	@Bean
	Tasklet tasklet() {
		return (contribution, context) -> RepeatStatus.FINISHED;
	}

	/**
	 * Creates a new job with the given job repository and step.
	 * @param jobRepository the job repository to be used for the job
	 * @param step the step to be executed in the job
	 * @return the created job
	 */
	@Bean
	Job job(JobRepository jobRepository, Step step) {
		return new JobBuilder("job", jobRepository).start(step).build();
	}

	/**
	 * Creates a Step object with the given parameters.
	 * @param jobRepository The JobRepository to be used by the Step.
	 * @param tasklet The Tasklet to be executed by the Step.
	 * @param transactionManager The PlatformTransactionManager to be used for transaction
	 * management.
	 * @return The created Step object.
	 */
	@Bean
	Step step1(JobRepository jobRepository, Tasklet tasklet, PlatformTransactionManager transactionManager) {
		return new StepBuilder("step1", jobRepository).tasklet(tasklet, transactionManager).build();
	}

	/**
	 * The main method is the entry point of the SampleBatchApplication class. It is
	 * responsible for starting the Spring application and exiting with the appropriate
	 * exit code.
	 * @param args the command line arguments passed to the application
	 */
	public static void main(String[] args) {
		// System.exit is common for Batch applications since the exit code can be used to
		// drive a workflow
		System.exit(SpringApplication.exit(SpringApplication.run(SampleBatchApplication.class, args)));
	}

}
