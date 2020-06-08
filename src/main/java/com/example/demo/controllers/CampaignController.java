package com.example.demo.controllers;

import com.example.demo.classes.*;
import com.example.demo.services.*;
import javafx.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class CampaignController {
    @Autowired
    private CampaignServiceImpl campaignService;
    @Autowired
    private DonationService donationService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private UserService userService;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private CloudinaryService cloudinaryService;
    @Autowired
    private RoleService roleService;

    //se verifica daca campania curenta este completa -> pt a dezactiva butonul sau de Donate
    @RequestMapping(value = "/campaignDetails",params = {"id"}, method = RequestMethod.GET)
    public ModelAndView campaignDetails(ModelMap model,
                                        @RequestParam("id") String id, /*HttpServletRequest request,*/
                                        @RequestParam(value = "bindingErrorMessages", required = false) List<ObjectError> bindingErrorMessages,
                                        @RequestParam(value = "successMessage", required = false) final String successMessage){
        Calendar current = Calendar.getInstance(); //se preia data curenta
       // ModelAndView modelAndView = new ModelAndView();
        UserAccount currentUserAccount = userService.getCurrentUserAccount();
        model.put("currentUser", currentUserAccount);
        Pair<Boolean, Boolean> pairCheck = roleService.checkIfAdminOrUser(currentUserAccount);
        model.put("isAdmin", pairCheck.getKey());
        model.put("isUser", pairCheck.getValue());

        if(bindingErrorMessages != null){
            model.put("bindingErrorMessages", bindingErrorMessages);
        }
        if(successMessage != null){
            model.put("successMessage", successMessage);
        }

        Campaign campaign = campaignService.getById(id);
        long daysLeftForDonations = campaignService.checkIfCampaignIsCompleted(campaign, current);

        //verificare daca contul curent este creatorul campaniei, acesta sa nu poata dona
        boolean canDonate = true;
        UserAccount campaignCreator = campaign.getCreator().getUserAccount();
        if(currentUserAccount != null){
            if(currentUserAccount.equals(campaignCreator) == true){
                canDonate = false;
            }
        }else canDonate = false;

        //daca canDonate e false (utilizatorul curent e creatorul campaniei), atunci trimitem in view toate donatiile facute catre campanie
        List<Donation> campaignDonations = null;
        if(canDonate == false){
            campaignDonations = donationService.getByCampaign(campaign);
        }

        //daca la campania curenta nu s-au strans deloc fonduri, creatorului i se permite sa editeze campania sau sa o stearga
        //se verifica in view si ca parametrul "canDonate" sa fie false
        boolean allowedToDeleteEdit = false;
        if(currentUserAccount != null && canDonate == false && campaign.getCurrentMoney() == 0){
            allowedToDeleteEdit = true;
        }

        float divMoney = ((float)campaign.getCurrentMoney())/campaign.getRequiredMoney();
        int progressBarValue = (int) (divMoney * 100);

        List<Category> allCategories = categoryService.getAllCategories();

        //obtinem din lista cu categoriile campaniei curente lista de String-uri cu id-urile acestora
        List<String> campaignCategoriesFilteredById  = campaign.getCategories().stream().map(Category::getId).collect(Collectors.toList());
        model.put("campaignCategoriesFilteredById", campaignCategoriesFilteredById);
        model.put("allCategories", allCategories);
        model.put("canDonate", canDonate);
        model.put("allowedToDeleteEdit", allowedToDeleteEdit);
        model.put("campaign", campaign);
        model.put("daysLeftForDonations", daysLeftForDonations);
        model.put("progressBarValue", progressBarValue);
        model.put("campaignDonations", campaignDonations);
        return new ModelAndView("campaignDetails", model);
    }

    @RequestMapping(value = "/changeCategories", method = RequestMethod.POST)
    public ModelAndView changeCategories(@RequestParam("campaignId") String campaignId,
                                         @RequestParam Map<String,String> allRequestParams, ModelMap model, WebRequest request,
                                         RedirectAttributes redirectAttributes){
        List<Category> allCategories = categoryService.getAllCategories();
        List<Category> newCategories = new ArrayList<>();

        for(Category category : allCategories){
            String categoryId = category.getId();
            String checkboxUserValue = allRequestParams.get(categoryId);
            if(checkboxUserValue != null){
                newCategories.add(category);
            }
        }
        Campaign currentCampaign = campaignService.getById(campaignId);
        currentCampaign.setCategories(newCategories);
        campaignService.updateCampaign(currentCampaign);

        String successChangeCategoriesMessage = messageSource.getMessage("campaign.successfullyChangedCategories", null, request.getLocale());
        redirectAttributes.addAttribute("successMessage", successChangeCategoriesMessage);
        return new ModelAndView("redirect:/campaignDetails?id=" + campaignId);

    }

    @RequestMapping(value = "/closeCampaign", method = RequestMethod.POST)
    public ModelAndView closeCampaign(@RequestParam("campaignId") String campaignId, HttpServletRequest request,
                                      RedirectAttributes redirectAttributes){
        Campaign campaign = campaignService.getById(campaignId);
        campaign.setStatus("closed");
        campaignService.updateCampaign(campaign);
        String closedCampaign = messageSource.getMessage("campaign.successfullyClosed", null, request.getLocale());
        redirectAttributes.addAttribute("successMessage", closedCampaign);
        return new ModelAndView("redirect:/campaignDetails?id=" + campaignId);
    }

    @RequestMapping(value = "/editCampaign", params = {"campaignId"}, method = RequestMethod.GET)
    public ModelAndView editCampaign(@RequestParam("campaignId") String campaignId, HttpServletRequest request){
        ModelAndView modelAndView = new ModelAndView();
        Campaign campaign = campaignService.getById(campaignId);
        modelAndView.addObject("campaign", campaign);
        UserAccount currentUserAccount = userService.getCurrentUserAccount();
        modelAndView.addObject("currentUser", currentUserAccount);
        Pair<Boolean, Boolean> pairCheck = roleService.checkIfAdminOrUser(currentUserAccount);
        modelAndView.addObject("isAdmin", pairCheck.getKey());
        modelAndView.addObject("isUser", pairCheck.getValue());
        modelAndView.setViewName("editCampaign");
        return modelAndView;
    }

    @RequestMapping(value = "/editCampaign", method = RequestMethod.POST)
    public ModelAndView editCampaign(@RequestParam("photosMultipart") ArrayList<MultipartFile> photosMultipart,
                                     @RequestParam("videoMultipart") MultipartFile videoMultipart,
                                     @Valid Campaign campaign, BindingResult bindingResult, WebRequest request,
                                     RedirectAttributes redirectAttributes){

        UserAccount currentUserAccount = userService.getCurrentUserAccount();

        if(bindingResult.hasErrors()){
            redirectAttributes.addFlashAttribute("bindingErrorMessages", bindingResult.getAllErrors());
            return new ModelAndView("redirect:/campaignDetails?id=" + campaign.getId());
        }else {
            Campaign dbCampaign = campaignService.getById(campaign.getId());
            List<String> dbCampaignPhotos = dbCampaign.getCampaignImages();
            Profile currentUserProfile = profileService.getByUserAccount(currentUserAccount);
            List<Campaign> currentUserCampaigns = campaignService.getByCreator(currentUserProfile);
            //daca lista de imagini a campaniei a fost modificata in edit
            if (photosMultipart.size() > 1 || (photosMultipart.size() == 1 && !photosMultipart.get(0).getOriginalFilename().equals(""))) {
                //sterge din Cloudinary toate pozele retinute in baza de date pt campania curenta (care nu mai sunt folosite la alte campanii ale userului curent)
                int i = 0;
                while (i < dbCampaignPhotos.size()) {
                    String photo = dbCampaignPhotos.get(i);
                    int ok = 0; // pp ca poza nu mai e folosita la alte camp ale userului curent
                    for (Campaign camp : currentUserCampaigns) {
                        if (!camp.getId().equals(campaign.getId())) {
                            List<String> auxCampPhotos = camp.getCampaignImages();
                            for (String auxPhoto : auxCampPhotos) {
                                if (auxPhoto.contains(photo)) {
                                    ok = 1;
                                    break;
                                }
                            }
                        }
                    }
                    if (ok == 0) {
                        cloudinaryService.deleteFileFromCloudinary(photo, currentUserAccount.getId(), "image");
                    }
                    dbCampaignPhotos.remove(photo);
                }

                //adaugam toate pozele din editedCampPhotoFilenames in Cloudinary
                try {
                    cloudinaryService.addMultipleImagesToCloudinary(photosMultipart, dbCampaign, currentUserAccount.getId());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //daca s-a selectat vreun video in editare, atunci il sterg pe cel din baza de date din Cloudinary, daca nu mai e folosit de alte camp
            //ale userului curent si il adaug pe cel nou
            if (!videoMultipart.getOriginalFilename().equals("")) {
                String dbVideo = dbCampaign.getCampaignVideo();
                int ok = 0;
                for (Campaign camp : currentUserCampaigns) {
                    if (!camp.getId().equals(campaign.getId()) && camp.getCampaignVideo().equals(dbVideo)) {
                        ok = 1;
                        break;
                    }
                }
                if (ok == 0) {
                    cloudinaryService.deleteFileFromCloudinary(dbVideo, currentUserAccount.getId(), "video");
                }
                try {
                    cloudinaryService.addVideoToCloudinary(videoMultipart, dbCampaign, currentUserAccount.getId());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            dbCampaign.setTitle(campaign.getTitle());
            dbCampaign.setCity(campaign.getCity());
            dbCampaign.setCountry(campaign.getCountry());
            dbCampaign.setStory(campaign.getStory());
            dbCampaign.setDeadlineInDays(campaign.getDeadlineInDays());
            dbCampaign.setRequiredMoney(campaign.getRequiredMoney());

            campaignService.updateCampaign(dbCampaign);
            String successEditCampaignMessage = messageSource.getMessage("campaign.successfulEdit", null, request.getLocale());
            redirectAttributes.addAttribute("successMessage", successEditCampaignMessage);
            return new ModelAndView("redirect:/campaignDetails?id=" + campaign.getId());
        }
    }

    @RequestMapping(value = "/startCampaign", method = RequestMethod.GET)
    public ModelAndView startCampaign(HttpServletRequest request, final RedirectAttributes redirectAttributes){
        ModelAndView modelAndView = new ModelAndView();
        UserAccount currentUserAccount = userService.getCurrentUserAccount();
        modelAndView.addObject("currentUser", currentUserAccount);
        Pair<Boolean, Boolean> pairCheck = roleService.checkIfAdminOrUser(currentUserAccount);
        Boolean isAdmin = pairCheck.getKey();
        Boolean isUser = pairCheck.getValue();
        modelAndView.addObject("isAdmin", isAdmin);
        modelAndView.addObject("isUser", isUser);

        if(currentUserAccount == null) modelAndView.setViewName("login");
        else if(isUser == false){
            String cantStartCampaignMessage = messageSource.getMessage("userRole.cantStartCampaign", null, request.getLocale());
            redirectAttributes.addAttribute("errorMessage", cantStartCampaignMessage);
            return new ModelAndView("redirect:/index");
        } else{
            Profile currentUserProfile = profileService.getByUserAccount(currentUserAccount);
            if(currentUserProfile == null) {
                modelAndView.addObject("errorMessage", "You aren't allowed to start a campaign until you create" +
                        " your profile!");
                modelAndView.setViewName("createProfile");
            }
            else {
                List<Category> allCategories = categoryService.getAllCategories();
                   /* Map<String,String> categorySelectMap = new LinkedHashMap<String,String>();
                    for(Category category : allCategories) {
                        categorySelectMap.put(category.getId(), category.getCategoryName());
                    }
                    modelAndView.addObject("categorySelectMap", categorySelectMap); */
                if(allCategories.size() > 0) {
                    modelAndView.addObject("allCategories", allCategories);
                }else {
                    modelAndView.addObject("allCategories", null);
                }
                modelAndView.addObject("profile", currentUserProfile);
                modelAndView.setViewName("startCampaign");
            }
        }
        return modelAndView;
    }



    //se cauta care campanii sunt complete (nu se mai poate dona pt ele) -> pt a afisa cate un check
    @RequestMapping(value = "/showCampaigns", method = RequestMethod.GET)
    public ModelAndView showCampaigns(HttpServletRequest request, @RequestParam(value="successMessage", required = false) final String successMessage) {
        Calendar current = Calendar.getInstance(); //se preia data curenta
        ModelAndView modelAndView = new ModelAndView();
        UserAccount currentUserAccount = userService.getCurrentUserAccount();
        modelAndView.addObject("currentUser", currentUserAccount);
        Pair<Boolean, Boolean> pairCheck = roleService.checkIfAdminOrUser(currentUserAccount);
        modelAndView.addObject("isAdmin", pairCheck.getKey());
        modelAndView.addObject("isUser", pairCheck.getValue());

        if(successMessage != null){
            modelAndView.addObject("successMessage", successMessage);
        }

        List<Campaign> campaigns = campaignService.getAllCampaigns();
        for (int i = 0; i < campaigns.size(); i++) {
            Campaign currentCampaign = campaigns.get(i);
            campaignService.checkIfCampaignIsCompleted(currentCampaign, current);
        }
        modelAndView.addObject("campaigns", campaigns);
        modelAndView.setViewName("showCampaigns");
        return modelAndView;
    }

    @RequestMapping(value = "/searchCampaign", method = RequestMethod.GET)
    public ModelAndView searchCampaign(@RequestParam("searchInput") String searchInput, HttpServletRequest request){
        ModelAndView modelAndView = new ModelAndView();
        List<Campaign> campaigns = campaignService.getAllCampaigns();
        List<Campaign> searchedCampaigns = new ArrayList<>();
        for(Campaign campaign:campaigns){
            if(campaign.getTitle().toLowerCase().contains(  searchInput.toLowerCase())){
                searchedCampaigns.add(campaign);
            }
        }
        modelAndView.addObject("campaigns", searchedCampaigns);
        UserAccount currentUserAccount = userService.getCurrentUserAccount();
        modelAndView.addObject("currentUser", currentUserAccount);
        Pair<Boolean, Boolean> pairCheck = roleService.checkIfAdminOrUser(currentUserAccount);
        modelAndView.addObject("isAdmin", pairCheck.getKey());
        modelAndView.addObject("isUser", pairCheck.getValue());

        modelAndView.setViewName("showCampaigns");
        return modelAndView;
    }


    @RequestMapping(value = "/startCampaign", method = RequestMethod.POST)
    public ModelAndView createCampaign(@RequestParam("photosMultipart") ArrayList<MultipartFile> photosMultipart,
                                       @RequestParam("videoMultipart") MultipartFile videoMultipart,
                                       @RequestParam(value = "categoriesSelect", required = false) List<String> categoriesSelect,
                                       @Valid Campaign campaign, BindingResult bindingResult, WebRequest request,
                                       RedirectAttributes redirectAttributes){
      //  ModelAndView modelAndView = new ModelAndView();
        boolean okCampaignMedia = true;
        String campaignMediaNotNullMessage = "";
        if(photosMultipart.size() == 1 && photosMultipart.get(0).getOriginalFilename().equals("") ||
                videoMultipart.getOriginalFilename().equals("")){
            okCampaignMedia = false;
        }

        if(okCampaignMedia == false){
            campaignMediaNotNullMessage = messageSource.getMessage("message.campaignMediaNotNull", null, request.getLocale());
        }

        if(bindingResult.hasErrors()){
           // modelAndView = startCampaign(null);
          //  modelAndView.addObject("bindingErrorMessages", bindingResult.getAllErrors());

            if(okCampaignMedia == false){
               // modelAndView.addObject("errorMessage",campaignMediaNotNullMessage);
                redirectAttributes.addAttribute("errorMessage", campaignMediaNotNullMessage);
            }
            return new ModelAndView("redirect:/startCampaign");
            //return modelAndView;
        }

        if(okCampaignMedia == false) {
           // modelAndView = startCampaign(null);
           // modelAndView.addObject("errorMessage", campaignMediaNotNullMessage);
           // return modelAndView;
            redirectAttributes.addAttribute("errorMessage", campaignMediaNotNullMessage);
            return new ModelAndView("redirect:/startCampaign");
        }

        UserAccount currentUserAccount = userService.getCurrentUserAccount();
       // modelAndView.addObject("currentUser", currentUserAccount);
       // Pair<Boolean, Boolean> pairCheck = roleService.checkIfAdminOrUser(currentUserAccount);
        // modelAndView.addObject("isAdmin", pairCheck.getKey());
        //modelAndView.addObject("isUser", pairCheck.getValue());

        Profile profile = profileService.getByUserAccount(currentUserAccount);
        campaign.setCreator(profile); //setarea in campanie a profilului creator
        campaign.setLaunchDate(new Date()); //new Date() creeaza un obiect Date cu data curenta
        //adaugam campania curenta in lista de camp a fiecarei categorii din selectul multiplu
        campaign.setStatus("active");

        if(categoriesSelect != null) {
            for (String categoryId : categoriesSelect) {
                Category category = categoryService.getById(categoryId);
                campaign.getCategories().add(category);
            }
            campaignService.insertCampaign(campaign);
        }
        try {
            //fiecare user va avea un folder cu numele id-ul sau
            //adaugarea tuturor fotografiilor in subfolderul cu poze(photos) in Cloudinary
            cloudinaryService.addMultipleImagesToCloudinary(photosMultipart, campaign, currentUserAccount.getId());

            //adaugare videoclipului in subfolderul cu videos (videos) in Cloudinary
            cloudinaryService.addVideoToCloudinary(videoMultipart, campaign, currentUserAccount.getId());
            campaignService.updateCampaign(campaign);

            profile.getCampaigns().add(campaign); //adaugam campania in lista cu campaniile profilului creator
            profileService.updateProfile(profile);
        } catch (IOException e){
            e.printStackTrace();
           // modelAndView = startCampaign(null);
           //  modelAndView.addObject("errorMessage", e.getMessage());
            redirectAttributes.addAttribute("errorMessage", e.getMessage());
            return new ModelAndView("redirect:/startCampaign");
        }

       // modelAndView = index(null, null, null);
        String successMessage = messageSource.getMessage("message.createdCampaign", null, request.getLocale());
       // modelAndView.addObject("successMessage", successMessage);
       // return modelAndView;
        redirectAttributes.addAttribute("successMessage", successMessage);
        return new ModelAndView("redirect:/index");
    }

    @RequestMapping(value = "/deleteCampaign", method = RequestMethod.POST)
    public ModelAndView deleteCampaign(@RequestParam("campaignId") String campaignId, HttpServletRequest request, RedirectAttributes redirectAttributes){
        boolean deleteStatus = true; //deleteStatus ramane true daca s-au sters cu succes fisierele utilizate doar de campania curenta
        UserAccount currentUserAccount = userService.getCurrentUserAccount();
        String currentUserId = currentUserAccount.getId();
        List<Campaign> currentUserAllCampaigns = campaignService.getByCreator(profileService.getByUserAccount(currentUserAccount));

        Campaign currentCampaign = campaignService.getById(campaignId);
        List<String> currentCampaignPhotos = currentCampaign.getCampaignImages();
        for(String photo : currentCampaignPhotos){
            int ok = 0; //presupunem ca o poza din campania ce trebuie stearsa nu mai este folosita de catre alta campanie a utilizatorului curent
            for(Campaign userCampaign : currentUserAllCampaigns){
                if(!userCampaign.getId().equals(campaignId) && userCampaign.getCampaignImages().contains(photo)){
                    ok = 1;
                    break;
                }
            }
            //sterg poza curenta din Cloudinary doar daca nu mai e folosita de alta campanie a utilizatorului curent
            if(ok == 0){
                deleteStatus = deleteStatus && cloudinaryService.deleteFileFromCloudinary(photo, currentUserId, "image");
            }
        }
        String video = currentCampaign.getCampaignVideo();
        int ok = 0; //presupunem ca videoclipul din campania ce trebuie stearsa nu mai este folosit de catre alta campanie a utilizatorului curent
        for(Campaign userCampaign : currentUserAllCampaigns){
            if(!userCampaign.getId().equals(campaignId) && userCampaign.getCampaignVideo().equals(video)){
                ok = 1;
                break;
            }
        }
        //sterg videoclipul curent din Cloudinary doar daca nu mai e folosit de alta campanie a utilizatorului curent
        if(ok == 0){
            deleteStatus = deleteStatus && cloudinaryService.deleteFileFromCloudinary(video, currentUserId, "video");
        }

        //stergerea campaniei din baza de date
        campaignService.deleteCampaign(campaignId);
        if(deleteStatus == true){
            String successDeleteCampaignMessage = messageSource.getMessage("campaign.successfulDelete", null, request.getLocale());
            redirectAttributes.addAttribute("successMessage", successDeleteCampaignMessage);
            return new ModelAndView("redirect:/showCampaigns");
        }else{
            String failDeleteCampaignMessage = messageSource.getMessage("campaign.failDelete", null, request.getLocale());
            redirectAttributes.addAttribute("errorMessage", failDeleteCampaignMessage);
            return new ModelAndView("redirect:/showCampaigns");
        }
    }

}
