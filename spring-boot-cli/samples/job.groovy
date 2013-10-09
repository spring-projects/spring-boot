package org.test

@Grab("hsqldb")
@Configuration
@EnableBatchProcessing
class JobConfig {

	@Autowired
	private JobBuilderFactory jobs

	@Autowired
	private StepBuilderFactory steps

	@Bean
	protected Tasklet tasklet() {
		return new Tasklet() { 
          @Override
          RepeatStatus execute(StepContribution contribution, ChunkContext context) {
            return RepeatStatus.FINISHED
          }
        }
	}

	@Bean
	Job job() throws Exception {
		return jobs.get("job").start(step1()).build()
	}

	@Bean
	protected Step step1() throws Exception {
		return steps.get("step1").tasklet(tasklet()).build()
	}
}


