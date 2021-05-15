/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.jdbc;

import java.sql.Wrapper;

import javax.sql.DataSource;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.util.ClassUtils;

/**
 * Unwraps a {@link DataSource} that may have been proxied or wrapped in a custom
 * {@link Wrapper} such as {@link DelegatingDataSource}.
 *
 * @author Tadaya Tsuyukubo
 * @author Stephane Nicoll
 * @since 2.0.7
 */
public final class DataSourceUnwrapper {

	private static final boolean DELEGATING_DATA_SOURCE_PRESENT = ClassUtils.isPresent(
			"org.springframework.jdbc.datasource.DelegatingDataSource", DataSourceUnwrapper.class.getClassLoader());

	private DataSourceUnwrapper() {
	}

	/**
	 * Return an object that implements the given {@code target} type, unwrapping delegate
	 * or proxy if necessary using the specified {@code unwrapInterface}.
	 * @param dataSource the datasource to handle
	 * @param unwrapInterface the interface that the target type must implement
	 * @param target the type that the result must implement
	 * @param <I> the interface that the target type must implement
	 * @param <T> the target type
	 * @return an object that implements the target type or {@code null}
	 * @since 2.3.8
	 * @see Wrapper#unwrap(Class)
	 */
	public static <I, T extends I> T unwrap(DataSource dataSource, Class<I> unwrapInterface, Class<T> target) {
		if (target.isInstance(dataSource)) {
			return target.cast(dataSource);
		}
		I unwrapped = safeUnwrap(dataSource, unwrapInterface);
		if (unwrapped != null && unwrapInterface.isAssignableFrom(target)) {
			return target.cast(unwrapped);
		}
		if (DELEGATING_DATA_SOURCE_PRESENT) {
			DataSource targetDataSource = DelegatingDataSourceUnwrapper.getTargetDataSource(dataSource);
			if (targetDataSource != null) {
				return unwrap(targetDataSource, unwrapInterface, target);
			}
		}
		if (AopUtils.isAopProxy(dataSource)) {
			Object proxyTarget = AopProxyUtils.getSingletonTarget(dataSource);
			if (proxyTarget instanceof DataSource) {
				return unwrap((DataSource) proxyTarget, unwrapInterface, target);
			}
		}
		return null;
	}

	/**
	 * Return an object that implements the given {@code target} type, unwrapping delegate
	 * or proxy if necessary. Consider using {@link #unwrap(DataSource, Class, Class)} as
	 * {@link Wrapper#unwrap(Class) unwrapping} won't be considered if {@code target} is
	 * not an interface.
	 * @param dataSource the datasource to handle
	 * @param target the type that the result must implement
	 * @param <T> the target type
	 * @return an object that implements the target type or {@code null}
	 */
	public static <T> T unwrap(DataSource dataSource, Class<T> target) {
		return unwrap(dataSource, target, target);
	}

	private static <S> S safeUnwrap(Wrapper wrapper, Class<S> target) {
		try {
			if (target.isInterface() && wrapper.isWrapperFor(target)) {
				return wrapper.unwrap(target);
			}
		}
		catch (Exception ex) {
			// Continue
		}
		return null;
	}

	private static class DelegatingDataSourceUnwrapper {

		private static DataSource getTargetDataSource(DataSource dataSource) {
			if (dataSource instanceof DelegatingDataSource) {
				return ((DelegatingDataSource) dataSource).getTargetDataSource();
			}
			return null;
		}

	}

}
