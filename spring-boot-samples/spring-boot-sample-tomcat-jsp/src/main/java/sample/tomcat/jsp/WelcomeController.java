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
package sample.tomcat.jsp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;

import org.apache.log4j.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
public class WelcomeController {

    private static final Logger log = Logger.getLogger(WelcomeController.class);

    private final List<SseEmitter> emitters = new ArrayList<>();

    public WelcomeController() {
        (new Thread(new Runnable() {

            @Override
            public void run() {
                int i = 0;
                while(true) {
                    try {
                        Thread.sleep(10 * 1000);
                    } catch (InterruptedException ex) {
                        log.warn("Exception");
                    }
                    log.info("Sending auto message" + i++);

                    Message message = new Message("bot", "auto generated message " + i);
                    
                    emitters.forEach((SseEmitter emitter) -> {
                        try {
                            emitter.send(message, MediaType.APPLICATION_JSON);
                        } catch (IOException e) {
                            emitter.complete();
                            emitters.remove(emitter);
                            e.printStackTrace();
                        }
                    });
                    
                    
                }
                
            }

        })).start();
    }

    @Value( "${application.message:Hello World}" )
    private String message = "Hello World";

    @RequestMapping( "/" )
    public String welcome(Map<String, Object> model) {
        model.put("time", new Date());
        model.put("message", this.message);
        return "welcome";
    }

    @RequestMapping( "/fail" )
    public String fail() {
        throw new MyException("Oh dear!");
    }

    @RequestMapping( "/fail2" )
    public String fail2() {
        throw new IllegalStateException();
    }

    @ExceptionHandler( MyException.class )
    @ResponseStatus( HttpStatus.BAD_REQUEST )
    public @ResponseBody
    MyRestResponse handleMyRuntimeException(MyException exception) {
        return new MyRestResponse("Some data I want to send back to the client.");
    }

    @RequestMapping( path = "/stream", method = RequestMethod.GET )
    public SseEmitter stream() throws IOException {
        log.info("Registering a stream.");

        SseEmitter emitter = new SseEmitter();

        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));

        return emitter;
    }

    @ResponseBody
    @RequestMapping( path = "/chat", method = RequestMethod.POST )
    public Message sendMessage(@Valid Message message) {

        log.info("Got message" + message);

        emitters.forEach((SseEmitter emitter) -> {
            try {
                emitter.send(message, MediaType.APPLICATION_JSON);
            } catch (IOException e) {
                emitter.complete();
                emitters.remove(emitter);
                e.printStackTrace();
            }
        });
        return message;
    }

}
