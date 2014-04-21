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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties to be used in configuring
 * a <A href="http://docs.oracle.com/javaee/6/api/javax/servlet/MultipartConfigElement.html">javax.servlet.MultipartConfigElement</a>.
 * <p/>
 * {@literal multipart.fileSizeThreshold} specifies the size threshold after which files will be written to disk. Default is 0, which means that the file will be written to disk immediately.
 * {@literal multipart.location} specifies the directory where files will be stored. The default is "". A common value is to use the system's temporary directory, which can be obtained
 * {@literal multipart.maxFileSize} specifies the maximum size permitted for uploaded files. The default is unlimited.
 * {@literal multipart.maxRequestSize} specifies the maximum size allowed for {@literal multipart/form-data} requests.
 * <p/>
 * These properties are ultimately passed through {@link org.springframework.boot.context.embedded.MultipartConfigFactory}
 * which means you may specify the values using {@literal long} values or using more readable {@literal String}
 * variants that accept {@literal KB} or {@literal MB} suffixes.
 *
 * @author Josh Long
 */
@ConfigurationProperties(prefix = "multipart", ignoreUnknownFields = false)
public class MultipartProperties {

    private String maxFileSize = "1Mb";

    private String maxRequestSize = "10Mb";

    private String fileSizeThreshold = null;

    private String location = null;

    public String getMaxFileSize() {
        return maxFileSize;
    }

    public String getMaxRequestSize() {
        return maxRequestSize;
    }

    public String getFileSizeThreshold() {
        return fileSizeThreshold;
    }

    public String getLocation() {
        return location;
    }

    public void setMaxFileSize(String maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public void setMaxRequestSize(String maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setFileSizeThreshold(String fileSizeThreshold) {
        this.fileSizeThreshold = fileSizeThreshold;
    }

}

