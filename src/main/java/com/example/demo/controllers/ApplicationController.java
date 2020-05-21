package com.example.demo.controllers;

import com.cloudinary.utils.ObjectUtils;
import com.example.demo.classes.*;
import com.example.demo.configuration.CloudinaryConfig;
import com.example.demo.repositories.RoleRepository;
import com.example.demo.services.*;
import com.sun.org.apache.xpath.internal.operations.Bool;
import com.sun.org.apache.xpath.internal.operations.Mult;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.jws.WebParam;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Controller
public class ApplicationController {
    @Autowired
    private RoleRepository roleRepository;

   /* @Autowired
    UserAccountRepository userAccountRepository;
    */

    @Autowired
    private UserService userService;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private CampaignService campaignService;
    @Autowired
    private DonationService donationService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    CloudinaryConfig cloudc;
    @Autowired
    private MessageSource messageSource;

    private final PayPalClient payPalClient;
    //detalii pentru donatia curenta
    private String currentCampaignId;
    private String currentAmount;

    //comparator descendent pt o lista de campanii, in functie de fondurile acumulate
    class FundsComparator implements Comparator<Campaign> {
        @Override
        public int compare(Campaign a, Campaign b) {
            int aFunds = a.getCurrentMoney();
            int bFunds = b.getCurrentMoney();
            return bFunds < aFunds ? -1 : aFunds == bFunds ? 0 : 1;
        }
    }

    @Autowired
    ApplicationController(PayPalClient payPalClient) {
        this.payPalClient = payPalClient;
    }


    private UserAccount getCurrentUserAccount(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user;
        if(auth!=null){
            user = userService.getByEmail(auth.getName());
        }else user = null;
        return user;
    }

    //metoda ce verifica daca un cont de utilizator contine rolurile Admin sau User
    private Pair<Boolean, Boolean> checkIfAdminOrUser(UserAccount user){
        boolean isAdmin = false;
        boolean isUser = false;
        if(user != null) {
            Set<Role> userRoles = user.getRoles();
            for (Role role : userRoles) {
                if (role.getRole().equals("ADMIN")) {
                    isAdmin = true;
                }else if(role.getRole().equals("USER")){
                    isUser = true;
                }
            }
        }
        return new Pair<>(isAdmin, isUser);
    }

