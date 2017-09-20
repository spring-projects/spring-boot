/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.quartz;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;

import org.springframework.boot.actuate.quartz.QuartzEndpoint.QuartzJob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link QuartzEndpoint}.
 *
 * @author Vedran Pavic
 */
public class QuartzEndpointTests {

	private final Scheduler scheduler = mock(Scheduler.class);

	private final QuartzEndpoint endpoint = new QuartzEndpoint(this.scheduler);

	private final JobDetail jobDetail = JobBuilder.newJob(Job.class).withIdentity("testJob").build();

	private final Trigger trigger = TriggerBuilder.newTrigger().forJob(this.jobDetail).withIdentity("testTrigger")
			.build();

	@Test
	public void quartzReport() throws Exception {
		String jobGroup = this.jobDetail.getKey().getGroup();
		given(this.scheduler.getJobGroupNames()).willReturn(Collections.singletonList(jobGroup));
		given(this.scheduler.getJobKeys(GroupMatcher.jobGroupEquals(jobGroup)))
				.willReturn(Collections.singleton(this.jobDetail.getKey()));
		Map<String, Object> quartzReport = this.endpoint.quartzReport();
		assertThat(quartzReport).hasSize(1);
	}

	@Test
	public void quartzJob() throws Exception {
		JobKey jobKey = this.jobDetail.getKey();
		given(this.scheduler.getJobDetail(jobKey)).willReturn(this.jobDetail);
		given(this.scheduler.getTriggersOfJob(jobKey))
				.willAnswer(invocation -> Collections.singletonList(this.trigger));
		QuartzJob quartzJob = this.endpoint.quartzJob(jobKey.getGroup(), jobKey.getName());
		assertThat(quartzJob.getJobGroup()).isEqualTo(jobKey.getGroup());
		assertThat(quartzJob.getJobName()).isEqualTo(jobKey.getName());
		assertThat(quartzJob.getClassName()).isEqualTo(this.jobDetail.getJobClass().getName());
		assertThat(quartzJob.getTriggers()).hasSize(1);
		assertThat(quartzJob.getTriggers().get(0).getTriggerGroup()).isEqualTo(this.trigger.getKey().getGroup());
		assertThat(quartzJob.getTriggers().get(0).getTriggerName()).isEqualTo(this.trigger.getKey().getName());
	}

}
