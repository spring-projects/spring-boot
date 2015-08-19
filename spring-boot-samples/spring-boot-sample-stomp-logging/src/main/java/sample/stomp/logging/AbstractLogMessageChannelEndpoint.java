package sample.stomp.logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.fusesource.jansi.HtmlAnsiOutputStream;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class AbstractLogMessageChannelEndpoint implements InitializingBean, DisposableBean {

    @Autowired(required = false)
    private SimpMessagingTemplate brokerMessagingTemplate;

    private ThreadPoolTaskExecutor executor;

    @Override
    public void afterPropertiesSet() throws Exception {
        executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(1);
        executor.afterPropertiesSet();
    }

    @Override
    public void destroy() throws Exception {
        executor.destroy();
    }

    private static String formatToHtml(byte[] payload) throws MessagingException {
        try {
            ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
            HtmlAnsiOutputStream ansiStream = new HtmlAnsiOutputStream(byteArrayStream);
            try {
                ansiStream.write(payload);
            } finally {
                ansiStream.close();
            }
            return new String(byteArrayStream.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new MessagingException("Error formatting " + payload + " to html.", ex);
        }
    }

    protected class LogLineMessageHandler implements MessageHandler {
        private final String channelName;

        public LogLineMessageHandler(String channelName) {
            this.channelName = channelName;
        }

        @Override
        public final void handleMessage(final Message<?> message) throws MessagingException {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    send(message);
                }
            });
        }

        protected void send(final Message<?> message) {
            String destination = "/topic/log/" + channelName;
            String payload = formatToHtml((byte[]) message.getPayload());
            brokerMessagingTemplate.convertAndSend(destination, payload);
        }
    }

}
