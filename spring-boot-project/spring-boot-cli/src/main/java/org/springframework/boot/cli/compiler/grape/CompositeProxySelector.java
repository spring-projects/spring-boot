/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.cli.compiler.grape;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Composite {@link ProxySelector}.
 *
 * @author Dave Syer
 * @since 1.1.0
 */
public class CompositeProxySelector implements ProxySelector {

	private List<ProxySelector> selectors = new ArrayList<>();

	public CompositeProxySelector(List<ProxySelector> selectors) {
		this.selectors = selectors;
	}

	@Override
	public Proxy getProxy(RemoteRepository repository) {
		for (ProxySelector selector : this.selectors) {
			Proxy proxy = selector.getProxy(repository);
			if (proxy != null) {
				return proxy;
			}
		}
		return null;
	}

}
