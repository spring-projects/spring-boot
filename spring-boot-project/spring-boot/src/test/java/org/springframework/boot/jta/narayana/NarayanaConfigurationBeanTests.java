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

package org.springframework.boot.jta.narayana;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import org.junit.After;
import org.junit.Test;

import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NarayanaConfigurationBean}.
 *
 * @author Gytis Trikleris
 */
public class NarayanaConfigurationBeanTests {

	@After
	@SuppressWarnings("unchecked")
	public void cleanup() {
		((Map<String, Object>) ReflectionTestUtils.getField(BeanPopulator.class,
				"beanInstances")).clear();
	}

	@Test
	public void shouldSetDefaultProperties() throws Exception {
		NarayanaProperties narayanaProperties = new NarayanaProperties();
		NarayanaConfigurationBean narayanaConfigurationBean = new NarayanaConfigurationBean(
				narayanaProperties);
		narayanaConfigurationBean.afterPropertiesSet();

		assertThat(BeanPopulator.getDefaultInstance(CoreEnvironmentBean.class)
				.getNodeIdentifier()).isEqualTo("1");
		assertThat(BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class)
				.getObjectStoreDir()).endsWith("ObjectStore");
		assertThat(BeanPopulator
				.getNamedInstance(ObjectStoreEnvironmentBean.class, "communicationStore")
				.getObjectStoreDir()).endsWith("ObjectStore");
		assertThat(BeanPopulator
				.getNamedInstance(ObjectStoreEnvironmentBean.class, "stateStore")
				.getObjectStoreDir()).endsWith("ObjectStore");
		assertThat(BeanPopulator.getDefaultInstance(CoordinatorEnvironmentBean.class)
				.isCommitOnePhase()).isTrue();
		assertThat(BeanPopulator.getDefaultInstance(CoordinatorEnvironmentBean.class)
				.getDefaultTimeout()).isEqualTo(60);
		assertThat(BeanPopulator.getDefaultInstance(RecoveryEnvironmentBean.class)
				.getPeriodicRecoveryPeriod()).isEqualTo(120);
		assertThat(BeanPopulator.getDefaultInstance(RecoveryEnvironmentBean.class)
				.getRecoveryBackoffPeriod()).isEqualTo(10);

		List<String> xaResourceOrphanFilters = Arrays.asList(
				"com.arjuna.ats.internal.jta.recovery.arjunacore.JTATransactionLogXAResourceOrphanFilter",
				"com.arjuna.ats.internal.jta.recovery.arjunacore.JTANodeNameXAResourceOrphanFilter");
		assertThat(BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class)
				.getXaResourceOrphanFilterClassNames())
						.isEqualTo(xaResourceOrphanFilters);

		List<String> recoveryModules = Arrays.asList(
				"com.arjuna.ats.internal.arjuna.recovery.AtomicActionRecoveryModule",
				"com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule");
		assertThat(BeanPopulator.getDefaultInstance(RecoveryEnvironmentBean.class)
				.getRecoveryModuleClassNames()).isEqualTo(recoveryModules);

		List<String> expiryScanners = Arrays.asList(
				"com.arjuna.ats.internal.arjuna.recovery.ExpiredTransactionStatusManagerScanner");
		assertThat(BeanPopulator.getDefaultInstance(RecoveryEnvironmentBean.class)
				.getExpiryScannerClassNames()).isEqualTo(expiryScanners);

		assertThat(BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class)
				.getXaResourceRecoveryClassNames()).isEmpty();
	}

	@Test
	public void shouldSetModifiedProperties() throws Exception {
		NarayanaProperties narayanaProperties = new NarayanaProperties();
		narayanaProperties.setTransactionManagerId("test-id");
		narayanaProperties.setLogDir("test-dir");
		narayanaProperties.setDefaultTimeout(Duration.ofSeconds(1));
		narayanaProperties.setPeriodicRecoveryPeriod(Duration.ofSeconds(2));
		narayanaProperties.setRecoveryBackoffPeriod(Duration.ofSeconds(3));
		narayanaProperties.setOnePhaseCommit(false);
		narayanaProperties.setXaResourceOrphanFilters(
				Arrays.asList("test-filter-1", "test-filter-2"));
		narayanaProperties
				.setRecoveryModules(Arrays.asList("test-module-1", "test-module-2"));
		narayanaProperties
				.setExpiryScanners(Arrays.asList("test-scanner-1", "test-scanner-2"));

		NarayanaConfigurationBean narayanaConfigurationBean = new NarayanaConfigurationBean(
				narayanaProperties);
		narayanaConfigurationBean.afterPropertiesSet();

		assertThat(BeanPopulator.getDefaultInstance(CoreEnvironmentBean.class)
				.getNodeIdentifier()).isEqualTo("test-id");
		assertThat(BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class)
				.getObjectStoreDir()).isEqualTo("test-dir");
		assertThat(BeanPopulator
				.getNamedInstance(ObjectStoreEnvironmentBean.class, "communicationStore")
				.getObjectStoreDir()).isEqualTo("test-dir");
		assertThat(BeanPopulator
				.getNamedInstance(ObjectStoreEnvironmentBean.class, "stateStore")
				.getObjectStoreDir()).isEqualTo("test-dir");
		assertThat(BeanPopulator.getDefaultInstance(CoordinatorEnvironmentBean.class)
				.isCommitOnePhase()).isFalse();
		assertThat(BeanPopulator.getDefaultInstance(CoordinatorEnvironmentBean.class)
				.getDefaultTimeout()).isEqualTo(1);
		assertThat(BeanPopulator.getDefaultInstance(RecoveryEnvironmentBean.class)
				.getPeriodicRecoveryPeriod()).isEqualTo(2);
		assertThat(BeanPopulator.getDefaultInstance(RecoveryEnvironmentBean.class)
				.getRecoveryBackoffPeriod()).isEqualTo(3);
		assertThat(BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class)
				.getXaResourceOrphanFilterClassNames())
						.isEqualTo(Arrays.asList("test-filter-1", "test-filter-2"));
		assertThat(BeanPopulator.getDefaultInstance(RecoveryEnvironmentBean.class)
				.getRecoveryModuleClassNames())
						.isEqualTo(Arrays.asList("test-module-1", "test-module-2"));
		assertThat(BeanPopulator.getDefaultInstance(RecoveryEnvironmentBean.class)
				.getExpiryScannerClassNames())
						.isEqualTo(Arrays.asList("test-scanner-1", "test-scanner-2"));
	}

}
