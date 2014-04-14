/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.MultiPartConfigFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for multi-part uploads. Adds a
 * {@link StandardServletMultipartResolver} when a {@link MultipartConfigElement} bean is not otherwise
 * defined. The {@link EmbeddedWebApplicationContext} will associate the
 * {@link MultipartConfigElement} bean to any {@link Servlet} beans.
 * <p/>
 * Adds a {@link javax.servlet.MultipartConfigElement } defaulted to {@literal 10Mb}.
 *
 * @author Greg Turnquist
 * @author Josh Long
 */
@Configuration
@ConditionalOnClass({Servlet.class, StandardServletMultipartResolver.class})
@EnableConfigurationProperties(MultipartProperties.class)
public class MultipartAutoConfiguration {

    @Autowired
    private MultipartProperties multipartProperties = new MultipartProperties();

    @Bean
    @ConditionalOnMissingBean
    public MultipartConfigElement multipartConfigElement() {
        MultiPartConfigFactory factory = new MultiPartConfigFactory();

        if (StringUtils.hasText(this.multipartProperties.getFileSizeThreshold())) {
            factory.setFileSizeThreshold(this.multipartProperties.getFileSizeThreshold());
        }

        if (StringUtils.hasText(this.multipartProperties.getLocation())) {
            factory.setLocation(this.multipartProperties.getLocation());
        }

        if (StringUtils.hasText(this.multipartProperties.getMaxRequestSize())) {
            factory.setMaxRequestSize(this.multipartProperties.getMaxRequestSize());
        }

        if (StringUtils.hasText(this.multipartProperties.getMaxFileSize())) {
            factory.setMaxFileSize(this.multipartProperties.getMaxFileSize());
        }

        return factory.createMultipartConfig();
    }


    @Bean
    @ConditionalOnMissingBean
    public StandardServletMultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
}
