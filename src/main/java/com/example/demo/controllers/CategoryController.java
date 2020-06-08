package com.example.demo.controllers;

import com.example.demo.classes.Campaign;
import com.example.demo.classes.Category;
import com.example.demo.classes.Profile;
import com.example.demo.classes.UserAccount;
import com.example.demo.services.*;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

@Controller
public class CategoryController {
    @Autowired
    private UserService userService;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private CloudinaryService cloudinaryService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private CampaignServiceImpl campaignService;
    @Autowired
    private RoleService roleService;


    @RequestMapping(value = "/addCategory", method = RequestMethod.POST)
    public ModelAndView createCategory(@Valid Category category, @RequestParam("photoMultipart") MultipartFile categoryPhoto,
                                       WebRequest request, RedirectAttributes redirectAttributes){

        ModelAndView modelAndView;
        UserAccount currentUserAccount = userService.getCurrentUserAccount();

        boolean okUploadPhoto = true;
        if(categoryPhoto.getOriginalFilename().equals("")) okUploadPhoto = false;
        boolean okCategoryName = true;
        if(category.getCategoryName().equals("")) okCategoryName = false;

        if(okCategoryName == false || okUploadPhoto == false){
            if(okCategoryName == false) {
                String categoryNameNotNullMessage = messageSource.getMessage("category.nameNotNull", null, request.getLocale());
                redirectAttributes.addAttribute("errorMessage", categoryNameNotNullMessage);
            }
            else {
                String categoryImageNotNullMessage = messageSource.getMessage("category.imageNotNull", null, request.getLocale());
                redirectAttributes.addAttribute("errorMessage", categoryImageNotNullMessage);
            }
            return new ModelAndView("redirect:/index");
        } else {
            Profile profile = profileService.getByUserAccount(currentUserAccount);
            if (profile == null) { //nu poti crea o categorie fara a avea completat profilul
                String cantCreateCategoryWithoutProfileMessage = messageSource.getMessage("category.cantCreateWithoutProfile",
                        null, request.getLocale());
                redirectAttributes.addAttribute("errorMessage", cantCreateCategoryWithoutProfileMessage);
                return new ModelAndView("redirect:" + "/myProfile");
            }
            else {
                Calendar c = Calendar.getInstance();
                category.setCreator(profile);
                category.setDate(c.getTime());

                Map uploadResult = null;
                try {
                    uploadResult = cloudinaryService.addCategoryImageToCloudinary(categoryPhoto);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                category.setCategoryPhoto(uploadResult.get("url").toString());
                categoryService.insertCategory(category);
                String successCreateCategoryMessage = messageSource.getMessage("category.successfulCreation",
                        null, request.getLocale());
                redirectAttributes.addAttribute("successMessage", successCreateCategoryMessage);
                return new ModelAndView("redirect:" + "/index");
            }
        }
        //   return modelAndView;
    }

    @RequestMapping(value = "/categoryDetails",params = {"id"}, method = RequestMethod.GET)
    public ModelAndView categoryDetails(@RequestParam("id") String id, HttpServletRequest request){
        ModelAndView modelAndView = new ModelAndView();
        UserAccount currentUserAccount = userService.getCurrentUserAccount();
        modelAndView.addObject("currentUser", currentUserAccount);
        Pair<Boolean, Boolean> pairCheck = roleService.checkIfAdminOrUser(currentUserAccount);
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
    public ModelAndView deleteCategory(@RequestParam("categoryId") String categoryId, HttpServletRequest request,
                                       RedirectAttributes redirectAttributes){
        boolean deleteStatus = true; //true daca s-a sters cu succes fotografia categoriei din Cloudinary
        UserAccount currentUserAccount = userService.getCurrentUserAccount();

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
            Map deleteMap = cloudinaryService.deleteImageByIdFromCloudinary(publicId);

            if(!deleteMap.get("result").equals("ok")){
                deleteStatus = false;
            }
        }

        categoryService.deleteCategory(categoryId);
        if(deleteStatus == true){
            String successDeleteCategoryMessage = messageSource.getMessage("category.successfulDelete", null, request.getLocale());
            redirectAttributes.addAttribute("successMessage", successDeleteCategoryMessage);
        }else{
            String failDeleteCategoryMessage = messageSource.getMessage("category.failDelete", null, request.getLocale());
            redirectAttributes.addAttribute("errorMessage", failDeleteCategoryMessage);
        }
        return new ModelAndView("redirect:/index");
    }

    @RequestMapping(value = "/editCategory", method = RequestMethod.POST)
    public ModelAndView editCategory(@RequestParam("photoMultipart") MultipartFile photoMultipart,
                                     @Valid Category category, BindingResult bindingResult, WebRequest request){
        ModelAndView modelAndView;
        UserAccount currentUserAccount = userService.getCurrentUserAccount();

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
                    Map deleteMap = cloudinaryService.deleteImageByIdFromCloudinary(publicId);

                    if(!deleteMap.get("result").equals("ok")){
                        deleteStatus = false;
                    }
                }

                //adaugam noua fotografie a categoriei in Cloudinary
                try {
                    Map uploadResult = cloudinaryService.addCategoryImageToCloudinary(photoMultipart);
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

        modelAndView.addObject("currentUser", currentUserAccount);
        Pair<Boolean, Boolean> pairCheck = roleService.checkIfAdminOrUser(currentUserAccount);
        modelAndView.addObject("isAdmin", pairCheck.getKey());
        modelAndView.addObject("isUser", pairCheck.getValue());
        return modelAndView;
    }
}