    @RequestMapping(value = {"/","/home", "/index"}, method = RequestMethod.GET)
    public ModelAndView index(HttpServletRequest request, @RequestParam(value="loginSuccess", required = false) Boolean loginSuccess,
                              @RequestParam(value="logoutSuccess", required = false) Boolean logoutSuccess) {
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

        //transmitem in index userul curent daca exista, pentru a sti cum sa afisam in meniu: login sau logout
        UserAccount user = getCurrentUserAccount();
        modelAndView.addObject("currentUser", user);
        //verificam daca userul curent are rol de Admin sau User
        Pair<Boolean, Boolean> pairCheck = checkIfAdminOrUser(user);
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

    @RequestMapping(value = "/myProfile", method = RequestMethod.GET)
    public ModelAndView getMyProfile(HttpServletRequest request){
        ModelAndView modelAndView = new ModelAndView();
        UserAccount currentUserAccount = getCurrentUserAccount();
        modelAndView.addObject("currentUser", currentUserAccount);
        Pair<Boolean, Boolean> pairCheck = checkIfAdminOrUser(currentUserAccount);
        modelAndView.addObject("isAdmin", pairCheck.getKey());
        modelAndView.addObject("isUser", pairCheck.getValue());

        if(currentUserAccount != null){
            Profile currentProfile = profileService.getByUserAccount(currentUserAccount);
            if(currentProfile == null){
                //userul trebuie sa-si creeze un nou profil
                modelAndView.addObject("currentUser", currentUserAccount);
                modelAndView.setViewName("createProfile");
            }else{
                modelAndView.addObject("profile", currentProfile);
                File imgProfile = decodeString(currentProfile.getEncodedProfilePicture(), "profileDecoding.jpg");
                modelAndView.addObject("srcProfilePicture", imgProfile.getName());

                String coverPictureDb = currentProfile.getEncodedCoverPicture();
                if(!coverPictureDb.equals("")) {
                    File imgCover = decodeString(currentProfile.getEncodedCoverPicture(), "coverDecoding.jpg");
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

    public static String encodeFile(File file){
        byte[] fileContent = new byte[0];
        try {
            fileContent = FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String encodedString = Base64.getEncoder().encodeToString(fileContent);
        return encodedString;
    }

    public static File decodeString(String encodedFile, String fileName){
        byte[] decodedBytes = Base64.getDecoder().decode(encodedFile);
        File file = new File("target\\classes\\static\\images\\" + fileName);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        writer.print("");
        writer.close();

        try {
            FileUtils.writeByteArrayToFile(file, decodedBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    public static File convertMultipartFileToFile(MultipartFile multipartFile){
        String path = "target\\classes\\static\\images\\auxphoto.jpg";
        File file = new File(path);
        try {
            file.createNewFile();
            Path filepath = Paths.get(path);

            OutputStream os = Files.newOutputStream(filepath);
            os.write(multipartFile.getBytes());
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    @RequestMapping(value = "/myProfile", method = RequestMethod.POST)
    public ModelAndView createProfile(@RequestParam("profilePicture") MultipartFile profilePicture,
                                      @RequestParam("coverPicture") MultipartFile coverPicture,
                                      @Valid Profile profile, BindingResult bindingResult, WebRequest request){
        ModelAndView modelAndView = new ModelAndView();
        UserAccount currentUserAccount = getCurrentUserAccount();
        profile.setUserAccount(currentUserAccount);
        profile.setEncodedProfilePicture(encodeFile(convertMultipartFileToFile(profilePicture)));
        if(coverPicture != null) {
            profile.setEncodedCoverPicture(encodeFile(convertMultipartFileToFile(coverPicture)));
        }
        profileService.insertProfile(profile);
        modelAndView.addObject("currentUser", currentUserAccount);
        Pair<Boolean, Boolean> pairCheck = checkIfAdminOrUser(currentUserAccount);
        modelAndView.addObject("isAdmin", pairCheck.getKey());
        modelAndView.addObject("isUser", pairCheck.getValue());
        modelAndView.addObject("successMessage", messageSource.getMessage("message.createdProfile", null, request.getLocale()));
        modelAndView.setViewName("index");
        return modelAndView;
    }

    void addMultipleImagesToCloudinary(List<MultipartFile> photosMultipart, Campaign campaign, String currentUserId) throws IOException {
        for(MultipartFile photo : photosMultipart){
            String imgNameWithExtension = photo.getOriginalFilename();
            String imgName = imgNameWithExtension.substring(0, imgNameWithExtension.indexOf('.'));
            Map uploadResult = cloudc.upload(photo.getBytes(), ObjectUtils.asMap("public_id",
                    currentUserId + "/photos/" + imgName, "resource_type", "auto", "overwrite", true));

            campaign.getCampaignImages().add(uploadResult.get("url").toString());
        }
    }

    void addVideoToCloudinary(MultipartFile videoMultipart, Campaign campaign, String currentUserId) throws IOException {
        String videoNameWithExtension = videoMultipart.getOriginalFilename();
        String videoName = videoNameWithExtension.substring(0, videoNameWithExtension.indexOf('.'));

        Map uploadVideo = cloudc.upload(videoMultipart.getBytes(), ObjectUtils.asMap( "public_id",
                    currentUserId + "/videos/" + videoName, "resource_type", "auto", "overwrite", true));
        campaign.setCampaignVideo(uploadVideo.get("url").toString());
    }

    @RequestMapping(value = "/startCampaign", method = RequestMethod.GET)
    public ModelAndView startCampaign(HttpServletRequest request){
        ModelAndView modelAndView = new ModelAndView();
        UserAccount currentUserAccount = getCurrentUserAccount();
        modelAndView.addObject("currentUser", currentUserAccount);
        Pair<Boolean, Boolean> pairCheck = checkIfAdminOrUser(currentUserAccount);
        Boolean isAdmin = pairCheck.getKey();
        Boolean isUser = pairCheck.getValue();
        modelAndView.addObject("isAdmin", isAdmin);
        modelAndView.addObject("isUser", isUser);

        if(currentUserAccount == null) modelAndView.setViewName("login");
        else if(isUser == false){
            modelAndView = index(null, null, null);
            String cantStartCampaignMessage = messageSource.getMessage("userRole.cantStartCampaign", null, request.getLocale());
            modelAndView.addObject("errorMessage", cantStartCampaignMessage);
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

    @RequestMapping(value = "/startCampaign", method = RequestMethod.POST)
    public ModelAndView createCampaign(@RequestParam("photosMultipart") ArrayList<MultipartFile> photosMultipart,
                                       @RequestParam("videoMultipart") MultipartFile videoMultipart,
                                       @RequestParam(value = "categoriesSelect", required = false) List<String> categoriesSelect,
                                       @Valid Campaign campaign, BindingResult bindingResult, WebRequest request){
        ModelAndView modelAndView = new ModelAndView();
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
            modelAndView = startCampaign(null);
            modelAndView.addObject("bindingErrorMessages", bindingResult.getAllErrors());
            if(okCampaignMedia == false){
                modelAndView.addObject("errorMessage",campaignMediaNotNullMessage);
            }
            return modelAndView;
        }

        if(okCampaignMedia == false) {
            modelAndView = startCampaign(null);
            modelAndView.addObject("errorMessage", campaignMediaNotNullMessage);
            return modelAndView;
        }

        /*if(categoriesSelect == null){
            modelAndView = startCampaign(null);
            String errorMessage = messageSource.getMessage("message.campaignCategoriesNotNull", null, request.getLocale());
            modelAndView.addObject("errorMessage", errorMessage);
            return modelAndView;
        } */

        UserAccount currentUserAccount = getCurrentUserAccount();
        modelAndView.addObject("currentUser", currentUserAccount);
        Pair<Boolean, Boolean> pairCheck = checkIfAdminOrUser(currentUserAccount);
        modelAndView.addObject("isAdmin", pairCheck.getKey());
        modelAndView.addObject("isUser", pairCheck.getValue());

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
            addMultipleImagesToCloudinary(photosMultipart, campaign, currentUserAccount.getId());

            //adaugare videoclipului in subfolderul cu videos (videos) in Cloudinary
            addVideoToCloudinary(videoMultipart, campaign, currentUserAccount.getId());
            campaignService.updateCampaign(campaign);

            profile.getCampaigns().add(campaign); //adaugam campania in lista cu campaniile profilului creator
            profileService.updateProfile(profile);
        } catch (IOException e){
            e.printStackTrace();
            modelAndView = startCampaign(null);
            modelAndView.addObject("errorMessage", e.getMessage());
        }

        modelAndView = index(null, null, null);
        String successMessage = messageSource.getMessage("message.createdCampaign", null, request.getLocale());
        modelAndView.addObject("successMessage", successMessage);
        return modelAndView;
    }

    //metoda ce primeste o campanie si o data si verifica daca aceasta data depaseste deadline-ul pt donatii a campaniei
    //returneaza nr de zile in care se mai pot face donatii sau 0 daca s-a terminat perioada
    long checkIfCampaignIsCompleted(Campaign campaign, Calendar current){
        int deadlineInDays = campaign.getDeadlineInDays();
        if(campaign.isCompleted() == false){
            Calendar c = Calendar.getInstance();
            c.setTime(campaign.getLaunchDate());
            c.add(Calendar.HOUR, deadlineInDays*24);
            if(current.compareTo(c) >= 0){
                campaign.setCompleted(true);
                return 0;
            }
            else {
                long daysBetween = ChronoUnit.DAYS.between(current.toInstant(), c.toInstant());
                return daysBetween;
            }
        }
        else return 0;
    }

    //se cauta care campanii sunt complete (nu se mai poate dona pt ele) -> pt a afisa cate un check
    @RequestMapping(value = "/showCampaigns", method = RequestMethod.GET)
    public ModelAndView showCampaigns(HttpServletRequest request){
        Calendar current = Calendar.getInstance(); //se preia data curenta
        ModelAndView modelAndView = new ModelAndView();
        UserAccount currentUserAccount = getCurrentUserAccount();
        modelAndView.addObject("currentUser", currentUserAccount);
        Pair<Boolean, Boolean> pairCheck = checkIfAdminOrUser(currentUserAccount);
        modelAndView.addObject("isAdmin", pairCheck.getKey());
        modelAndView.addObject("isUser", pairCheck.getValue());

        List<Campaign> campaigns = campaignService.getAllCampaigns();
        for(int i=0; i<campaigns.size(); i++){
            Campaign currentCampaign = campaigns.get(i);
            checkIfCampaignIsCompleted(currentCampaign, current);
        }
        modelAndView.addObject("campaigns", campaigns);
        modelAndView.setViewName("showCampaigns");
        return modelAndView;
    }

    //se verifica daca campania curenta este completa -> pt a dezactiva butonul sau de Donate
    @RequestMapping(value = "/campaignDetails",params = {"id"}, method = RequestMethod.GET)
    public ModelAndView campaignDetails(@RequestParam("id") String id, HttpServletRequest request){
        Calendar current = Calendar.getInstance(); //se preia data curenta
        ModelAndView modelAndView = new ModelAndView();
        UserAccount currentUserAccount = getCurrentUserAccount();
        modelAndView.addObject("currentUser", currentUserAccount);
        Pair<Boolean, Boolean> pairCheck = checkIfAdminOrUser(currentUserAccount);
        modelAndView.addObject("isAdmin", pairCheck.getKey());
        modelAndView.addObject("isUser", pairCheck.getValue());

        Campaign campaign = campaignService.getById(id);
        long daysLeftForDonations = checkIfCampaignIsCompleted(campaign, current);

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
        modelAndView.addObject("campaignCategoriesFilteredById", campaignCategoriesFilteredById);
        modelAndView.addObject("allCategories", allCategories);
        modelAndView.addObject("canDonate", canDonate);
        modelAndView.addObject("allowedToDeleteEdit", allowedToDeleteEdit);
        modelAndView.addObject("campaign", campaign);
        modelAndView.addObject("daysLeftForDonations", daysLeftForDonations);
        modelAndView.addObject("progressBarValue", progressBarValue);
        modelAndView.addObject("campaignDonations", campaignDonations);
        modelAndView.setViewName("campaignDetails");
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
        UserAccount currentUserAccount = getCurrentUserAccount();
        modelAndView.addObject("currentUser", currentUserAccount);
        Pair<Boolean, Boolean> pairCheck = checkIfAdminOrUser(currentUserAccount);
        modelAndView.addObject("isAdmin", pairCheck.getKey());
        modelAndView.addObject("isUser", pairCheck.getValue());

        modelAndView.setViewName("showCampaigns");
        return modelAndView;
    }

    @RequestMapping(value = "/changeCategories", method = RequestMethod.POST)
    public ModelAndView changeCategories(@RequestParam("campaignId") String campaignId,
                                         @RequestParam Map<String,String> allRequestParams, ModelMap model, WebRequest request){
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

        ModelAndView modelAndView = campaignDetails(campaignId, null);
        String successChangeCategoriesMessage = messageSource.getMessage("campaign.successfullyChangedCategories", null, request.getLocale());
        modelAndView.addObject("successMessage", successChangeCategoriesMessage);
        return modelAndView;
    }

    /*
    //metoda cu wallet fix la activarea contului de user - fara PayPal
    @RequestMapping(value = "/donate", method = RequestMethod.POST)
    public ModelAndView donate(@RequestParam("amountOfMoney") int amountOfMoney, @RequestParam("campaignId") String campaignId,
                               HttpServletRequest request){
        Campaign campaign = campaignService.getById(campaignId);
        UserAccount userAccount = getCurrentUserAccount();
        int userWallet = userAccount.getWallet();
        int required = campaign.getRequiredMoney();
        int sum = campaign.getCurrentMoney() + amountOfMoney;
        if(sum >= required && campaign.isCompleted() == false){
            userAccount.setWallet(userWallet - (required - campaign.getCurrentMoney()));
            campaign.setCompleted(true);
            campaign.setCurrentMoney(required);
        }
        else {
            campaign.setCurrentMoney(sum);
            userAccount.setWallet(userAccount.getWallet() - amountOfMoney);
        }
        campaignService.updateCampaign(campaign);
        userService.updateUser(userAccount);
        return showCampaigns(request);
        //afisare mesaj de succes dupa o donare
    }
    */
    //sterge un fisier de orice tip din Cloudinary (fisier al unei campanii), in functie de id-ul sau public
    boolean deleteFileFromCloudinary(String fileDbUrl, String userId, String fileType){
        //public_id-ul unui fisier din Cloudinary nu contine extensia
        String publicId = fileDbUrl.substring(fileDbUrl.indexOf(userId));
        publicId = publicId.substring(0, publicId.indexOf('.'));
        Map deleteMap;
        if(fileType.equals("image")) {
            deleteMap = cloudc.delete(publicId, ObjectUtils.emptyMap());
        }else {
            deleteMap = cloudc.delete(publicId, ObjectUtils.asMap("resource_type", "video"));
        }
        if(deleteMap.get("result").equals("ok")) return true;
        return false;
    }


    @RequestMapping(value = "/deleteCampaign", method = RequestMethod.POST)
    public ModelAndView deleteCampaign(@RequestParam("campaignId") String campaignId, HttpServletRequest request){
        boolean deleteStatus = true; //deleteStatus ramane true daca s-au sters cu succes fisierele utilizate doar de campania curenta
        UserAccount currentUser = getCurrentUserAccount();
        String currentUserId = currentUser.getId();
        List<Campaign> currentUserAllCampaigns = campaignService.getByCreator(profileService.getByUserAccount(currentUser));

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
                deleteStatus = deleteStatus && deleteFileFromCloudinary(photo, currentUserId, "image");
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
            deleteStatus = deleteStatus && deleteFileFromCloudinary(video, currentUserId, "video");
        }

        //stergerea campaniei din baza de date
        campaignService.deleteCampaign(campaignId);
        ModelAndView modelAndView = showCampaigns(request);
        if(deleteStatus == true){
            /*System.out.println("Stergerea fisierelor din Cloudinary a avut loc cu succes!"); */
            String successDeleteCampaignMessage = messageSource.getMessage("campaign.successfulDelete", null, request.getLocale());
            modelAndView.addObject("successMessage", successDeleteCampaignMessage);
        }else{
            String failDeleteCampaignMessage = messageSource.getMessage("campaign.failDelete", null, request.getLocale());
            modelAndView.addObject("errorMessage", failDeleteCampaignMessage);
        }
        return modelAndView;
    }
    @RequestMapping(value = "/editCampaign", params = {"campaignId"}, method = RequestMethod.GET)
    public ModelAndView editCampaign(@RequestParam("campaignId") String campaignId, HttpServletRequest request){
        ModelAndView modelAndView = new ModelAndView();
        Campaign campaign = campaignService.getById(campaignId);
        modelAndView.addObject("campaign", campaign);
        UserAccount currentUserAccount = getCurrentUserAccount();
        modelAndView.addObject("currentUser", currentUserAccount);
        Pair<Boolean, Boolean> pairCheck = checkIfAdminOrUser(currentUserAccount);
        modelAndView.addObject("isAdmin", pairCheck.getKey());
        modelAndView.addObject("isUser", pairCheck.getValue());
        modelAndView.setViewName("editCampaign");
        return modelAndView;
    }

    @RequestMapping(value = "/editCampaign", method = RequestMethod.POST)
    public ModelAndView editCampaign(@RequestParam("photosMultipart") ArrayList<MultipartFile> photosMultipart,
                                     @RequestParam("videoMultipart") MultipartFile videoMultipart,
                                     @Valid Campaign campaign, BindingResult bindingResult, WebRequest request){

        ModelAndView modelAndView = new ModelAndView();
        UserAccount currentUser = getCurrentUserAccount();

        if(bindingResult.hasErrors()){
            modelAndView = campaignDetails(campaign.getId(), null);
            modelAndView.addObject("bindingErrorMessages", bindingResult.getAllErrors());
        }else {
            Campaign dbCampaign = campaignService.getById(campaign.getId());
            List<String> dbCampaignPhotos = dbCampaign.getCampaignImages();
            Profile currentUserProfile = profileService.getByUserAccount(currentUser);
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
                        deleteFileFromCloudinary(photo, currentUser.getId(), "image");
                    }
                    dbCampaignPhotos.remove(photo);
                }

                //adaugam toate pozele din editedCampPhotoFilenames in Cloudinary
                try {
                    addMultipleImagesToCloudinary(photosMultipart, dbCampaign, currentUser.getId());
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
                    deleteFileFromCloudinary(dbVideo, currentUser.getId(), "video");
                }
                try {
                    addVideoToCloudinary(videoMultipart, dbCampaign, currentUser.getId());
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
            modelAndView = campaignDetails(campaign.getId(), null);
            String successEditCampaignMessage = messageSource.getMessage("campaign.successfulEdit", null, request.getLocale());
            modelAndView.addObject("successMessage", successEditCampaignMessage);
        }

        modelAndView.addObject("currentUser", currentUser);
        Pair<Boolean, Boolean> pairCheck = checkIfAdminOrUser(currentUser);
        modelAndView.addObject("isAdmin", pairCheck.getKey());
        modelAndView.addObject("isUser", pairCheck.getValue());
        return modelAndView;
    }

    @RequestMapping(value = "/closeCampaign", method = RequestMethod.POST)
    public ModelAndView closeCampaign(@RequestParam("campaignId") String campaignId, HttpServletRequest request){
        Campaign campaign = campaignService.getById(campaignId);
        campaign.setStatus("closed");
        campaignService.updateCampaign(campaign);
        ModelAndView modelAndView = campaignDetails(campaignId, request);
        String closedCampaign = messageSource.getMessage("campaign.successfullyClosed", null, request.getLocale());
        modelAndView.addObject("successMessage", closedCampaign);
        return modelAndView;
    }

    //metoda de donatie cu PayPal
    @RequestMapping(value = "/donate", method = RequestMethod.POST)
    public ModelAndView donate(@RequestParam("amountOfMoney") String amountOfMoney, @RequestParam("campaignId") String campaignId,
                               WebRequest request) {

        //userul nu poate efectua nicio donatie daca nu are completat profilul sau
        UserAccount currentUser = getCurrentUserAccount();
        if(profileService.getByUserAccount(currentUser) == null){
            ModelAndView modelAndView = getMyProfile(null);
            String cantMakeDonationWithoutProfile = messageSource.getMessage("campaign.cantDonateWithoutProfile", null, request.getLocale());
            modelAndView.addObject("errorMessage", cantMakeDonationWithoutProfile);
            return modelAndView;
        }

        //setez campurile currentCampaignId si currentAmount din clasa cu parametrii primiti si voi modifica baza de date
        //doar atunci cand donatia a fost finalizata cu succes
        this.currentCampaignId = campaignId;
        this.currentAmount = amountOfMoney;

        Map<String, Object> result = payPalClient.createPayment(amountOfMoney);
        return new ModelAndView("redirect:" + result.get("redirect_url"));
    }

    //aici se ajunge dupa ce user-ul a confirmat payment-ul
    //se va finaliza donatia prin apelul metodei "completePayment" din payPalClient ce preia Payment ID si Payer ID din request
    @RequestMapping(value = "/complete/payment",params = {"paymentId", "token", "PayerID"}, method = RequestMethod.GET)
    public ModelAndView completePayment(HttpServletRequest request){
        Map<String, Object> result = payPalClient.completePayment(request);
        UserAccount currentUserAccount = getCurrentUserAccount();
        Profile currentProfile = profileService.getByUserAccount(currentUserAccount);

        String status = result.get("status").toString();
        if(status.equals("success")){
            //modific baza de date pt ca donatia s-a finalizat cu succes
            //doar adaug la campania curenta la currentMoney campul currentAmount retinut in clasa
            //daca s-a ajuns aici -> butonul Donate nu a fost disabled -> nu s-a atins deadline-ul cand a inceput donatia
            Campaign campaign = campaignService.getById(this.currentCampaignId);
            campaign.setCurrentMoney(campaign.getCurrentMoney() + Integer.parseInt(this.currentAmount));

            List<Donation> thisBackerDonations = donationService.getByInvestor(currentProfile);
            //verificam daca utilizatorul curent a mai facut vreo donatie pt campania curenta, pentru a nu-l mai numara la backers
            int ok = 0;
            for(Donation donation : thisBackerDonations){
                if(donation.getCampaign().equals(campaign)){
                    ok = 1;
                    break;
                }
            }
            if(ok == 0) {
                campaign.setNumberOfBackers(campaign.getNumberOfBackers() + 1);
            }
            campaignService.updateCampaign(campaign);

            Calendar c = Calendar.getInstance();
            Donation donation = new Donation(c.getTime(), campaign, currentProfile, Integer.parseInt(this.currentAmount));
            donationService.insertDonation(donation);
        }
        ModelAndView modelAndView = index(null, null, null);
        modelAndView.addObject("payPalTransactionStatus", status); //trimit in index statusul pt a afisa mesaj de succes/eroare
        modelAndView.addObject("currentUser", currentUserAccount);
        Pair<Boolean, Boolean> pairCheck = checkIfAdminOrUser(currentUserAccount);
        modelAndView.addObject("isAdmin", pairCheck.getKey());
        modelAndView.addObject("isUser", pairCheck.getValue());
        String successfulDonationMessage = messageSource.getMessage("campaign.successfulDonation", null, request.getLocale());
        modelAndView.addObject("successMessage", successfulDonationMessage);
        //modelAndView.setViewName("index");
        return modelAndView;
    }

    @RequestMapping(value = "/addCategory", method = RequestMethod.POST)
    public ModelAndView createCategory(@Valid Category category, @RequestParam("photoMultipart") MultipartFile categoryPhoto,
                                       WebRequest request){

        ModelAndView modelAndView;
        UserAccount currentUserAccount = getCurrentUserAccount();

        boolean okUploadPhoto = true;
        if(categoryPhoto.getOriginalFilename().equals("")) okUploadPhoto = false;
        boolean okCategoryName = true;
        if(category.getCategoryName().equals("")) okCategoryName = false;

        if(okCategoryName == false || okUploadPhoto == false){
            modelAndView = index(null, null, null);
            if(okCategoryName == false) {
                String categoryNameNotNullMessage = messageSource.getMessage("category.nameNotNull", null, request.getLocale());
                modelAndView.addObject("errorMessage", categoryNameNotNullMessage);
            }
            else {
                String categoryImageNotNullMessage = messageSource.getMessage("category.imageNotNull", null, request.getLocale());
                modelAndView.addObject("errorMessage", categoryImageNotNullMessage);
            }
        } else {
            Profile profile = profileService.getByUserAccount(currentUserAccount);
            if (profile == null) { //nu poti crea o categorie fara a avea completat profilul
                modelAndView = getMyProfile(null);
                String cantCreateCategoryWithoutProfileMessage = messageSource.getMessage("category.cantCreateWithoutProfile",
                                                        null, request.getLocale());
                modelAndView.addObject("errorMessage", cantCreateCategoryWithoutProfileMessage);
                //return new ModelAndView("redirect:" + "/myProfile");
            }
            else {
                Calendar c = Calendar.getInstance();
                category.setCreator(profile);
                category.setDate(c.getTime());
               // category.setCampaigns(new ArrayList<>());
                String imgNameWithExtension = categoryPhoto.getOriginalFilename();
                String imgName = imgNameWithExtension.substring(0, imgNameWithExtension.indexOf('.'));
                Map uploadResult = null;
                try {
                    uploadResult = cloudc.upload(categoryPhoto.getBytes(), ObjectUtils.asMap("public_id",
                            "categories/" + imgName, "resource_type", "auto", "overwrite", true));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                category.setCategoryPhoto(uploadResult.get("url").toString());
                categoryService.insertCategory(category);
                modelAndView = index(null, null, null);
                String successCreateCategoryMessage = messageSource.getMessage("category.successfulCreation",
                                                    null, request.getLocale());
                modelAndView.addObject("successMessage", successCreateCategoryMessage);
                //return new ModelAndView("redirect:" + "/index");
            }
        }
        return modelAndView;
    }

    @RequestMapping(value = "/categoryDetails",params = {"id"}, method = RequestMethod.GET)
    public ModelAndView categoryDetails(@RequestParam("id") String id, HttpServletRequest request){
        ModelAndView modelAndView = new ModelAndView();
        UserAccount currentUserAccount = getCurrentUserAccount();
        modelAndView.addObject("currentUser", currentUserAccount);
        Pair<Boolean, Boolean> pairCheck = checkIfAdminOrUser(currentUserAccount);
        modelAndView.addObject("isAdmin", pairCheck.getKey());
        modelAndView.addObject("isUser", pairCheck.getValue());

        Category category = categoryService.getById(id);
        List<Campaign> campaigns = campaignService.getAllCampaigns();
        campaigns.removeIf(camp -> camp.getCategories().contains(category) == false);
        modelAndView.addObject("categoryCampaigns", campaigns);
        modelAndView.addObject("category", category);
        modelAndView.addObject("numberOfCampaigns", campaigns.size());

        UserAccount categoryCreator = category.getCreator().getUserAccount();
        boolean allowedToDeleteEdit = false;
        boolean isCreator = false;
        if(currentUserAccount != null && categoryCreator.equals(currentUserAccount)){
            isCreator = true;
            if(campaigns.isEmpty()) {
                allowedToDeleteEdit = true;
            }
        }

        modelAndView.addObject("allowedToDeleteEdit", allowedToDeleteEdit);
        modelAndView.addObject("isCreator", isCreator);
        modelAndView.setViewName("categoryDetails");
        return modelAndView;
    }

    @RequestMapping(value = "/deleteCategory", method = RequestMethod.POST)
    public ModelAndView deleteCategory(@RequestParam("categoryId") String categoryId, HttpServletRequest request){
        boolean deleteStatus = true; //true daca s-a sters cu succes fotografia categoriei din Cloudinary
        UserAccount currentUser = getCurrentUserAccount();

        String currentUserId = currentUser.getId();
        Category currentCategory = categoryService.getById(categoryId);
        //stergem poza categoriei din Cloudinary doar daca nu mai este folosita de alte categorii
        String currentCategoryPhoto = currentCategory.getCategoryPhoto();
        boolean ok = true;
        List<Category> allCategories = categoryService.getAllCategories();
        for(Category category : allCategories){
            if(!category.getId().equals(categoryId) && category.getCategoryPhoto().equals(currentCategoryPhoto)){
                ok = false;
                break;
            }
        }

        if(ok == true){
            String publicId = currentCategoryPhoto.substring(currentCategoryPhoto.indexOf("categories"));
            publicId = publicId.substring(0, publicId.indexOf('.'));
            Map deleteMap = cloudc.delete(publicId, ObjectUtils.emptyMap());

            if(!deleteMap.get("result").equals("ok")){
                deleteStatus = false;
            }
        }

        categoryService.deleteCategory(categoryId);
        ModelAndView modelAndView = index(request, null, null);
        if(deleteStatus == true){
            String successDeleteCategoryMessage = messageSource.getMessage("category.successfulDelete", null, request.getLocale());
            modelAndView.addObject("successMessage", successDeleteCategoryMessage);
        }else{
            String failDeleteCategoryMessage = messageSource.getMessage("category.failDelete", null, request.getLocale());
            modelAndView.addObject("errorMessage", failDeleteCategoryMessage);
        }
        modelAndView.addObject("currentUser", currentUser);
        Pair<Boolean, Boolean> pairCheck = checkIfAdminOrUser(currentUser);
        modelAndView.addObject("isAdmin", pairCheck.getKey());
        modelAndView.addObject("isUser", pairCheck.getValue());
        return modelAndView;
    }

    @RequestMapping(value = "/editCategory", method = RequestMethod.POST)
    public ModelAndView editCategory(@RequestParam("photoMultipart") MultipartFile photoMultipart,
                                     @Valid Category category, BindingResult bindingResult, WebRequest request){
        ModelAndView modelAndView;
        UserAccount currentUser = getCurrentUserAccount();

        if(bindingResult.hasErrors()){
            modelAndView = categoryDetails(category.getId(), null);
            modelAndView.addObject("bindingErrorMessages", bindingResult.getAllErrors());
        }else {
            boolean deleteStatus = true;
            Category dbCategory = categoryService.getById(category.getId());
            String dbCategoryPhoto = dbCategory.getCategoryPhoto();
            List<Category> allCategories = categoryService.getAllCategories();

            //daca imaginea categoriei a fost modificata in edit
            if (!photoMultipart.getOriginalFilename().equals("")) {
                //sterge din Cloudinary poza categoriei retinuta in baza de date (care nu mai e folosita la alta categorie

                int ok = 0; // pp ca poza nu mai e folosita la alte categorii
                for (Category cat : allCategories) {
                    if (!cat.getId().equals(category.getId())) {
                        String auxPhoto = cat.getCategoryPhoto();
                        if (auxPhoto.equals(dbCategoryPhoto)) {
                            ok = 1;
                            break;
                        }
                    }
                }

                if (ok == 0) {
                    String publicId = dbCategoryPhoto.substring(dbCategoryPhoto.indexOf("categories"));
                    publicId = publicId.substring(0, publicId.indexOf('.'));
                    Map deleteMap = cloudc.delete(publicId, ObjectUtils.emptyMap());

                    if(!deleteMap.get("result").equals("ok")){
                        deleteStatus = false;
                    }
                }

                //adaugam noua fotografie a categoriei in Cloudinary
                try {
                    String imgNameWithExtension = photoMultipart.getOriginalFilename();
                    String imgName = imgNameWithExtension.substring(0, imgNameWithExtension.indexOf('.'));
                    Map uploadResult = cloudc.upload(photoMultipart.getBytes(), ObjectUtils.asMap("public_id",
                            "categories/" + imgName, "resource_type", "auto", "overwrite", true));

                    dbCategory.setCategoryPhoto(uploadResult.get("url").toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            dbCategory.setCategoryName(category.getCategoryName());
            dbCategory.setDescription(category.getDescription());
            categoryService.updateCategory(dbCategory);

            modelAndView = categoryDetails(category.getId(), null);
            String successEditCategoryMessage = messageSource.getMessage("category.successfulEdit", null, request.getLocale());
            modelAndView.addObject("successMessage", successEditCategoryMessage);
            if(deleteStatus == false){
                String failEditCategoryMessage = messageSource.getMessage("category.failEdit", null, request.getLocale());
                modelAndView.addObject("errorMessage", failEditCategoryMessage);
            }
        }

        modelAndView.addObject("currentUser", currentUser);
        Pair<Boolean, Boolean> pairCheck = checkIfAdminOrUser(currentUser);
        modelAndView.addObject("isAdmin", pairCheck.getKey());
        modelAndView.addObject("isUser", pairCheck.getValue());
        return modelAndView;
    }

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
        UserAccount currentUserAccount = getCurrentUserAccount();
        modelAndView.addObject("currentUser", currentUserAccount);
        Pair<Boolean, Boolean> pairCheck = checkIfAdminOrUser(currentUserAccount);
        modelAndView.addObject("isAdmin", pairCheck.getKey());
        modelAndView.addObject("isUser", pairCheck.getValue());
        modelAndView.setViewName("userRolesDashboard");
        return modelAndView;
    }

    @RequestMapping(value = "/userRolesManagement", method = RequestMethod.POST)
    public ModelAndView userRolesManagement(@RequestParam Map<String,String> allRequestParams, ModelMap model, WebRequest request){

        List<UserAccount> allUsers = userService.getAllUsers();
        for(UserAccount user : allUsers){
            Set<Role> newRoleList = new HashSet<>();

            String userId = user.getId();
            String checkboxUserValue = allRequestParams.get(userId + "user");
            if(checkboxUserValue != null){
                newRoleList.add(roleRepository.findByRole("USER"));
            }
            String checkboxAdminValue = allRequestParams.get(userId + "admin");
            if(checkboxAdminValue != null){
                newRoleList.add(roleRepository.findByRole("ADMIN"));
            }
            //daca adminul a deselectat ambele roluri, utilizatorul sa ramana cu rolul User pe care il avea implicit
            if(checkboxAdminValue == null && checkboxUserValue == null){
                newRoleList.add(roleRepository.findByRole("USER"));
            }
            user.setRoles(newRoleList);
            userService.updateUser(user);
        }
        ModelAndView modelAndView = index(null, null, null);
        String successfulRoleChange = messageSource.getMessage("role.successfullyChanged", null, request.getLocale());
        modelAndView.addObject("successMessage", successfulRoleChange);
        return modelAndView;
    }

    /*@RequestMapping("/carla")
    @ResponseBody
    public String carla(){
        return "Hello carla!";
    } */
}
