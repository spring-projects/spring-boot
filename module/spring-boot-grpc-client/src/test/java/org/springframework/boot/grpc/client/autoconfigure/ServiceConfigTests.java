/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.client.autoconfigure;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.grpc.LoadBalancerRegistry;
import io.grpc.NameResolver.ConfigOrError;
import io.grpc.Status.Code;
import io.grpc.internal.AutoConfiguredLoadBalancerFactory;
import io.grpc.internal.ScParser;
import io.grpc.internal.ServiceConfigUtil;
import io.grpc.internal.ServiceConfigUtil.LbConfig;
import io.grpc.internal.ServiceConfigUtil.PolicySelection;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ServiceConfig}.
 *
 * @author Phillip Webb
 */
class ServiceConfigTests {

	@Test
	@WithResource(name = "config.yaml", content = """
			config:
			  load-balancing:
			  - pickfirst: {}
			""")
	void pickFirstLoadBalancing() throws Exception {
		Map<String, Object> map = bindAndGetAsMap();
		assertThat(map).containsKey("loadBalancingConfig");
		List<Map<String, ?>> loadBalancingConfigs = ServiceConfigUtil.getLoadBalancingConfigsFromServiceConfig(map);
		assertThat(loadBalancingConfigs).hasSize(1);
		assertThat(loadBalancingConfigs.get(0)).containsKey("pick_first");
		PolicySelection loadBalancingPolicySelection = getLoadBalancingPolicySelection(loadBalancingConfigs);
		assertThat(loadBalancingPolicySelection.toString()).contains("PickFirstLoadBalancer");
		assertThat(loadBalancingPolicySelection.getConfig()).extracting("shuffleAddressList").isNull();
	}

	@Test
	@WithResource(name = "config.yaml", content = """
			config:
			  load-balancing:
			  - pickfirst:
			      shuffle-address-list: true
			""")
	void pickFirstLoadBalancingWithProperties() throws Exception {
		Map<String, Object> map = bindAndGetAsMap();
		assertThat(map).containsKey("loadBalancingConfig");
		List<Map<String, ?>> loadBalancingConfigs = ServiceConfigUtil.getLoadBalancingConfigsFromServiceConfig(map);
		assertThat(loadBalancingConfigs).hasSize(1);
		assertThat(loadBalancingConfigs.get(0)).containsKey("pick_first");
		PolicySelection loadBalancingPolicySelection = getLoadBalancingPolicySelection(loadBalancingConfigs);
		assertThat(loadBalancingPolicySelection.toString()).contains("PickFirstLoadBalancer");
		assertThat(loadBalancingPolicySelection.getConfig()).extracting("shuffleAddressList").isEqualTo(Boolean.TRUE);
	}

	@Test
	@WithResource(name = "config.yaml", content = """
			config:
			  load-balancing:
			  - roundrobin: {}
			""")
	void roundRobinLoadBalancing() throws Exception {
		Map<String, Object> map = bindAndGetAsMap();
		assertThat(map).containsKey("loadBalancingConfig");
		List<Map<String, ?>> loadBalancingConfigs = ServiceConfigUtil.getLoadBalancingConfigsFromServiceConfig(map);
		assertThat(loadBalancingConfigs).hasSize(1);
		assertThat(loadBalancingConfigs.get(0)).containsKey("round_robin");
		PolicySelection loadBalancingPolicySelection = getLoadBalancingPolicySelection(loadBalancingConfigs);
		assertThat(loadBalancingPolicySelection.toString()).contains("policy=round_robin")
			.contains("no service config");
	}

	@Test
	@WithResource(name = "config.yaml", content = """
			config:
			  load-balancing:
			  - weightedroundrobin: {}
			""")
	void weightedRoundRobinLoadBalancing() throws Exception {
		Map<String, Object> map = bindAndGetAsMap();
		assertThat(map).containsKey("loadBalancingConfig");
		List<Map<String, ?>> loadBalancingConfigs = ServiceConfigUtil.getLoadBalancingConfigsFromServiceConfig(map);
		assertThat(loadBalancingConfigs).hasSize(1);
		assertThat(loadBalancingConfigs.get(0)).containsKey("weighted_round_robin");
		PolicySelection loadBalancingPolicySelection = getLoadBalancingPolicySelection(loadBalancingConfigs);
		assertThat(loadBalancingPolicySelection.toString()).contains("WeightedRoundRobinLoadBalancerProvider");
	}

