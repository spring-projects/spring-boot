package de.nerdpause.demo.multistaterequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * Simple {@link Controller} for the demo's web frontend. Displays the main page '/' as Thymeleaf
 * and provides two endpoints '/start' and '/status' to start a long request with multiple states
 * and to ask for its status, respectively.
 *
 * @author Jan-Philipp Kappmeier
 */
@Controller
public class MultiStateRequestController {

    /**
     * The asynchronous service performing the long task on request in a different thread.
     */
    @Autowired
    MultiStateRequestAsyncService service;

    /**
     * Displays the Thymeleaf demo page.
     *
     * @return the view
     */
    @RequestMapping("/")
    String home() {
        return "multiStateRequestPage";
    }

    /**
     * Starts a request which may take longer and returns the first status update.
     * 
     * @return the first status update, when it occurres
     */
    @RequestMapping(name = "/start", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<String> startRequest() {
        // Prepare already for the first state change
        DeferredResult<String> result = service.getStatus();

        // Actually let the asynchronous service do something
        service.doSomething();

        // Return the deferred result that will be set in the above asynchronous call
        return result;
    }

    /**
     * Query the controller for a status update. The status update will be sent using a deferred
     * result. Whenever a status is returned the caller can immediately poll again for the next
     * status update.
     *
     * @return the current status of the request, whenever it is changed
     */
    @RequestMapping(name = "/status", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<String> requestStatus() {
        return service.getStatus();
    }
}
