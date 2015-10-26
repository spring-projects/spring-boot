package sample.quartz;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SampleQuartzApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleQuartzApplication.class, args);
	}

	@Bean
	public JobDetail jobDetail() {
		return JobBuilder.newJob().ofType(SampleJob.class).withIdentity("sampleJob")
				.storeDurably().build();
	}

	@Bean
	public Trigger trigger() {
		SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
				.withIntervalInSeconds(10).repeatForever();

		return TriggerBuilder.newTrigger().forJob(jobDetail())
				.withIdentity("sampleTrigger").withSchedule(scheduleBuilder).build();
	}

}
