package com.example.demo.controllers;

import com.example.demo.classes.Campaign;
import com.example.demo.classes.Profile;
import com.example.demo.classes.UserAccount;
import com.example.demo.services.CampaignServiceImpl;
import com.example.demo.services.ProfileService;
import com.example.demo.services.RoleService;
import com.example.demo.services.UserService;
import com.example.demo.utils.FileOperation;
import javafx.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.File;
import java.util.List;

@Controller
public class UserProfileController {

    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private CampaignServiceImpl campaignService;
    @Autowired
    private MessageSource messageSource;

    @RequestMapping(value = "/myProfile", method = RequestMethod.GET)
    public ModelAndView getMyProfile(HttpServletRequest request, @RequestParam(value="errorMessage", required = false) final String errorMessage){
        ModelAndView modelAndView = new ModelAndView();
        UserAccount currentUserAccount = userService.getCurrentUserAccount();
        modelAndView.addObject("currentUser", currentUserAccount);
        Pair<Boolean, Boolean> pairCheck = roleService.checkIfAdminOrUser(currentUserAccount);
        modelAndView.addObject("isAdmin", pairCheck.getKey());
        modelAndView.addObject("isUser", pairCheck.getValue());

        if(errorMessage != null){
            modelAndView.addObject("errorMessage", errorMessage);
        }

        if(currentUserAccount != null){
            Profile currentProfile = profileService.getByUserAccount(currentUserAccount);
            if(currentProfile == null){
                //userul trebuie sa-si creeze un nou profil
                modelAndView.addObject("currentUser", currentUserAccount);
                modelAndView.setViewName("createProfile");
            }else{
                modelAndView.addObject("profile", currentProfile);
                File imgProfile = FileOperation.decodeString(currentProfile.getEncodedProfilePicture(), "profileDecoding.jpg");
                modelAndView.addObject("srcProfilePicture", imgProfile.getName());

                String coverPictureDb = currentProfile.getEncodedCoverPicture();
                if(!coverPictureDb.equals("")) {
                    File imgCover = FileOperation.decodeString(currentProfile.getEncodedCoverPicture(), "coverDecoding.jpg");
                    modelAndView.addObject("srcCoverPicture", imgCover.getName());
                }else{
                    modelAndView.addObject("srcCoverPicture", null);
                }
                List<Campaign> myCampaigns = campaignService.getByCreator(currentProfile);
                modelAndView.addObject("myCampaigns", myCampaigns);
                modelAndView.setViewName("showProfile");
            }
        }else modelAndView.setViewName("login");
        return modelAndView;
    }

    @RequestMapping(value = "/myProfile", method = RequestMethod.POST)
    public ModelAndView createProfile(@RequestParam("profilePicture") MultipartFile profilePicture,
                                      @RequestParam("coverPicture") MultipartFile coverPicture,
                                      @Valid Profile profile, BindingResult bindingResult, WebRequest request){
        ModelAndView modelAndView = new ModelAndView();
        UserAccount currentUserAccount = userService.getCurrentUserAccount();
        profile.setUserAccount(currentUserAccount);
        profile.setEncodedProfilePicture(FileOperation.encodeFile(FileOperation.convertMultipartFileToFile(profilePicture)));
        if(coverPicture != null) {
            profile.setEncodedCoverPicture(FileOperation.encodeFile(FileOperation.convertMultipartFileToFile(coverPicture)));
        }
        profileService.insertProfile(profile);
        modelAndView.addObject("currentUser", currentUserAccount);
        Pair<Boolean, Boolean> pairCheck = roleService.checkIfAdminOrUser(currentUserAccount);
        modelAndView.addObject("isAdmin", pairCheck.getKey());
        modelAndView.addObject("isUser", pairCheck.getValue());
        modelAndView.addObject("successMessage", messageSource.getMessage("message.createdProfile", null, request.getLocale()));
        modelAndView.setViewName("index");
        return modelAndView;
    }
}
