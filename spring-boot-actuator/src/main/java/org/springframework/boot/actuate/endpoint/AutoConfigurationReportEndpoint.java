/*
 * Copyright 2012-2013 the original author or authors.
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
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.AutoConfigurationReportEndpoint.Report;
import org.springframework.boot.autoconfigure.AutoConfigurationReport;
import org.springframework.boot.autoconfigure.AutoConfigurationReport.ConditionAndOutcome;
import org.springframework.boot.autoconfigure.AutoConfigurationReport.ConditionAndOutcomes;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Condition;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * {@link Endpoint} to expose the {@link AutoConfigurationReport}.
 * 
 * @author Greg Turnquist
 * @author Phillip Webb
 */
@ConfigurationProperties(name = "endpoints.autoconfig", ignoreUnknownFields = false)
public class AutoConfigurationReportEndpoint extends AbstractEndpoint<Report> {

	@Autowired
	private AutoConfigurationReport autoConfigurationReport;

	public AutoConfigurationReportEndpoint() {
		super("/autoconfig");
	}

	@Override
	public Report invoke() {
		return new Report(this.autoConfigurationReport);
	}

	/**
	 * Adapts {@link AutoConfigurationReport} to a JSON friendly structure.
	 */
	@JsonPropertyOrder({ "positiveMatches", "negativeMatches" })
	public static class Report {

		private MultiValueMap<String, MessageAndCondition> positiveMatches;

		private MultiValueMap<String, MessageAndCondition> negativeMatches;

		public Report(AutoConfigurationReport report) {
			this.positiveMatches = new LinkedMultiValueMap<String, MessageAndCondition>();
			this.negativeMatches = new LinkedMultiValueMap<String, MessageAndCondition>();
			for (Map.Entry<String, ConditionAndOutcomes> entry : report
					.getConditionAndOutcomesBySource().entrySet()) {
				dunno(entry.getValue().isFullMatch() ? this.positiveMatches
						: this.negativeMatches, entry.getKey(), entry.getValue());

			}

		}

		private void dunno(MultiValueMap<String, MessageAndCondition> map, String source,
				ConditionAndOutcomes conditionAndOutcomes) {
			String name = ClassUtils.getShortName(source);
			for (ConditionAndOutcome conditionAndOutcome : conditionAndOutcomes) {
				map.add(name, new MessageAndCondition(conditionAndOutcome));
			}
		}

		public Map<String, List<MessageAndCondition>> getPositiveMatches() {
			return this.positiveMatches;
		}

		public Map<String, List<MessageAndCondition>> getNegativeMatches() {
			return this.negativeMatches;
		}

	}

	/**
	 * Adapts {@link ConditionAndOutcome} to a JSON friendly structure.
	 */
	@JsonPropertyOrder({ "condition", "message" })
	public static class MessageAndCondition {

		private final String condition;

		private final String message;

		public MessageAndCondition(ConditionAndOutcome conditionAndOutcome) {
			Condition condition = conditionAndOutcome.getCondition();
			ConditionOutcome outcome = conditionAndOutcome.getOutcome();
			this.condition = ClassUtils.getShortName(condition.getClass());
			if (StringUtils.hasLength(outcome.getMessage())) {
				this.message = outcome.getMessage();
			}
			else {
				this.message = (outcome.isMatch() ? "matched" : "did not match");
			}
		}

		public String getCondition() {
			return this.condition;
		}

		public String getMessage() {
			return this.message;
		}

	}
}