	@Test
	@WithResource(name = "config.yaml", content = """
			config:
			  load-balancing:
			  - weightedroundrobin:
			      blackout-period: 1m
			      weight-expiration-period: 500ms
			      out-of-band-reporting-period: 1s
			      enable-out-of-band-load-report: true
			      weight-update-period: 2s
			      error-utilization-penalty: 0.5
			""")
	void weightedRoundRobinLoadBalancingWithProperties() throws Exception {
		Map<String, Object> map = bindAndGetAsMap();
		assertThat(map).containsKey("loadBalancingConfig");
		List<Map<String, ?>> loadBalancingConfigs = ServiceConfigUtil.getLoadBalancingConfigsFromServiceConfig(map);
		assertThat(loadBalancingConfigs).hasSize(1);
		assertThat(loadBalancingConfigs.get(0)).containsKey("weighted_round_robin");
		PolicySelection loadBalancingPolicySelection = getLoadBalancingPolicySelection(loadBalancingConfigs);
		assertThat(loadBalancingPolicySelection.toString()).contains("WeightedRoundRobinLoadBalancerProvider");
		assertThat(loadBalancingPolicySelection.getConfig()).extracting("blackoutPeriodNanos")
			.isEqualTo(Duration.ofMinutes(1).toNanos());
		assertThat(loadBalancingPolicySelection.getConfig()).extracting("weightExpirationPeriodNanos")
			.isEqualTo(Duration.ofMillis(500).toNanos());
		assertThat(loadBalancingPolicySelection.getConfig()).extracting("enableOobLoadReport").isEqualTo(true);
		assertThat(loadBalancingPolicySelection.getConfig()).extracting("oobReportingPeriodNanos")
			.isEqualTo(Duration.ofSeconds(1).toNanos());
		assertThat(loadBalancingPolicySelection.getConfig()).extracting("weightUpdatePeriodNanos")
			.isEqualTo(Duration.ofSeconds(2).toNanos());
		assertThat(loadBalancingPolicySelection.getConfig()).extracting("errorUtilizationPenalty").isEqualTo(0.5f);
	}

	@Test
	@WithResource(name = "config.yaml", content = """
			config:
			  load-balancing:
			  - grpc:
			      child:
			      - roundrobin: {}
			      - pickfirst: {}
			      service-name: test
			      initial-fallback-timeout: 10s
			""")
	void grpcLoadBalancingWithProperties() throws Exception {
		Map<String, Object> map = bindAndGetAsMap();
		assertThat(map).containsKey("loadBalancingConfig");
		List<Map<String, ?>> loadBalancingConfigs = ServiceConfigUtil.getLoadBalancingConfigsFromServiceConfig(map);
		assertThat(loadBalancingConfigs).hasSize(1);
		assertThat(loadBalancingConfigs.get(0)).containsKey("grpclb");
		PolicySelection loadBalancingPolicySelection = getLoadBalancingPolicySelection(loadBalancingConfigs);
		assertThat(loadBalancingPolicySelection.toString()).contains("GrpclbLoadBalancerProvider");
		assertThat(loadBalancingPolicySelection.getConfig()).extracting("mode").hasToString("ROUND_ROBIN");
		assertThat(loadBalancingPolicySelection.getConfig()).extracting("serviceName").isEqualTo("test");
		assertThat(loadBalancingPolicySelection.getConfig()).extracting("fallbackTimeoutMs")
			.isEqualTo(Duration.ofSeconds(10).toMillis());
	}

	@Test
	@WithResource(name = "config.yaml", content = """
			config:
			  load-balancing:
			  - pickfirst: {}
			  - weightedroundrobin: {}
			""")
	void multipleLoadBalancerPolicies() throws Exception {
		Map<String, Object> map = bindAndGetAsMap();
		assertThat(map).containsKey("loadBalancingConfig");
		List<Map<String, ?>> loadBalancingConfigs = ServiceConfigUtil.getLoadBalancingConfigsFromServiceConfig(map);
		assertThat(loadBalancingConfigs).hasSize(2);
		assertThat(loadBalancingConfigs.get(0)).containsKey("pick_first");
		assertThat(loadBalancingConfigs.get(1)).containsKey("weighted_round_robin");
	}

