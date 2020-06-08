package com.example.demo.controllers;

import com.example.demo.classes.*;
import com.example.demo.services.*;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Controller
public class HomeController {

    @Autowired
    private UserService userService;
    @Autowired
    private CampaignServiceImpl campaignService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private RoleService roleService;


    //comparator descendent pt o lista de campanii, in functie de fondurile acumulate
    class FundsComparator implements Comparator<Campaign> {
        @Override
        public int compare(Campaign a, Campaign b) {
            int aFunds = a.getCurrentMoney();
            int bFunds = b.getCurrentMoney();
            return bFunds < aFunds ? -1 : aFunds == bFunds ? 0 : 1;
        }
    }


    @RequestMapping(value = {"/","/home", "/index"}, method = RequestMethod.GET)
    public ModelAndView index(HttpServletRequest request, @RequestParam(value="loginSuccess", required = false) Boolean loginSuccess,
                              @RequestParam(value="logoutSuccess", required = false) Boolean logoutSuccess,
                              @RequestParam(value="errorMessage", required = false) final String errorMessage,
                              @RequestParam(value="successMessage", required = false) final String successMessage) {
       // System.out.println(messageSource.getMessage("confirmRegistration", null, request.getLocale()));
        ModelAndView modelAndView = new ModelAndView();
        if(loginSuccess != null && loginSuccess == true) {
            String successfullyLoggedIn = messageSource.getMessage("auth.message.successfullyLoggedIn", null, request.getLocale());
            modelAndView.addObject("successMessage", successfullyLoggedIn);
        }
        if(logoutSuccess != null && logoutSuccess == true){
            String successfullyLoggedOut = messageSource.getMessage("auth.message.successfullyLoggedOut", null, request.getLocale());
            modelAndView.addObject("successMessage", successfullyLoggedOut);
        }
        if(errorMessage != null){
            modelAndView.addObject("errorMessage", errorMessage);
        }
        if(successMessage != null){
            modelAndView.addObject("successMessage", successMessage);
        }

        //transmitem in index userul curent daca exista, pentru a sti cum sa afisam in meniu: login sau logout
        UserAccount currentUserAccount = userService.getCurrentUserAccount();
        modelAndView.addObject("currentUser", currentUserAccount);
        //verificam daca userul curent are rol de Admin sau User
        Pair<Boolean, Boolean> pairCheck = roleService.checkIfAdminOrUser(currentUserAccount);
        modelAndView.addObject("isAdmin", pairCheck.getKey());
        modelAndView.addObject("isUser", pairCheck.getValue());

        List<Campaign> allCampaigns = campaignService.getAllCampaigns();
        allCampaigns.sort(new FundsComparator());
        allCampaigns.removeIf(p -> p.getCurrentMoney() == 0);
        int len = allCampaigns.size();
        if(len > 0){
            if(len > 6){
                allCampaigns.subList(6, allCampaigns.size()).clear();
            }
            modelAndView.addObject("mostPopularCampaigns", allCampaigns);
        }else modelAndView.addObject("mostPopularCampaigns", null);

        List<Category> allCategories = categoryService.getAllCategories();
        if(allCategories.size() > 0) {
            modelAndView.addObject("allCategories", allCategories);
        }
        else modelAndView.addObject("allCategories", null);
        modelAndView.setViewName("index");
        return modelAndView;
    }
}
