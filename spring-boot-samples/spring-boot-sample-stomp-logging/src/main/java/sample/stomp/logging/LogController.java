/*
 * Copyright 2012-2015 the original author or authors.
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

package sample.stomp.logging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController("/log")
public class LogController {

    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @RequestMapping(method = RequestMethod.POST)
    public void log(@RequestParam(value = "logger", required = false) String loggerName, @RequestParam(value = "level", defaultValue = "info") String levelName,
             @RequestParam(value = "msg", defaultValue = "log message") String msg, @RequestParam(value = "error", required = false) Boolean error) {
        LogLevel level = LogLevel.valueOf(levelName.toUpperCase());
        Throwable ex = null;
        if ((error == null && level.compareTo(LogLevel.WARN) >= 0) || Boolean.TRUE.equals(error)) {
            ex = new Exception("Expected exception.");
        }
        Log log = StringUtils.isEmpty(loggerName) ? LogFactory.getLog(LogController.class) : LogFactory.getLog(loggerName);
        switch (level) {
            case TRACE :
                log.trace(msg, ex);
                break;
            case DEBUG :
                log.debug(msg, ex);
                break;
            case INFO :
                log.info(msg, ex);
                break;
            case WARN :
                log.warn(msg, ex);
                break;
            case ERROR :
                log.error(msg, ex);
            case FATAL :
                log.fatal(msg, ex);
                break;
            default:
                throw new IllegalArgumentException("unsupported level: " + level);
        }
    }
}
