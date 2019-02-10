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

package org.springframework.boot.diagnostics.analyzer;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.util.StringUtils;

/**
 * An {@link AbstractNoUniqueBeanDefinitionFailureAnalyzer} that performs analysis of
 * failures caused by a {@link NoUniqueBeanDefinitionException}.
 *
 * @author Andy Wilkinson
 * @author Dmytro Nosan
 */
class NoUniqueBeanDefinitionFailureAnalyzer extends
		NoUniqueBeanDefinitionFailureAnalyzerSupport<NoUniqueBeanDefinitionException> {

	@Override
	protected Collection<String> getBeanNames(Throwable rootFailure,
			NoUniqueBeanDefinitionException cause) {
		String message = cause.getMessage();
		if (message != null && message.contains("but found")) {
			return Arrays.asList(StringUtils.commaDelimitedListToStringArray(
					message.substring(message.lastIndexOf(':') + 1).trim()));
		}
		return null;
	}

}
