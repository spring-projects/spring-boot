/*
 * Copyright 2012-2015 the original author or authors.
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
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class NarayanaConfigurationBean implements InitializingBean {

	private static final String JBOSSTS_PROPERTIES_FILE_NAME = "jbossts-properties.xml";

	private final NarayanaProperties narayanaProperties;

	public NarayanaConfigurationBean(NarayanaProperties narayanaProperties) {
		this.narayanaProperties = narayanaProperties;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (isPropertiesFileAvailable()) {
			return;
		}

		setNodeIdentifier(this.narayanaProperties.getTransactionManagerId());
		setObjectStoreDir(this.narayanaProperties.getLogDir());
		setCommitOnePhase(this.narayanaProperties.isOnePhaseCommit());
		setDefaultTimeout(this.narayanaProperties.getDefaultTimeout());
		setPeriodicRecoveryPeriod(this.narayanaProperties.getPeriodicRecoveryPeriod());
		setRecoveryBackoffPeriod(this.narayanaProperties.getRecoveryBackoffPeriod());
		setXaResourceOrphanFilters(this.narayanaProperties.getXaResourceOrphanFilters());
		setRecoveryModules(this.narayanaProperties.getRecoveryModules());
		setExpiryScanners(this.narayanaProperties.getExpiryScanners());
	}

	private boolean isPropertiesFileAvailable() {
		return Thread.currentThread().getContextClassLoader().getResource(JBOSSTS_PROPERTIES_FILE_NAME) != null;
	}

	private void setNodeIdentifier(String nodeIdentifier) throws CoreEnvironmentBeanException {
		BeanPopulator.getDefaultInstance(CoreEnvironmentBean.class).setNodeIdentifier(nodeIdentifier);
	}

	private void setObjectStoreDir(String objectStoreDir) {
		BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class).setObjectStoreDir(objectStoreDir);
		BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "communicationStore")
				.setObjectStoreDir(objectStoreDir);
		BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "stateStore").setObjectStoreDir(objectStoreDir);
	}

	private void setCommitOnePhase(boolean isCommitOnePhase) {
		BeanPopulator.getDefaultInstance(CoordinatorEnvironmentBean.class).setCommitOnePhase(isCommitOnePhase);
	}

	private void setDefaultTimeout(int defaultTimeout) {
		BeanPopulator.getDefaultInstance(CoordinatorEnvironmentBean.class).setDefaultTimeout(defaultTimeout);
	}

	private void setPeriodicRecoveryPeriod(int periodicRecoveryPeriod) {
		BeanPopulator.getDefaultInstance(RecoveryEnvironmentBean.class).setPeriodicRecoveryPeriod(periodicRecoveryPeriod);
	}

	private void setRecoveryBackoffPeriod(int recoveryBackoffPeriod) {
		BeanPopulator.getDefaultInstance(RecoveryEnvironmentBean.class).setRecoveryBackoffPeriod(recoveryBackoffPeriod);
	}

	private void setXaResourceOrphanFilters(List<String> xaResourceOrphanFilters) {
		BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class).setXaResourceOrphanFilterClassNames(xaResourceOrphanFilters);
	}

	private void setRecoveryModules(List<String> recoveryModules) {
		BeanPopulator.getDefaultInstance(RecoveryEnvironmentBean.class).setRecoveryModuleClassNames(recoveryModules);
	}

	private void setExpiryScanners(List<String> expiryScanners) {
		BeanPopulator.getDefaultInstance(RecoveryEnvironmentBean.class).setExpiryScannerClassNames(expiryScanners);
	}

}
