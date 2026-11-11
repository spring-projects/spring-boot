/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SampleBatchApplication {

	@Bean
	Job job(JobRepository jobRepository) {
		return new JobBuilder("job", jobRepository)
			.start(new StepBuilder("step1", jobRepository)
				.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED)
				.build())
			.build();
	}

	public static void main(String[] args) {
		// System.exit is common for Batch applications since the exit code can be used to
		// drive a workflow
		System.exit(SpringApplication.exit(SpringApplication.run(SampleBatchApplication.class, args)));
	}

}
