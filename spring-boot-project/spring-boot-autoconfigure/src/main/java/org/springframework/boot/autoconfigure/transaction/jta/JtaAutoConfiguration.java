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

package org.springframework.boot.autoconfigure.transaction.jta;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.XADataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for JTA.
 *
 * @author Josh Long
 * @author Phillip Webb
 * @author Nishant Raut
 * @since 1.2.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(javax.transaction.Transaction.class)
@ConditionalOnProperty(prefix = "spring.jta", value = "enabled", matchIfMissing = true)
@AutoConfigureBefore({ XADataSourceAutoConfiguration.class, ActiveMQAutoConfiguration.class,
		ArtemisAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
@Import({ JndiJtaConfiguration.class, BitronixJtaConfiguration.class, AtomikosJtaConfiguration.class })
public class JtaAutoConfiguration {

}
