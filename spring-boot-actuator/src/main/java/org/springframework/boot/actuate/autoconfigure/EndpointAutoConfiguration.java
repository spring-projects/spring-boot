/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.AutoConfigurationReportEndpoint;
import org.springframework.boot.actuate.endpoint.BeansEndpoint;
import org.springframework.boot.actuate.endpoint.ConfigurationPropertiesReportEndpoint;
import org.springframework.boot.actuate.endpoint.DumpEndpoint;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.endpoint.InfoEndpoint;
import org.springframework.boot.actuate.endpoint.MetricsEndpoint;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.endpoint.RequestMappingEndpoint;
import org.springframework.boot.actuate.endpoint.ShutdownEndpoint;
import org.springframework.boot.actuate.endpoint.TraceEndpoint;
import org.springframework.boot.actuate.endpoint.VanillaPublicMetrics;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository;
import org.springframework.boot.actuate.trace.InMemoryTraceRepository;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.boot.context.properties.ConfigurationBeanFactoryMetaData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for common management
 * {@link Endpoint}s.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 * @author Greg Turnquist
 * @author Christian Dupuis
 */
@Configuration
public class EndpointAutoConfiguration {

	@Autowired
	private InfoPropertiesConfiguration properties;

	@Autowired(required = false)
	private HealthAggregator healthAggregator = new OrderedHealthAggregator();

	@Autowired(required = false)
	Map<String, HealthIndicator> healthIndicators = new HashMap<String, HealthIndicator>();

	@Autowired(required = false)
	private MetricReader metricRepository = new InMemoryMetricRepository();

	@Autowired(required = false)
	private PublicMetrics metrics;

	@Autowired(required = false)
	private TraceRepository traceRepository = new InMemoryTraceRepository();

	@Autowired(required = false)
	private ConfigurationBeanFactoryMetaData beanFactoryMetaData;

	@Bean
	@ConditionalOnMissingBean
	public EnvironmentEndpoint environmentEndpoint() {
		return new EnvironmentEndpoint();
	}

	@Bean
	@ConditionalOnMissingBean
	public HealthEndpoint healthEndpoint() {
		return new HealthEndpoint(this.healthAggregator, this.healthIndicators);
	}

	@Bean
	@ConditionalOnMissingBean
	public BeansEndpoint beansEndpoint() {
		return new BeansEndpoint();
	}

	@Bean
	@ConditionalOnMissingBean
	public InfoEndpoint infoEndpoint() throws Exception {
		LinkedHashMap<String, Object> info = new LinkedHashMap<String, Object>();
		info.putAll(this.properties.infoMap());
		GitInfo gitInfo = this.properties.gitInfo();
		if (gitInfo.getBranch() != null) {
			info.put("git", gitInfo);
		}
		return new InfoEndpoint(info);
	}

	@Bean
	@ConditionalOnMissingBean
	public MetricsEndpoint metricsEndpoint() {
		if (this.metrics == null) {
			this.metrics = new VanillaPublicMetrics(this.metricRepository);
		}
		return new MetricsEndpoint(this.metrics);
	}

	@Bean
	@ConditionalOnMissingBean
	public TraceEndpoint traceEndpoint() {
		return new TraceEndpoint(this.traceRepository);
	}

	@Bean
	@ConditionalOnMissingBean
	public DumpEndpoint dumpEndpoint() {
		return new DumpEndpoint();
	}

	@Bean
	@ConditionalOnBean(ConditionEvaluationReport.class)
	@ConditionalOnMissingBean(search = SearchStrategy.CURRENT)
	public AutoConfigurationReportEndpoint autoConfigurationAuditEndpoint() {
		return new AutoConfigurationReportEndpoint();
	}

	@Bean
	@ConditionalOnMissingBean
	public ShutdownEndpoint shutdownEndpoint() {
		return new ShutdownEndpoint();
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

	@Bean
	@ConditionalOnMissingBean
	public ConfigurationPropertiesReportEndpoint configurationPropertiesReportEndpoint() {
		ConfigurationPropertiesReportEndpoint endpoint = new ConfigurationPropertiesReportEndpoint();
		endpoint.setConfigurationBeanFactoryMetaData(this.beanFactoryMetaData);
		return endpoint;
	}

	@Configuration
	protected static class InfoPropertiesConfiguration {

		@Autowired
		private final ConfigurableEnvironment environment = new StandardEnvironment();

		@Value("${spring.git.properties:classpath:git.properties}")
		private Resource gitProperties;

		public GitInfo gitInfo() throws Exception {
			PropertiesConfigurationFactory<GitInfo> factory = new PropertiesConfigurationFactory<GitInfo>(
					new GitInfo());
			factory.setTargetName("git");
			Properties properties = new Properties();
			if (this.gitProperties.exists()) {
				properties = PropertiesLoaderUtils.loadProperties(this.gitProperties);
			}
			factory.setProperties(properties);
			return factory.getObject();
		}

		public Map<String, Object> infoMap() throws Exception {
			PropertiesConfigurationFactory<Map<String, Object>> factory = new PropertiesConfigurationFactory<Map<String, Object>>(
					new LinkedHashMap<String, Object>());
			factory.setTargetName("info");
			factory.setPropertySources(this.environment.getPropertySources());
			return factory.getObject();
		}

	}

	public static class GitInfo {

		private String branch;

		private final Commit commit = new Commit();

		public String getBranch() {
			return this.branch;
		}

		public void setBranch(String branch) {
			this.branch = branch;
		}

		public Commit getCommit() {
			return this.commit;
		}

		public static class Commit {

			private String id;

			private String time;

			public String getId() {
				return this.id == null ? "" : (this.id.length() > 7 ? this.id.substring(
						0, 7) : this.id);
			}

			public void setId(String id) {
				this.id = id;
			}

			public String getTime() {
				return this.time;
			}

			public void setTime(String time) {
				this.time = time;
			}

		}

	}

}
