package com.example.demo.controllers;

import com.example.demo.classes.Role;
import com.example.demo.classes.UserAccount;
import com.example.demo.services.RoleService;
import com.example.demo.services.UserService;
import javafx.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class UserRolesController {

    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private MessageSource messageSource;

    @RequestMapping(value = "/userRolesManagement", method = RequestMethod.GET)
    public ModelAndView userRolesManagement(HttpServletRequest request){
        ModelAndView modelAndView = new ModelAndView();
        List<UserAccount> allUsers = userService.getAllUsers();
        modelAndView.addObject("allUsers", allUsers);

        //obtinem din fiecare lista de obiecte Role din fiecare user cate o lista de roluri sub forma de String si le trimitem in view
        List<List<String>> userRolesFiltered = new ArrayList<>();
        for(UserAccount user : allUsers){
            List<String> roleNames = user.getRoles().stream().map(Role::getRole).collect(Collectors.toList());
            userRolesFiltered.add(roleNames);
        }
        modelAndView.addObject("userRolesFilteredStrings", userRolesFiltered);
        UserAccount currentUserAccount = userService.getCurrentUserAccount();
        modelAndView.addObject("currentUser", currentUserAccount);
        Pair<Boolean, Boolean> pairCheck = roleService.checkIfAdminOrUser(currentUserAccount);
        modelAndView.addObject("isAdmin", pairCheck.getKey());
        modelAndView.addObject("isUser", pairCheck.getValue());
        modelAndView.setViewName("userRolesDashboard");
        return modelAndView;
    }

    @RequestMapping(value = "/userRolesManagement", method = RequestMethod.POST)
    public ModelAndView userRolesManagement(@RequestParam Map<String,String> allRequestParams, ModelMap model, WebRequest request,
                                            RedirectAttributes redirectAttributes){

        List<UserAccount> allUsers = userService.getAllUsers();
        for(UserAccount user : allUsers){
            Set<Role> newRoleList = new HashSet<>();

            String userId = user.getId();
            String checkboxUserValue = allRequestParams.get(userId + "user");
            if(checkboxUserValue != null){
                newRoleList.add(roleService.findByRole("USER"));
            }
            String checkboxAdminValue = allRequestParams.get(userId + "admin");
            if(checkboxAdminValue != null){
                newRoleList.add(roleService.findByRole("ADMIN"));
            }
            //daca adminul a deselectat ambele roluri, utilizatorul sa ramana cu rolul User pe care il avea implicit
            if(checkboxAdminValue == null && checkboxUserValue == null){
                newRoleList.add(roleService.findByRole("USER"));
            }
            user.setRoles(newRoleList);
            userService.updateUser(user);
        }
        String successfulRoleChange = messageSource.getMessage("role.successfullyChanged", null, request.getLocale());
        redirectAttributes.addAttribute("successMessage", successfulRoleChange);
        return new ModelAndView("redirect:/index");
    }
}
