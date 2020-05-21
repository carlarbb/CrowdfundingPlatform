package com.example.demo.eventhandlers;

import com.example.demo.classes.UserAccount;
import com.example.demo.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

//custom login success handler
@Component
public class MyAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    //custom landing page after successful login
    @Autowired
    UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        //set our response to OK status
        response.setStatus(HttpServletResponse.SC_OK);

        /*for (GrantedAuthority auth : authentication.getAuthorities()) {
            //daca user-ul logat cu succes are rol de ADMIN => redirectionare la pagina "dashboard"
            if ("ADMIN".equals(auth.getAuthority())) {
                List<UserAccount> users = userService.getAllUsers();
                //  modelAndView.addObject("users", users );

                //trimite lista cu toti utilizatorii in dashboard
                response.sendRedirect("/dashboard");
            }
            //daca user-ul logat cu succes are rol de USER => redirectionare la pagina "index"
            else if("USER".equals(auth.getAuthority())){
                response.sendRedirect("/index");
            }
        } */
        response.sendRedirect("/index?loginSuccess=true");
    }
}
