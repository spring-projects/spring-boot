package org.test

@Grab("org.hsqldb:hsqldb-j5:2.0.0")
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

import groovy.util.logging.Log
import org.springframework.util.StringUtils
import groovy.util.logging.Log

@Component
@Log
class JobRunner implements CommandLineRunner {

	@Autowired(required=false)
	private JobParametersConverter converter = new DefaultJobParametersConverter()

	@Autowired
	private JobLauncher jobLauncher

	@Autowired
	private Job job

	void run(String... args) {
		log.info("Running default command line with: ${args}")
		launchJobFromProperties(StringUtils.splitArrayElementsIntoProperties(args, "="))
	}

	protected void launchJobFromProperties(Properties properties) {
		jobLauncher.run(job, converter.getJobParameters(properties))
	}
}

import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator

@Component
class DatabaseInitializer {

	@Autowired
	private DataSource dataSource

	@Autowired
	private ResourceLoader resourceLoader

	@PostConstruct
	protected void initialize() {
		String platform = org.springframework.batch.support.DatabaseType.fromMetaData(dataSource).toString().toLowerCase()
		if (platform=="hsql") {
			platform = "hsqldb"
		}
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator()
		populator.addScript(resourceLoader.getResource("org/springframework/batch/core/schema-${platform}.sql"))
		populator.setContinueOnError(true)
		DatabasePopulatorUtils.execute(populator, dataSource)
	}
}

