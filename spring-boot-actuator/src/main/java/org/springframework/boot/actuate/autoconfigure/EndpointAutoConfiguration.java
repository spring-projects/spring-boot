/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import liquibase.integration.spring.SpringLiquibase;
import org.flywaydb.core.Flyway;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.AutoConfigurationReportEndpoint;
import org.springframework.boot.actuate.endpoint.BeansEndpoint;
import org.springframework.boot.actuate.endpoint.ConfigurationPropertiesReportEndpoint;
import org.springframework.boot.actuate.endpoint.DumpEndpoint;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.EndpointProperties;
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;
import org.springframework.boot.actuate.endpoint.FlywayEndpoint;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.endpoint.InfoEndpoint;
import org.springframework.boot.actuate.endpoint.LiquibaseEndpoint;
import org.springframework.boot.actuate.endpoint.LoggersEndpoint;
import org.springframework.boot.actuate.endpoint.MetricsEndpoint;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.endpoint.RequestMappingEndpoint;
import org.springframework.boot.actuate.endpoint.ShutdownEndpoint;
import org.springframework.boot.actuate.endpoint.TraceEndpoint;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.trace.InMemoryTraceRepository;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for common management
 * {@link Endpoint}s.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Greg Turnquist
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Meang Akira Tanaka
 * @author Ben Hale
 */
@Configuration
@AutoConfigureAfter({ FlywayAutoConfiguration.class, LiquibaseAutoConfiguration.class })
@EnableConfigurationProperties(EndpointProperties.class)
public class EndpointAutoConfiguration {

	private final HealthAggregator healthAggregator;

	private final Map<String, HealthIndicator> healthIndicators;

	private final List<InfoContributor> infoContributors;

	private final Collection<PublicMetrics> publicMetrics;

	private final TraceRepository traceRepository;

	public EndpointAutoConfiguration(ObjectProvider<HealthAggregator> healthAggregator,
			ObjectProvider<Map<String, HealthIndicator>> healthIndicators,
			ObjectProvider<List<InfoContributor>> infoContributors,
			ObjectProvider<Collection<PublicMetrics>> publicMetrics,
			ObjectProvider<TraceRepository> traceRepository) {
		this.healthAggregator = healthAggregator.getIfAvailable();
		this.healthIndicators = healthIndicators.getIfAvailable();
		this.infoContributors = infoContributors.getIfAvailable();
		this.publicMetrics = publicMetrics.getIfAvailable();
		this.traceRepository = traceRepository.getIfAvailable();
	}

	@Bean
	@ConditionalOnMissingBean
	public EnvironmentEndpoint environmentEndpoint() {
		return new EnvironmentEndpoint();
	}

	@Bean
	@ConditionalOnMissingBean
	public HealthEndpoint healthEndpoint() {
		HealthAggregator healthAggregator = (this.healthAggregator != null)
				? this.healthAggregator : new OrderedHealthAggregator();
		Map<String, HealthIndicator> healthIndicators = (this.healthIndicators != null)
				? this.healthIndicators : Collections.<String, HealthIndicator>emptyMap();
		return new HealthEndpoint(healthAggregator, healthIndicators);
	}

	@Bean
	@ConditionalOnMissingBean
	public BeansEndpoint beansEndpoint() {
		return new BeansEndpoint();
	}

	@Bean
	@ConditionalOnMissingBean
	public InfoEndpoint infoEndpoint() throws Exception {
		return new InfoEndpoint((this.infoContributors != null) ? this.infoContributors
				: Collections.<InfoContributor>emptyList());
	}

	@Bean
	@ConditionalOnBean(LoggingSystem.class)
	@ConditionalOnMissingBean
	public LoggersEndpoint loggersEndpoint(LoggingSystem loggingSystem) {
		return new LoggersEndpoint(loggingSystem);
	}

	@Bean
	@ConditionalOnMissingBean
	public MetricsEndpoint metricsEndpoint() {
		List<PublicMetrics> publicMetrics = new ArrayList<PublicMetrics>();
		if (this.publicMetrics != null) {
			publicMetrics.addAll(this.publicMetrics);
		}
		Collections.sort(publicMetrics, AnnotationAwareOrderComparator.INSTANCE);
		return new MetricsEndpoint(publicMetrics);
	}

	@Bean
	@ConditionalOnMissingBean
	public TraceEndpoint traceEndpoint() {
		return new TraceEndpoint((this.traceRepository != null) ? this.traceRepository
				: new InMemoryTraceRepository());
	}

	@Bean
	@ConditionalOnMissingBean
	public DumpEndpoint dumpEndpoint() {
		return new DumpEndpoint();
	}

	@Bean
	@ConditionalOnBean(ConditionEvaluationReport.class)
	@ConditionalOnMissingBean(search = SearchStrategy.CURRENT)
	public AutoConfigurationReportEndpoint autoConfigurationReportEndpoint() {
		return new AutoConfigurationReportEndpoint();
	}

	@Bean
	@ConditionalOnMissingBean
	public ShutdownEndpoint shutdownEndpoint() {
		return new ShutdownEndpoint();
	}

	@Bean
	@ConditionalOnMissingBean
	public ConfigurationPropertiesReportEndpoint configurationPropertiesReportEndpoint() {
		return new ConfigurationPropertiesReportEndpoint();
	}

	@Configuration
	@ConditionalOnBean(Flyway.class)
	@ConditionalOnClass(Flyway.class)
	static class FlywayEndpointConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public FlywayEndpoint flywayEndpoint(Map<String, Flyway> flyways) {
			return new FlywayEndpoint(flyways);
		}

	}

	@Configuration
	@ConditionalOnBean(SpringLiquibase.class)
	@ConditionalOnClass(SpringLiquibase.class)
	static class LiquibaseEndpointConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public LiquibaseEndpoint liquibaseEndpoint(
				Map<String, SpringLiquibase> liquibases) {
			return new LiquibaseEndpoint(liquibases);
		}

	}

	@Configuration
	@ConditionalOnClass(AbstractHandlerMethodMapping.class)
	protected static class RequestMappingEndpointConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public RequestMappingEndpoint requestMappingEndpoint() {
			RequestMappingEndpoint endpoint = new RequestMappingEndpoint();
			return endpoint;
		}

	}

}
