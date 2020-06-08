package com.example.demo.controllers;

import com.example.demo.classes.*;
import com.example.demo.services.CampaignServiceImpl;
import com.example.demo.services.DonationService;
import com.example.demo.services.ProfileService;
import com.example.demo.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

@Controller
public class DonationController {
    @Autowired
    private UserService userService;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private CampaignServiceImpl campaignService;
    @Autowired
    private DonationService donationService;

    private final PayPalClient payPalClient;
    //detalii pentru donatia curenta
    private String currentCampaignId;
    private String currentAmount;

    @Autowired
    DonationController(PayPalClient payPalClient) {
        this.payPalClient = payPalClient;
    }

    //metoda de donatie cu PayPal
    @RequestMapping(value = "/donate", method = RequestMethod.POST)
    public ModelAndView donate(@RequestParam("amountOfMoney") String amountOfMoney, @RequestParam("campaignId") String campaignId,
                               WebRequest request, RedirectAttributes redirectAttributes) {

        //userul nu poate efectua nicio donatie daca nu are completat profilul sau
        UserAccount currentUserAccount = userService.getCurrentUserAccount();
        if(profileService.getByUserAccount(currentUserAccount) == null){
            String cantMakeDonationWithoutProfile = messageSource.getMessage("campaign.cantDonateWithoutProfile", null, request.getLocale());
            redirectAttributes.addAttribute("errorMessage", cantMakeDonationWithoutProfile);
            return new ModelAndView("redirect:/myProfile");
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
    public ModelAndView completePayment(HttpServletRequest request, RedirectAttributes redirectAttributes){
        Map<String, Object> result = payPalClient.completePayment(request);
        UserAccount currentUserAccount = userService.getCurrentUserAccount();
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

        //modelAndView.addObject("payPalTransactionStatus", status);
        String successfulDonationMessage = messageSource.getMessage("campaign.successfulDonation", null, request.getLocale());
        redirectAttributes.addAttribute("successMessage", successfulDonationMessage);
        return new ModelAndView("redirect:/index");
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
}
