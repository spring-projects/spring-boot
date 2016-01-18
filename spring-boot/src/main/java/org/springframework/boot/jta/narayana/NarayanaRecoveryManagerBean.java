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

import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.arjuna.recovery.RecoveryModule;
import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;
import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;
import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Bean to set up Narayana recovery manager.
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class NarayanaRecoveryManagerBean implements InitializingBean, DisposableBean {

	private final RecoveryManagerService recoveryManagerService;

	public NarayanaRecoveryManagerBean(RecoveryManagerService recoveryManagerService) {
		this.recoveryManagerService = recoveryManagerService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.recoveryManagerService.create();
		this.recoveryManagerService.start();
	}

	@Override
	public void destroy() throws Exception {
		this.recoveryManagerService.stop();
		this.recoveryManagerService.destroy();
	}

	void registerXAResourceRecoveryHelper(XAResourceRecoveryHelper xaResourceRecoveryHelper) {
		getXARecoveryModule(RecoveryManager.manager()).addXAResourceRecoveryHelper(xaResourceRecoveryHelper);
	}

	private XARecoveryModule getXARecoveryModule(RecoveryManager recoveryManager) {
		for (RecoveryModule recoveryModule : recoveryManager.getModules()) {
			if (recoveryModule instanceof XARecoveryModule) {
				return (XARecoveryModule) recoveryModule;
			}
		}

		throw new IllegalStateException("XARecoveryModule is not registered with recovery manager");
	}

}
