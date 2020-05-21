package com.example.demo.controllers;

import com.example.demo.classes.UserAccount;
import com.example.demo.events.OnRegistrationCompleteEvent;
import com.example.demo.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import java.util.Calendar;
import java.util.Locale;

@Controller
public class LoginController {
    @Autowired
    private UserService userService;
    @Autowired
    ApplicationEventPublisher eventPublisher;
    @Autowired
    private MessageSource messageSource;

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ModelAndView login() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("login");
        return modelAndView;
    }

    @RequestMapping(value = "/signup", method = RequestMethod.GET)
    public ModelAndView signup() {
        ModelAndView modelAndView = new ModelAndView();
        UserAccount user = new UserAccount();
        modelAndView.addObject("user", user);
        modelAndView.setViewName("signup");
        return modelAndView;
    }

    @RequestMapping(value = "/signup", method = RequestMethod.POST)
    public ModelAndView createNewUser(@Valid UserAccount user, BindingResult bindingResult, WebRequest request) {
        ModelAndView modelAndView = new ModelAndView();
        UserAccount userExists = userService.getByEmail(user.getEmail());
        if (userExists != null) {
            bindingResult.rejectValue("email", "error.user",
                            "There is already an user registered with the username provided!");
            modelAndView.addObject("errorMessage", "There is already an user registered with the username provided!");
        }
        if (bindingResult.hasErrors()) {
            /*for (Object object : bindingResult.getAllErrors()) {
                if (object instanceof FieldError) {
                    FieldError fieldError = (FieldError) object;
                    System.out.println(fieldError.getDefaultMessage());
                }
            }

            modelAndView.addObject("errorMessages", bindingResult.getAllErrors()); */
            modelAndView.setViewName("signup");
        } else {
            userService.insertUser(user);
            try {
                String appUrl = request.getContextPath();

                //Controllerul publica un Spring ApplicationEvent pentru a declansa executia urmatoarelor doua task-uri de back-end:
                // -crearea token-ului pentru utilizatorul nou inregistrat
                //-trimiterea mail-ului de verificare
                eventPublisher.publishEvent(new OnRegistrationCompleteEvent(user, request.getLocale(), appUrl));
                modelAndView.addObject("successMessage", "Registration mail was successfully sent!");
                modelAndView.setViewName("login");

            } catch (Exception ex) {
                //trimite eroarea care a avut loc la pag de register
                /*bindingResult.rejectValue("exception", "error.user",
                        ex.getMessage());
                modelAndView.addObject("errorMessages", bindingResult.getAllErrors()); */
                modelAndView.addObject("errorMessage", ex.getMessage());
                modelAndView.setViewName("signup");
                return modelAndView;
            }
        }
        return modelAndView;
    }

    //metoda pentru procesarea token-ului de verificare, apelata cand userul da click pe linkul de confirmare din mail
    //se extrage valoarea token-ului primit in URL, se cauta user-ul cu tokenul respectiv
    //in baza de date si se seteaza enabled = true
    @RequestMapping(value = "/registrationConfirm", method = RequestMethod.GET)
    public ModelAndView confirmRegistration(WebRequest request, @RequestParam("token") String token){
        ModelAndView modelAndView = new ModelAndView();
        Locale locale = request.getLocale();
        UserAccount user = userService.getByTokenId(token);
        //contul utilizatorului e deja activat -> sa nu se acceseze linkul de confirmare de mai multe ori
        if(user.isEnabled() == true){
            String alreadyActivatedAccount = messageSource.getMessage("auth.message.alreadyActivatedAccount", null, locale);
            modelAndView.addObject("errorMessage", alreadyActivatedAccount);

        }else {
            Calendar cal = Calendar.getInstance();
            //verificare daca token-ul a expirat -> se retrimite automat mail nou cu alt token
            if ((user.getTokenExpiryDate().getTime() - cal.getTime().getTime()) <= 0) {
                String expirationMessage = messageSource.getMessage("auth.message.expiredToken", null, locale);
                modelAndView.addObject("errorMessage", expirationMessage);
                //se retrimite event pentru a retrimite mail cu token nou
                eventPublisher.publishEvent(new OnRegistrationCompleteEvent(user, request.getLocale(), request.getContextPath()));
            }

            user.setEnabled(true);
            String activatedAccount = messageSource.getMessage("auth.message.successfulAccountActivation", null, locale);
            modelAndView.addObject("successMessage", activatedAccount);
            userService.updateUser(user);
        }

        modelAndView.setViewName("login");
        return modelAndView;
    }

    //model and view method for admin dashboard page which is a secure page that will show data from the successful login
    @RequestMapping(value = "/dashboard", method = RequestMethod.GET)
    public ModelAndView dashboard() {
        ModelAndView modelAndView = new ModelAndView();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user = userService.getByEmail(auth.getName());
        modelAndView.addObject("currentUser", user);
        modelAndView.addObject("fullName", "Welcome " + user.getFirstName() + " " + user.getLastName());
        modelAndView.addObject("adminMessage", "Content Available Only for Users with Admin Role");
        modelAndView.setViewName("dashboard");
        return modelAndView;
    }
}
