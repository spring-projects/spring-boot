/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.mobile;

import org.springframework.core.Ordered;
import org.springframework.mobile.device.view.LiteDeviceDelegatingViewResolver;
import org.springframework.web.servlet.ViewResolver;

/**
 * A factory for {@link LiteDeviceDelegatingViewResolver} that applies customizations of
 * {@link DeviceDelegatingViewResolverProperties}.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public class DeviceDelegatingViewResolverFactory {

	private final DeviceDelegatingViewResolverProperties properties;

	public DeviceDelegatingViewResolverFactory(
			DeviceDelegatingViewResolverProperties properties) {
		this.properties = properties;
	}

	/**
	 * Create a {@link LiteDeviceDelegatingViewResolver} delegating to the specified
	 * {@link ViewResolver}.
	 * @param delegate the view resolver to delegate to
	 * @param delegatingOrder the order of the {@link LiteDeviceDelegatingViewResolver}
	 * @return a {@link LiteDeviceDelegatingViewResolver} handling the specified resolver
	 */
	public LiteDeviceDelegatingViewResolver createViewResolver(ViewResolver delegate,
			int delegatingOrder) {
		LiteDeviceDelegatingViewResolver resolver = new LiteDeviceDelegatingViewResolver(
				delegate);
		resolver.setEnableFallback(this.properties.isEnableFallback());
		resolver.setNormalPrefix(this.properties.getNormalPrefix());
		resolver.setNormalSuffix(this.properties.getNormalSuffix());
		resolver.setMobilePrefix(this.properties.getMobilePrefix());
		resolver.setMobileSuffix(this.properties.getMobileSuffix());
		resolver.setTabletPrefix(this.properties.getTabletPrefix());
		resolver.setTabletSuffix(this.properties.getTabletSuffix());
		resolver.setOrder(delegatingOrder);
		return resolver;
	}

	/**
	 * Create a {@link LiteDeviceDelegatingViewResolver} delegating to the specified
	 * {@link ViewResolver} and computing a sensible order for it. The specified
	 * {@link ViewResolver} should implement {@link Ordered}, consider using
	 * {@link #createViewResolver(ViewResolver, int)} if that's not the case.
	 * @param delegate the view resolver to delegate to
	 * @return a {@link LiteDeviceDelegatingViewResolver} handling the specified resolver
	 */
	public LiteDeviceDelegatingViewResolver createViewResolver(ViewResolver delegate) {
		if (!(delegate instanceof Ordered)) {
			throw new IllegalStateException("ViewResolver " + delegate
					+ " should implement " + Ordered.class.getName());
		}
		int delegateOrder = ((Ordered) delegate).getOrder();
		return createViewResolver(delegate, adjustOrder(delegateOrder));
	}

	private int adjustOrder(int order) {
		if (order == Ordered.HIGHEST_PRECEDENCE) {
			return Ordered.HIGHEST_PRECEDENCE;
		}
		// The view resolver must be ordered higher than the delegate view
		// resolver, otherwise the view names will not be adjusted
		return order - 1;
	}

}
