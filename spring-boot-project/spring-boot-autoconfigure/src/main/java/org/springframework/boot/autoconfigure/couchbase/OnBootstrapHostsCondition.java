/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.couchbase;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.OnPropertyListCondition;

/**
 * Condition to determine if {@code spring.couchbase.bootstrap-hosts} is specified.
 *
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author Eneias Silva
 */
class OnBootstrapHostsCondition extends OnPropertyListCondition {

	OnBootstrapHostsCondition() {
		super("spring.couchbase.bootstrap-hosts", () -> ConditionMessage.forCondition("Couchbase Bootstrap Hosts"));
	}

}
