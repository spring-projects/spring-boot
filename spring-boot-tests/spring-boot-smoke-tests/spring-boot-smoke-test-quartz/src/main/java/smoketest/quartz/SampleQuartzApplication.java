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

package smoketest.quartz;

import java.util.Calendar;

import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.CronScheduleBuilder;
import org.quartz.DailyTimeIntervalScheduleBuilder;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TimeOfDay;
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
	public JobDetail helloJobDetail() {
		return JobBuilder.newJob(SampleJob.class).withIdentity("helloJob", "samples").usingJobData("name", "World")
				.storeDurably().build();
	}

	@Bean
	public JobDetail anotherJobDetail() {
		return JobBuilder.newJob(SampleJob.class).withIdentity("anotherJob", "samples").usingJobData("name", "Everyone")
				.storeDurably().build();
	}

	@Bean
	public Trigger everyTwoSecTrigger() {
		return TriggerBuilder.newTrigger().forJob("helloJob", "samples").withIdentity("sampleTrigger")
				.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(2).repeatForever()).build();
	}

	@Bean
	public Trigger everyDayTrigger() {
		return TriggerBuilder.newTrigger().forJob("helloJob", "samples").withIdentity("every-day", "samples")
				.withSchedule(SimpleScheduleBuilder.repeatHourlyForever(24)).build();
	}

	@Bean
	public Trigger threeAmWeekdaysTrigger() {
		return TriggerBuilder.newTrigger().forJob("anotherJob", "samples").withIdentity("3am-weekdays", "samples")
				.withSchedule(CronScheduleBuilder.atHourAndMinuteOnGivenDaysOfWeek(3, 0, 1, 2, 3, 4, 5)).build();
	}

	@Bean
	public Trigger onceAWeekTrigger() {
		return TriggerBuilder.newTrigger().forJob("anotherJob", "samples").withIdentity("once-a-week", "samples")
				.withSchedule(CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInWeeks(1))
				.build();
	}

	@Bean
	public Trigger everyHourWorkingHourTuesdayAndThursdayTrigger() {
		return TriggerBuilder.newTrigger().forJob("helloJob", "samples").withIdentity("every-hour-tue-thu", "samples")
				.withSchedule(DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule()
						.onDaysOfTheWeek(Calendar.TUESDAY, Calendar.THURSDAY)
						.startingDailyAt(TimeOfDay.hourAndMinuteOfDay(9, 0))
						.endingDailyAt(TimeOfDay.hourAndMinuteOfDay(18, 0)).withInterval(1, IntervalUnit.HOUR))
				.build();
	}

}
