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

package org.springframework.boot.autoconfigure.jdbc;

import javax.sql.DataSource;

import com.p6spy.engine.spy.P6DataSource;
import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSource;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Helper class that provides static methods for {@link DataSource}s wrapped in a known proxy {@link DataSource}
 * providers.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.5.3
 */
public final class ProxyDataSourceUtil {

	private ProxyDataSourceUtil() {
	}

	/**
	 * Tries to fidn real {@link DataSource} from the known proxy {@link DataSource}
	 * providers, returns input {@link DataSource} if it is not proxy of known provider.
	 *
	 * @param dataSource input {@link DataSource}
	 * @return real {@link DataSource} or input {@link DataSource}
	 */
	public static DataSource tryFindRealDataSource(DataSource dataSource) {
		Assert.notNull(dataSource, "data source is null");
		ClassLoader classLoader = dataSource.getClass().getClassLoader();
		return getRealDataSourceRecursively(dataSource, classLoader);
	}


	private static DataSource getRealDataSourceRecursively(DataSource proxyDataSource, ClassLoader classLoader) {
		DataSource dataSource = null;
		Class<? extends DataSource> proxyDataSourceClass = proxyDataSource.getClass();
		if (ClassUtils.isPresent("com.p6spy.engine.spy.P6DataSource", classLoader)
				&& proxyDataSource instanceof P6DataSource) {
			dataSource = (DataSource) new DirectFieldAccessor(proxyDataSource)
					.getPropertyValue("realDataSource");
		}
		if (ClassUtils.isPresent("net.ttddyy.dsproxy.support.ProxyDataSource", classLoader)
				&& proxyDataSource instanceof ProxyDataSource) {
			dataSource = (DataSource) new DirectFieldAccessor(proxyDataSource)
					.getPropertyValue("dataSource");
		}
		if (ClassUtils.isPresent("com.vladmihalcea.flexypool.FlexyPoolDataSource", classLoader)
				&& proxyDataSource instanceof FlexyPoolDataSource) {
			dataSource = (DataSource) new DirectFieldAccessor(proxyDataSource)
					.getPropertyValue("targetDataSource");
		}
		if (dataSource != null) {
			return getRealDataSourceRecursively(dataSource, classLoader);
		}
		return proxyDataSource;
	}
}
