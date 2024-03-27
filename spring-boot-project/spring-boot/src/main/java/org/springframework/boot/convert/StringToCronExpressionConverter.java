/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.convert;

import org.springframework.core.convert.converter.Converter;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.util.StringUtils;

/**
 * {@link Converter} to convert from a {@link String} to a {@link CronExpression}.
 *
 * @author Ahmad Amiri
 */
class StringToCronExpressionConverter implements Converter<String, CronExpression> {

	@Override
	public CronExpression convert(String source) {
		if (StringUtils.hasLength(source)) {
			return CronExpression.parse(source);
		}
		return null;
	}

}
