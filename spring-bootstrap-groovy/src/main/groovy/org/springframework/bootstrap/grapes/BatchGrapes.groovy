package org.springframework.bootstrap.grapes

@GrabResolver(name='spring-milestone', root='http://repo.springframework.org/milestone')
@GrabResolver(name='spring-snapshot', root='http://repo.springframework.org/snapshot')
@GrabConfig(systemClassLoader=true)
@Grab("org.springframework.bootstrap:spring-bootstrap:@@version@@")
@Grab("org.springframework.batch:spring-batch-core:2.2.0.M1")
@Grab("org.springframework:spring-context:@@dependency.springframework.version@@")
class BatchGrapes {
}

import org.springframework.bootstrap.context.annotation.ConditionalOnMissingBean
import org.springframework.bootstrap.CommandLineRunner
import org.springframework.batch.core.Job
import org.springframework.batch.core.converter.DefaultJobParametersConverter
import org.springframework.batch.core.converter.JobParametersConverter
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.util.StringUtils
import groovy.util.logging.Log

@Configuration
@ConditionalOnMissingBean(CommandLineRunner)
@Log
class BatchCommand {

  @Autowired(required=false)
  private JobParametersConverter converter = new DefaultJobParametersConverter()

  @Autowired
  private JobLauncher jobLauncher

  @Autowired
  private Job job

  @Bean
  CommandLineRunner batchCommandLineRunner() { 
    return new CommandLineRunner() { 
      void run(String... args) {
        log.info("Running default command line with: ${args}")
        launchJobFromProperties(StringUtils.splitArrayElementsIntoProperties(args, "="))
      }
    }
  }

  protected void launchJobFromProperties(Properties properties) { 
    jobLauncher.run(job, converter.getJobParameters(properties))
  }

}
