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
 * @author Josh Long
 */
@ConfigurationProperties(prefix = "multipart", ignoreUnknownFields = false)
public class MultipartProperties {

    private String maxFileSize = "10Mb" ;

    private String maxRequestSize = "100Kb" ;

    private String fileSizeThreshold = null ;

    private String location = null ;

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

