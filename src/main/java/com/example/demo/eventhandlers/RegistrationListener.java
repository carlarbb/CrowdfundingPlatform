package com.example.demo.eventhandlers;

import com.example.demo.classes.UserAccount;
import com.example.demo.events.OnRegistrationCompleteEvent;
import com.example.demo.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;


// Handles the OnRegistrationCompleteEvent
@Component
public class RegistrationListener implements ApplicationListener<OnRegistrationCompleteEvent> {
    @Autowired
    private UserService userService;

    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private MessageSource messageSource;

    @Async
    @Override
    public void onApplicationEvent(OnRegistrationCompleteEvent event) {
        this.confirmRegistration(event);
    }

    //primeste OnRegistrationCompleteEvent, extrage informatiile despre User, ii adauga un token de verificare si trimite mail
    //erorile sunt tratate de LoginController in metoda createNewUser()

    private void confirmRegistration(OnRegistrationCompleteEvent event) {
        UserAccount user = event.getUser();
        //immutable universally unique identifier (UUID)
        String token = UUID.randomUUID().toString();
        user.setTokenId(token);

        //calculeaza tokenExpiryDate din clasa UserAccount
        final int EXPIRATION = 60 * 24;
        Calendar cal = Calendar.getInstance();
        Date date = calculateExpiryDate(EXPIRATION, cal);
        user.setTokenExpiryDate(date);

        userService.updateUser(user);

        String recipientAddress = user.getEmail();
        String subject = messageSource.getMessage("auth.message.subjectForRegistrationConfirm", null, event.getLocale());

        String registrationConfirmLink = messageSource.getMessage("auth.message.registrationConfirmLink", null, event.getLocale());
        String confirmationUrl = event.getAppUrl() + registrationConfirmLink + token;
        String message = messageSource.getMessage("auth.message.confirmRegistration", null, event.getLocale());

        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipientAddress);
        email.setSubject(subject);
        email.setText(message + "http://localhost:8080" + confirmationUrl);
        mailSender.send(email);
    }

    private Date calculateExpiryDate(int expiryTimeInMinutes, Calendar cal) {
        cal.setTime(new java.sql.Timestamp(cal.getTime().getTime()));
        cal.add(Calendar.MINUTE, expiryTimeInMinutes);
        return new Date(cal.getTime().getTime());
    }
}
