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

import javax.sql.DataSource;
import javax.sql.XADataSource;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import org.springframework.boot.jta.XADataSourceWrapper;

/**
 * {@link XADataSourceWrapper} that uses {@link NarayanaDataSourceBean} to wrap an {@link XADataSource}.
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class NarayanaXADataSourceWrapper implements XADataSourceWrapper {

	private final NarayanaRecoveryManagerBean narayanaRecoveryManagerBean;

	private final NarayanaProperties narayanaProperties;

	public NarayanaXADataSourceWrapper(NarayanaRecoveryManagerBean narayanaRecoveryManagerBean,
			NarayanaProperties narayanaProperties) {
		this.narayanaRecoveryManagerBean = narayanaRecoveryManagerBean;
		this.narayanaProperties = narayanaProperties;
	}

	@Override
	public DataSource wrapDataSource(XADataSource dataSource) {
		this.narayanaRecoveryManagerBean.registerXAResourceRecoveryHelper(getRecoveryHelper(dataSource));

		return new NarayanaDataSourceBean(dataSource);
	}

	private XAResourceRecoveryHelper getRecoveryHelper(XADataSource dataSource) {
		if (this.narayanaProperties.getRecoveryDbUser() == null && this.narayanaProperties.getRecoveryDbPass() == null) {
			return new DataSourceXAResourceRecoveryHelper(dataSource);
		}

		return new DataSourceXAResourceRecoveryHelper(dataSource, this.narayanaProperties.getRecoveryDbUser(),
				this.narayanaProperties.getRecoveryDbPass());
	}

}
