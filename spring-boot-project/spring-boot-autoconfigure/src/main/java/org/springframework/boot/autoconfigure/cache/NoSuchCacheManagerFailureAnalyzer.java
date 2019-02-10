/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.cache;

import java.util.Collection;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.diagnostics.analyzer.NoSuchBeanDefinitionFailureAnalyzerSupport;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.analyzer.NoUniqueBeanDefinitionFailureAnalyzerSupport;
import org.springframework.cache.CacheManager;

/**
 * An {@link AbstractFailureAnalyzer} for
 * {@link CacheAutoConfiguration.NoSuchCacheManagerException}.
 *
 * @author Dmytro Nosan
 */
class NoSuchCacheManagerFailureAnalyzer extends
		NoSuchBeanDefinitionFailureAnalyzerSupport<CacheAutoConfiguration.NoSuchCacheManagerException> {

	private final NoUniqueCacheManagerFailureAnalyzer analyzer = new NoUniqueCacheManagerFailureAnalyzer();

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		super.setBeanFactory(beanFactory);
		this.analyzer.setBeanFactory(beanFactory);
	}

	@Override
	public FailureAnalysis analyze(Throwable failure) {
		FailureAnalysis analyze = this.analyzer.analyze(failure);
		if (analyze != null) {
			return analyze;
		}
		return super.analyze(failure);
	}

	@Override
	protected BeanMetadata getBeanMetadata(Throwable rootFailure,
			CacheAutoConfiguration.NoSuchCacheManagerException cause) {
		return new BeanMetadata(CacheManager.class);
	}

	private static final class NoUniqueCacheManagerFailureAnalyzer extends
			NoUniqueBeanDefinitionFailureAnalyzerSupport<CacheAutoConfiguration.NoUniqueCacheManagerException> {

		@Override
		protected Collection<String> getBeanNames(Throwable rootFailure,
				CacheAutoConfiguration.NoUniqueCacheManagerException cause) {
			return cause.getBeanNames();
		}

	}

}
