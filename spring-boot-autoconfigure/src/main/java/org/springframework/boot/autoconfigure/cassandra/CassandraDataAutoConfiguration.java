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

package org.springframework.boot.autoconfigure.cassandra;

import com.datastax.driver.core.Cluster;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.CassandraEntityClassScanner;
import org.springframework.data.cassandra.config.CassandraSessionFactoryBean;
import org.springframework.data.cassandra.convert.CassandraConverter;
import org.springframework.data.cassandra.convert.MappingCassandraConverter;
import org.springframework.data.cassandra.core.CassandraAdminOperations;
import org.springframework.data.cassandra.core.CassandraAdminTemplate;
import org.springframework.data.cassandra.mapping.BasicCassandraMappingContext;
import org.springframework.data.cassandra.mapping.CassandraMappingContext;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration} for Spring Data's Cassandra support.
 * <p/>
 * Registers a {@link org.springframework.data.cassandra.config.CassandraSessionFactoryBean} a {@link org.springframework.data.cassandra.core.CassandraAdminOperations} a {@link org.springframework.data.cassandra.mapping.CassandraMappingContext} and a
 * {@link org.springframework.data.cassandra.convert.CassandraConverter} beans if no other beans of the same type are configured.
 * <p/>
 *
 * @author Julien Dubois
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass({Cluster.class, CassandraAdminOperations.class})
@EnableConfigurationProperties(CassandraProperties.class)
@AutoConfigureAfter(CassandraAutoConfiguration.class)
public class CassandraDataAutoConfiguration {

    @Autowired
    BeanFactory beanFactory;

    @Autowired
    private CassandraProperties properties;

    @Autowired
    private Cluster cluster;

    @Bean
    @ConditionalOnMissingBean
    public CassandraSessionFactoryBean session() throws Exception {
        CassandraSessionFactoryBean session = new CassandraSessionFactoryBean();
        session.setCluster(this.cluster);
        session.setConverter(cassandraConverter());
        session.setKeyspaceName(properties.getKeyspaceName());
        return session;
    }

    @Bean
    @ConditionalOnMissingBean
    public CassandraAdminOperations cassandraTemplate() throws Exception {
        return new CassandraAdminTemplate(session().getObject(), cassandraConverter());
    }

    @Bean
    @ConditionalOnMissingBean
    public CassandraMappingContext cassandraMapping() throws ClassNotFoundException {
        BasicCassandraMappingContext bean = new BasicCassandraMappingContext();
        bean.setInitialEntitySet(CassandraEntityClassScanner.scan(AutoConfigurationPackages.get(beanFactory)));
        bean.setBeanClassLoader(beanFactory.getClass().getClassLoader());
        return bean;
    }

    @Bean
    @ConditionalOnMissingBean
    public CassandraConverter cassandraConverter() throws Exception {
        return new MappingCassandraConverter(cassandraMapping());
    }
}
