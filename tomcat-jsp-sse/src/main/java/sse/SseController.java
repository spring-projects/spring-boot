package sse;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
public class SseController {

    /** Logger. */
    private static final Logger log = Logger.getLogger(SseController.class);
    /** List of all emitters connected to the service. */
    private final List<SseEmitter> sseEmitter = new LinkedList<>();
    /** Temporary state for demonstration. */
    private State state = new State("state 0");
    /** Counter for state changes. */
    private int counter = 1;

    private final Runnable changeState = () -> {
        log.info("Sending auto message " + counter);
        state = new State("state " + counter++);

        synchronized (sseEmitter) {
            sseEmitter.forEach((SseEmitter emitter) -> {
                try {
                    emitter.send(state, MediaType.APPLICATION_JSON);
                } catch (IOException e) {
                    emitter.complete();
                    sseEmitter.remove(emitter);
                }
            });
        }
    };

    /**
     * Initialize the controller and start a repeated task to model state changes.
     */
    public SseController() {
        ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(4);
        scheduledPool.scheduleWithFixedDelay(changeState, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Some website.
     * @param model model in spring mvc
     * @return a .jsp ressource
     */
    @RequestMapping( "/" )
    public String sseExamplePage(Map<String, Object> model) {
        model.put("state", state);
        return "server-update";
    }

    /**
     * Viewer can register here to get sse messages.
     * @return an server state event emitter
     * @throws IOException if registering the new emitter fails
     */
    @RequestMapping (path = "/register", method = RequestMethod.GET)
    public SseEmitter register() throws IOException {
        log.info("Registering a stream.");

        SseEmitter emitter = new SseEmitter();

        synchronized (sseEmitter) {
            sseEmitter.add(emitter);
        }
        emitter.onCompletion(() -> sseEmitter.remove(emitter));

        return emitter;
    }
}
