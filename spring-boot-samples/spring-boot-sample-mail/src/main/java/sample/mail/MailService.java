package sample.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

@Component
public class MailService {

    private final MailSender mailSender;

    @Autowired
    public MailService(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void processMessage(Message message) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(message.getTo());
        msg.setText(message.getText());
        try{
            this.mailSender.send(msg);
        }
        catch (MailException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
