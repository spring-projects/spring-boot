package de.nerdpause.demo.multistaterequest;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * Very simple asynchronous service performing a step in two phases. The phases are faked by sending
 * the {@link Thread} to sleep. Whenever a phase is done, a listener for a response is notified, if
 * such is present.
 * 
 * Observe that the service can only handle a single request at any time.
 *
 * @author Jan-Philipp Kappmeier
 */
@Service
public class MultiStateRequestAsyncService {

    /**
     * The states of the request.
     */
    private enum LongRequestStatus {
        INITIALIZING, PHASE1, DONE;
    }

    /**
     * Holder of the status response, can be {@literal null}.
     */
    private DeferredResult<String> waitingResponse = null;
    private LongRequestStatus currentStatus = LongRequestStatus.INITIALIZING;

    /**
     * Performs a task that takes several seconds in a different thread.
     */
    @Async
    void doSomething() {
        // Initialize the current status and notify any listener that is already present
        currentStatus = LongRequestStatus.INITIALIZING;
        publishStatus(currentStatus);

        // Perform some work until the final state is reached.
        do {
            currentStatus = doNecessaryWork(currentStatus);
            publishStatus(currentStatus);
        } while (currentStatus != LongRequestStatus.DONE);
    }

    /**
     * Sends the {@link DeferredResult} with the current state if a listener for an answer is
     * present.
     *
     * @param nextToReturn the state that is to be returned
     */
    private synchronized void publishStatus(LongRequestStatus nextToReturn) {
        if (waitingResponse != null) {
            waitingResponse.setResult(nextToReturn.toString());
        }
        waitingResponse = null;
    }

    /**
     * Sleeps the current thread for a few seconds and returns the next status.
     *
     * @param status the current status
     * @return the next status
     */
    private LongRequestStatus doNecessaryWork(LongRequestStatus status) {
        try {
            Thread.sleep(4000);
        } catch (InterruptedException ex) {
            Logger.getLogger(MultiStateRequestAsyncService.class.getName()).log(Level.SEVERE, "Failed in waiting period", ex);
        }
        return status == LongRequestStatus.INITIALIZING ? LongRequestStatus.PHASE1 : LongRequestStatus.DONE;
    }

    /**
     * Returns a {@link DeferredResult} that will contain the next status. It will be actually sent
     * when the status changes the next time.
     *
     * @return the status
     */
    public synchronized DeferredResult<String> getStatus() {
        waitingResponse = new DeferredResult<>();

        // If the request completed, immediately return the status because it will never change
        if (currentStatus == LongRequestStatus.DONE) {
            waitingResponse.setResult(currentStatus.toString());
        }

        return waitingResponse;
    }
}
