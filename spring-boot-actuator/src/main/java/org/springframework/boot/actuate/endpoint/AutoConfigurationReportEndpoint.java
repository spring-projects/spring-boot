/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.AutoConfigurationReportEndpoint.Report;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcomes;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Condition;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * {@link Endpoint} to expose the {@link ConditionEvaluationReport}.
 *
 * @author Greg Turnquist
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 */
@ConfigurationProperties(prefix = "endpoints.autoconfig")
public class AutoConfigurationReportEndpoint extends AbstractEndpoint<Report> {

	@Autowired
	private ConditionEvaluationReport autoConfigurationReport;

	public AutoConfigurationReportEndpoint() {
		super("autoconfig");
	}

	@Override
	public Report invoke() {
		return new Report(this.autoConfigurationReport);
	}

	/**
	 * Adapts {@link ConditionEvaluationReport} to a JSON friendly structure.
	 */
	@JsonPropertyOrder({ "positiveMatches", "negativeMatches", "exclusions" })
	@JsonInclude(Include.NON_EMPTY)
	public static class Report {

		private final MultiValueMap<String, MessageAndCondition> positiveMatches;

		private final Map<String, MessageAndConditions> negativeMatches;

		private final List<String> exclusions;

		private final Report parent;

		public Report(ConditionEvaluationReport report) {
			this.positiveMatches = new LinkedMultiValueMap<String, MessageAndCondition>();
			this.negativeMatches = new LinkedHashMap<String, MessageAndConditions>();
			this.exclusions = report.getExclusions();
			for (Map.Entry<String, ConditionAndOutcomes> entry : report
					.getConditionAndOutcomesBySource().entrySet()) {
				if (entry.getValue().isFullMatch()) {
					add(this.positiveMatches, entry.getKey(), entry.getValue());
				}
				else {
					add(this.negativeMatches, entry.getKey(), entry.getValue());
				}
			}
			boolean hasParent = report.getParent() != null;
			this.parent = (hasParent ? new Report(report.getParent()) : null);
		}

		private void add(Map<String, MessageAndConditions> map, String source,
				ConditionAndOutcomes conditionAndOutcomes) {
			String name = ClassUtils.getShortName(source);
			map.put(name, new MessageAndConditions(conditionAndOutcomes));
		}

		private void add(MultiValueMap<String, MessageAndCondition> map, String source,
				ConditionAndOutcomes conditionAndOutcomes) {
			String name = ClassUtils.getShortName(source);
			for (ConditionAndOutcome conditionAndOutcome : conditionAndOutcomes) {
				map.add(name, new MessageAndCondition(conditionAndOutcome));
			}
		}

		public Map<String, List<MessageAndCondition>> getPositiveMatches() {
			return this.positiveMatches;
		}

		public Map<String, MessageAndConditions> getNegativeMatches() {
			return this.negativeMatches;
		}

		public List<String> getExclusions() {
			return this.exclusions;
		}

		public Report getParent() {
			return this.parent;
		}

	}

	/**
	 * Adapts {@link ConditionAndOutcomes} to a JSON friendly structure.
	 */
	@JsonPropertyOrder({ "notMatched", "matched" })
	public static class MessageAndConditions {

		private final List<MessageAndCondition> notMatched = new ArrayList<MessageAndCondition>();

		private final List<MessageAndCondition> matched = new ArrayList<MessageAndCondition>();

		public MessageAndConditions(ConditionAndOutcomes conditionAndOutcomes) {
			for (ConditionAndOutcome conditionAndOutcome : conditionAndOutcomes) {
				List<MessageAndCondition> target = conditionAndOutcome.getOutcome()
						.isMatch() ? this.matched : this.notMatched;
				target.add(new MessageAndCondition(conditionAndOutcome));
			}
		}

		public List<MessageAndCondition> getNotMatched() {
			return this.notMatched;
		}

		public List<MessageAndCondition> getMatched() {
			return this.matched;
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
