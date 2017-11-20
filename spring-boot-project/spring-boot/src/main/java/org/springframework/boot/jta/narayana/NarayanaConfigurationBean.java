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

import java.util.List;

import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

import org.springframework.beans.factory.InitializingBean;

/**
 * Bean that configures Narayana transaction manager.
 *
 * @author Gytis Trikleris
 * @since 1.4.0
 */
public class NarayanaConfigurationBean implements InitializingBean {

	private static final String JBOSSTS_PROPERTIES_FILE_NAME = "jbossts-properties.xml";

	private final NarayanaProperties properties;

	public NarayanaConfigurationBean(NarayanaProperties narayanaProperties) {
		this.properties = narayanaProperties;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (isPropertiesFileAvailable()) {
			return;
		}
		setNodeIdentifier(this.properties.getTransactionManagerId());
		setObjectStoreDir(this.properties.getLogDir());
		setCommitOnePhase(this.properties.isOnePhaseCommit());
		if (this.properties.getDefaultTimeout() != null) {
			setDefaultTimeout((int) this.properties.getDefaultTimeout().getSeconds());
		}
		if (this.properties.getPeriodicRecoveryPeriod() != null) {
			setPeriodicRecoveryPeriod(
					(int) this.properties.getPeriodicRecoveryPeriod().getSeconds());
		}
		if (this.properties.getRecoveryBackoffPeriod() != null) {
			setRecoveryBackoffPeriod(
					(int) this.properties.getRecoveryBackoffPeriod().getSeconds());
		}
		setXaResourceOrphanFilters(this.properties.getXaResourceOrphanFilters());
		setRecoveryModules(this.properties.getRecoveryModules());
		setExpiryScanners(this.properties.getExpiryScanners());
	}

	private boolean isPropertiesFileAvailable() {
		return Thread.currentThread().getContextClassLoader()
				.getResource(JBOSSTS_PROPERTIES_FILE_NAME) != null;
	}

	private void setNodeIdentifier(String nodeIdentifier)
			throws CoreEnvironmentBeanException {
		getPopulator(CoreEnvironmentBean.class).setNodeIdentifier(nodeIdentifier);
	}

	private void setObjectStoreDir(String objectStoreDir) {
		if (objectStoreDir != null) {
			getPopulator(ObjectStoreEnvironmentBean.class)
					.setObjectStoreDir(objectStoreDir);
			getPopulator(ObjectStoreEnvironmentBean.class, "communicationStore")
					.setObjectStoreDir(objectStoreDir);
			getPopulator(ObjectStoreEnvironmentBean.class, "stateStore")
					.setObjectStoreDir(objectStoreDir);
		}
	}

	private void setCommitOnePhase(boolean isCommitOnePhase) {
		getPopulator(CoordinatorEnvironmentBean.class)
				.setCommitOnePhase(isCommitOnePhase);
	}

	private void setDefaultTimeout(int defaultTimeout) {
		getPopulator(CoordinatorEnvironmentBean.class).setDefaultTimeout(defaultTimeout);
	}

	private void setPeriodicRecoveryPeriod(int periodicRecoveryPeriod) {
		getPopulator(RecoveryEnvironmentBean.class)
				.setPeriodicRecoveryPeriod(periodicRecoveryPeriod);
	}

	private void setRecoveryBackoffPeriod(int recoveryBackoffPeriod) {
		getPopulator(RecoveryEnvironmentBean.class)
				.setRecoveryBackoffPeriod(recoveryBackoffPeriod);
	}

	private void setXaResourceOrphanFilters(List<String> xaResourceOrphanFilters) {
		getPopulator(JTAEnvironmentBean.class)
				.setXaResourceOrphanFilterClassNames(xaResourceOrphanFilters);
	}

	private void setRecoveryModules(List<String> recoveryModules) {
		getPopulator(RecoveryEnvironmentBean.class)
				.setRecoveryModuleClassNames(recoveryModules);
	}

	private void setExpiryScanners(List<String> expiryScanners) {
		getPopulator(RecoveryEnvironmentBean.class)
				.setExpiryScannerClassNames(expiryScanners);
	}

	private <T> T getPopulator(Class<T> beanClass) {
		return BeanPopulator.getDefaultInstance(beanClass);
	}

	private <T> T getPopulator(Class<T> beanClass, String name) {
		return BeanPopulator.getNamedInstance(beanClass, name);
	}

}
