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

package org.springframework.boot.autoconfigure.orm.jpa;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jta.JtaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.HibernateEntityManagerCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.ClassUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Hibernate JPA.
 *
 * @author Phillip Webb
 * @author Josh Long
 */
@Configuration
@ConditionalOnClass({LocalContainerEntityManagerFactoryBean.class,
        EnableTransactionManagement.class, EntityManager.class})
@Conditional(HibernateEntityManagerCondition.class)
@AutoConfigureAfter({DataSourceAutoConfiguration.class, JtaAutoConfiguration.class})
public class HibernateJpaAutoConfiguration extends JpaBaseConfiguration {

    private static final String JTA_PLATFORM = "hibernate.transaction.jta.platform";

    private static final String HIBERNATE43_NOJTAPLATFORM_CLASS = "org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform";
    private static final String HIBERNATE42_NOJTAPLATFORM_CLASS = "org.hibernate.service.jta.platform.internal.NoJtaPlatform";
    private static final String SPRING_JTA_PLATFORM_CLASS = "org.springframework.boot.orm.jpa.hibernate.SpringJtaPlatform";

    private static final Logger logger = LoggerFactory.getLogger(HibernateJpaAutoConfiguration.class);

    @Autowired
    private JpaProperties properties;

    @Autowired
    private DataSource dataSource;

    @Override
    protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
        return new HibernateJpaVendorAdapter();
    }

    @Override
    protected Map<String, Object> getVendorProperties() {
        Map<String, Object> vendorProperties = new LinkedHashMap<String, Object>();
        vendorProperties.putAll(this.properties.getHibernateProperties(this.dataSource));
        return vendorProperties;
    }

    @Override
    protected void customizeVendorProperties(Map<String, Object> vendorProperties) {
        super.customizeVendorProperties(vendorProperties);
        if (!vendorProperties.containsKey(JTA_PLATFORM)) {
            JtaTransactionManager jtaTransactionManager = getJtaTransactionManager();
            try {
                if (jtaTransactionManager != null) {
                    Object springJtaPlatform;
                    Class<?> springJtaPlatformClass = Class.forName(SPRING_JTA_PLATFORM_CLASS);
                    Constructor<?> constructor = springJtaPlatformClass.getDeclaredConstructor(JtaTransactionManager.class);
                    springJtaPlatform = constructor.newInstance(jtaTransactionManager);

                    vendorProperties.put(JTA_PLATFORM, springJtaPlatform);
                } else {
                    Class<?> noJtaPlatformClass;
                    try {
                        noJtaPlatformClass = Class.forName(HIBERNATE43_NOJTAPLATFORM_CLASS);
                    } catch (ClassNotFoundException e) {
                        // No Hibernate 4.3 found, try with 4.2 classes
                        noJtaPlatformClass = Class.forName(HIBERNATE42_NOJTAPLATFORM_CLASS);
                    }
                    if (noJtaPlatformClass != null) {
                        vendorProperties.put(JTA_PLATFORM, noJtaPlatformClass.newInstance());
                    }
                }
            } catch (Exception e) {
                logger.error("Could not configure the JTA platform", e);
            }
        }
    }

    static class HibernateEntityManagerCondition extends SpringBootCondition {

        private static String[] CLASS_NAMES = {
                "org.hibernate.ejb.HibernateEntityManager",
                "org.hibernate.jpa.HibernateEntityManager"};

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context,
                                                AnnotatedTypeMetadata metadata) {
            for (String className : CLASS_NAMES) {
                if (ClassUtils.isPresent(className, context.getClassLoader())) {
                    return ConditionOutcome.match("found HibernateEntityManager class");
                }
            }
            return ConditionOutcome.noMatch("did not find HibernateEntityManager class");
        }
    }

}
