/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.batch.mongodb.autoconfigure;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoOperations;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link BatchDataMongoAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
@Testcontainers(disabledWithoutDocker = true)
class BatchDataMongoAutoConfigurationIntegrationTests {

	@Container
	static final MongoDBContainer mongoDb = TestImage.forContainer(MongoDBContainer.class).withReplicaSet();

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(JobConfiguration.class)
		.withPropertyValues("spring.batch.data.mongodb.schema.initialize=true",
				"spring.mongodb.uri=" + mongoDb.getReplicaSetUrl())
		.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class, DataMongoAutoConfiguration.class,
				BatchDataMongoAutoConfiguration.class));

	@Test
	void runJob() {
		this.contextRunner.withUserConfiguration(JobConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(JobOperator.class)
				.hasSingleBean(JobRepository.class)
				.hasSingleBean(Job.class);

			JobParameters jobParameters = new JobParametersBuilder().addString("name", "foo")
				.addLocalDateTime("runtime", LocalDateTime.now())
				.toJobParameters();
			JobExecution jobExecution = context.getBean(JobOperator.class)
				.start(context.getBean(Job.class), jobParameters);

			assertThat(jobExecution).isNotNull();
			assertThat(context.getBean(JobRepository.class).getLastJobExecution("job", jobParameters)).isNotNull();
			assertThat(context.getBean(MongoOperations.class).getCollection("BATCH_JOB_EXECUTION").countDocuments())
				.isPositive();
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class JobConfiguration {

		@Bean
		Job job(JobRepository jobRepository) {
			return new JobBuilder("job", jobRepository)
				.start(new StepBuilder("step1", jobRepository)
					.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED)
					.build())
				.build();
		}

	}

}
