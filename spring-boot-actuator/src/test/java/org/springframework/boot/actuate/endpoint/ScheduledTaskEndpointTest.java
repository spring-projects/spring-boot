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

package org.springframework.boot.actuate.endpoint;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;


import static org.assertj.core.api.Assertions.assertThat;

public class ScheduledTaskEndpointTest extends AbstractEndpointTests<ScheduledTaskEndpoint> {

	public ScheduledTaskEndpointTest() {
		super(Config.class, ScheduledTaskEndpoint.class, "schedules", true, "endpoints.schedules");
	}

	@Test
	public void shouldGetTaskInformation() throws Exception {
		List<ScheduledTaskEndpoint.ScheduledTaskInformation> invoke = getEndpointBean().invoke();

		assertThat(invoke.size()).isEqualTo(3);

		Optional<ScheduledTaskEndpoint.ScheduledTaskInformation> fixedRateTaskOptional =
				filterTaskInformation(invoke, ScheduledTaskEndpoint.ScheduledType.FIXEDRATE);
		assertThat(fixedRateTaskOptional.isPresent()).isTrue();
		assertThat(fixedRateTaskOptional.get().getInterval()).isEqualTo(2000);
		assertThat(fixedRateTaskOptional.get().getInitialDelay()).isEqualTo(0);

		Optional<ScheduledTaskEndpoint.ScheduledTaskInformation> cronTaskOptional =
				filterTaskInformation(invoke, ScheduledTaskEndpoint.ScheduledType.CRON);
		assertThat(cronTaskOptional.isPresent()).isTrue();
		assertThat(cronTaskOptional.get().getExpression()).isEqualTo("* */1 * * * *");
		assertThat(cronTaskOptional.get().getInitialDelay()).isEqualTo(0);
		assertThat(cronTaskOptional.get().getName()).contains("Config.cronMethod");
	}

	private Optional<ScheduledTaskEndpoint.ScheduledTaskInformation> filterTaskInformation(
			List<ScheduledTaskEndpoint.ScheduledTaskInformation> invoke, ScheduledTaskEndpoint.ScheduledType type) {
		return invoke.stream().filter(scheduledTaskInformation -> scheduledTaskInformation.getType()
				.equals(type)).findFirst();
	}

	@Configuration
	@EnableConfigurationProperties
	@EnableScheduling
	public static class Config {

		@Bean
		public ScheduledTaskEndpoint endpoint() {
			return new ScheduledTaskEndpoint();
		}

		@Scheduled(fixedDelay = 1000)
		public void fixedDelayMethod() {

		}

		@Scheduled(fixedRate = 2000)
		public void fixedRateMethod() {

		}

		@Scheduled(cron = "* */1 * * * *")
		public void cronMethod() {

		}
	}
}