	@Test
	@WithResource(name = "config.yaml", content = """
			config:
			  load-balancing:
			  - pickfirst: {}
			    weightedroundrobin: {}
			""")
	void whenMultileLoadBalancingPoliciesInListItemThrowsException() {
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> bindAndGetAsMap())
			.havingRootCause()
			.isInstanceOf(MutuallyExclusiveConfigurationPropertiesException.class);
	}

	@Test
	@WithResource(name = "config.yaml", content = """
			config:
			  load-balancing:
			  - {}
			""")
	void whenNoLoadBalancingPoliciesInListItemThrowsException() {
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> bindAndGetAsMap())
			.havingRootCause()
			.isInstanceOf(InvalidConfigurationPropertyValueException.class);
	}

	@Test
	@WithResource(name = "config.yaml", content = """
			config:
			  method:
			  - name:
			    - service: s-one
			      method: m-one
			    - service: s-two
			      method: m-two
			    wait-for-ready: true
			    max-request-message: 10KB
			    max-response-message: 20KB
			    timeout: 30s
			""")
	@SuppressWarnings("unchecked")
	void methodConfig() throws Exception {
		Map<String, Object> map = bindAndGetAsMap();
		assertThat(map).containsKey("methodConfig");
		Map<String, ?> serviceMethodMap = getServiceMethodMap(map, false);
		assertThat(serviceMethodMap).containsOnlyKeys("s-one/m-one", "s-two/m-two");
		Object methodInfo = serviceMethodMap.get("s-one/m-one");
		assertThat(methodInfo).extracting("timeoutNanos").isEqualTo(Duration.ofSeconds(30).toNanos());
		assertThat(methodInfo).extracting("waitForReady").isEqualTo(Boolean.TRUE);
		assertThat(methodInfo).extracting("maxOutboundMessageSize").isEqualTo(10240);
		assertThat(methodInfo).extracting("maxInboundMessageSize").isEqualTo(20480);
	}

	@Test
	@WithResource(name = "config.yaml", content = """
			config:
			  method:
			  - name:
			    - service: s-one
			      method: m-one
			    retry:
			      max-attempts: 2
			      initial-backoff: 1m
			      max-backoff: 1h
			      backoff-multiplier: 2.5
			      per-attempt-receive-timeout: 2s
			      retryable-status-codes:
			      - cancelled
			      - already-exists
			""")
	void methodConfigRetryPolicy() throws Exception {
		Map<String, Object> map = bindAndGetAsMap();
		Map<String, ?> serviceMethodMap = getServiceMethodMap(map, true);
		Object methodInfo = serviceMethodMap.get("s-one/m-one");
		assertThat(methodInfo).extracting("retryPolicy.maxAttempts").isEqualTo(2);
		assertThat(methodInfo).extracting("retryPolicy.initialBackoffNanos").isEqualTo(Duration.ofMinutes(1).toNanos());
		assertThat(methodInfo).extracting("retryPolicy.maxBackoffNanos").isEqualTo(Duration.ofHours(1).toNanos());
		assertThat(methodInfo).extracting("retryPolicy.backoffMultiplier").isEqualTo(2.5);
		assertThat(methodInfo).extracting("retryPolicy.perAttemptRecvTimeoutNanos")
			.isEqualTo(Duration.ofSeconds(2).toNanos());
		assertThat(methodInfo).extracting("retryPolicy.retryableStatusCodes")
			.asInstanceOf(InstanceOfAssertFactories.SET)
			.containsExactlyInAnyOrder(Code.CANCELLED, Code.ALREADY_EXISTS);
	}

	@Test
	@WithResource(name = "config.yaml", content = """
			config:
			  method:
			  - name:
			    - service: s-one
			      method: m-one
			    hedging:
			      max-attempts: 4
			      delay: 6s
			      non-fatal-status-codes:
			      - invalid-argument
			      - deadline-exceeded
			""")
	void methodConfigHedgingPolicy() throws Exception {
		Map<String, Object> map = bindAndGetAsMap();
		Map<String, ?> serviceMethodMap = getServiceMethodMap(map, true);
		Object methodInfo = serviceMethodMap.get("s-one/m-one");
		assertThat(methodInfo).extracting("hedgingPolicy.maxAttempts").isEqualTo(4);
		assertThat(methodInfo).extracting("hedgingPolicy.hedgingDelayNanos").isEqualTo(Duration.ofSeconds(6).toNanos());
		assertThat(methodInfo).extracting("hedgingPolicy.nonFatalStatusCodes")
			.asInstanceOf(InstanceOfAssertFactories.SET)
			.containsExactlyInAnyOrder(Code.INVALID_ARGUMENT, Code.DEADLINE_EXCEEDED);
	}

	@Test
	@WithResource(name = "config.yaml", content = """
			config:
			  method:
			  - name:
			    - service: s-one
			      method: m-one
			    retry: {}
			    hedging: {}
			""")
	void whenMultiplePoliciesInMethodConfigThrowsException() {
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> bindAndGetAsMap())
			.havingRootCause()
			.isInstanceOf(MutuallyExclusiveConfigurationPropertiesException.class);
	}

	@Test
	@WithResource(name = "config.yaml", content = """
			config:
			  retrythrottling:
			    max-tokens: 2.5
			    token-ratio: 1.5
			""")
	void retryThrottling() throws Exception {
		Map<String, Object> map = bindAndGetAsMap();
		assertThat(map).containsKey("retryThrottling");
		Object throttle = ReflectionTestUtils.invokeMethod(ServiceConfigUtil.class, "getThrottlePolicy", map);
		assertThat(throttle).extracting("maxTokens").isEqualTo(2500);
		assertThat(throttle).extracting("tokenRatio").isEqualTo(1500);
	}

	@Test
	@WithResource(name = "config.yaml", content = """
			config:
			  healthcheck:
			    service-name: test
			""")
	@SuppressWarnings("unchecked")
	void healthCheck() throws Exception {
		Map<String, Object> map = bindAndGetAsMap();
		assertThat(map).containsKey("healthCheckConfig");
		Map<String, Object> healthCheckedService = (Map<String, Object>) ServiceConfigUtil.getHealthCheckedService(map);
		assertThat(healthCheckedService).hasSize(1).containsEntry("serviceName", "test");
	}

	private PolicySelection getLoadBalancingPolicySelection(List<Map<String, ?>> rawConfigs) {
		List<LbConfig> unwrappedConfigs = ServiceConfigUtil.unwrapLoadBalancingConfigList(rawConfigs);
		LoadBalancerRegistry registry = LoadBalancerRegistry.getDefaultRegistry();
		ConfigOrError selected = ServiceConfigUtil.selectLbPolicyFromList(unwrappedConfigs, registry);
		assertThat(selected).isNotNull();
		PolicySelection policySelection = (PolicySelection) selected.getConfig();
		if (policySelection == null) {
			System.err.println(selected);
			System.err.println(selected.getError());
			if (selected.getError() != null && selected.getError().asException() != null) {
				selected.getError().asException().printStackTrace();
			}
		}
		assertThat(policySelection).isNotNull();
		return policySelection;
	}

	@SuppressWarnings("unchecked")
	private Map<String, ?> getServiceMethodMap(Map<String, Object> map, boolean retryEnabled) {
		ScParser scParser = new ScParser(retryEnabled, 100, 100, new AutoConfiguredLoadBalancerFactory("pick_first"));
		Object config = scParser.parseServiceConfig(map).getConfig();
		assertThat(config).isNotNull();
		Object serviceMethodMap = ReflectionTestUtils.getField(config, "serviceMethodMap");
		assertThat(serviceMethodMap).isNotNull();
		return (Map<String, ?>) serviceMethodMap;
	}

	private Map<String, Object> bindAndGetAsMap() throws Exception {
		Map<String, Object> map = new LinkedHashMap<>();
		bind().applyTo(map);
		return map;
	}

	private ServiceConfig bind() throws Exception {
		YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
		PropertySource<?> propertySource = loader.load("config.yaml", new ClassPathResource("config.yaml")).get(0);
		MockEnvironment environment = new MockEnvironment();
		environment.getPropertySources().addLast(propertySource);
		Binder binder = Binder.get(environment);
		return binder.bind("config", ServiceConfig.class).get();
	}

}
