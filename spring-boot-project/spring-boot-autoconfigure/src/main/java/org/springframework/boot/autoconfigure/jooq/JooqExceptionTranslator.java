/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.jooq;

import java.sql.SQLException;

import org.apache.commons.logging.LogFactory;
import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;

import org.springframework.dao.DataAccessException;

/**
 * Transforms {@link SQLException} into a Spring-specific {@link DataAccessException}.
 *
 * @author Lukas Eder
 * @author Andreas Ahlenstorf
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 1.5.10
 * @deprecated since 3.3.0 for removal in 3.5.0 in favor of
 * {@link ExceptionTranslatorExecuteListener#DEFAULT} or
 * {@link ExceptionTranslatorExecuteListener#of}
 */
@Deprecated(since = "3.3.0", forRemoval = true)
public class JooqExceptionTranslator implements ExecuteListener {

	private final DefaultExceptionTranslatorExecuteListener delegate = new DefaultExceptionTranslatorExecuteListener(
			LogFactory.getLog(JooqExceptionTranslator.class));

	@Override
	public void exception(ExecuteContext context) {
		this.delegate.exception(context);
	}

}
