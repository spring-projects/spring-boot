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

package org.springframework.boot.jta.narayana;

import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class NarayanaRecoveryManagerBeanTests {

	private RecoveryManagerService recoveryManagerService;

	private NarayanaRecoveryManagerBean narayanaRecoveryManagerBean;

	@Before
	public void before() {
		this.recoveryManagerService = mock(RecoveryManagerService.class);
		this.narayanaRecoveryManagerBean = new NarayanaRecoveryManagerBean(this.recoveryManagerService);
	}

	@Test
	public void shouldCreateAndStartRecoveryManagerService() throws Exception {
		this.narayanaRecoveryManagerBean.afterPropertiesSet();

		verify(this.recoveryManagerService, times(1)).create();
		verify(this.recoveryManagerService, times(1)).start();
	}

	@Test
	public void shouldStopAndDestroyRecoveryManagerService() throws Exception {
		this.narayanaRecoveryManagerBean.destroy();

		verify(this.recoveryManagerService, times(1)).stop();
		verify(this.recoveryManagerService, times(1)).destroy();
	}

}
