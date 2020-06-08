package com.example.demo.embeddedMongo.controllers;

import com.example.demo.classes.Campaign;
import com.example.demo.configuration.SystemProfileValueSource2;
import com.example.demo.controllers.CampaignController;
import com.example.demo.interfaces.CampaignService;
import com.example.demo.repositories.CampaignRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Date;

import static org.springframework.core.env.AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ProfileValueSourceConfiguration(value = SystemProfileValueSource2.class)
@IfProfileValue(name = ACTIVE_PROFILES_PROPERTY_NAME, value = "mongo-embedded") //"spring.profiles.active"
@RunWith(SpringRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS) //close and recreate ApplicationContext
@DataMongoTest
public class CampaignControllerTestIntegration {

    @Autowired
    private CampaignController campaignController;

    @Autowired
    private CampaignRepository campaignRepository;

    static String campaignId;

    @Before
    public void setUp(){
        Campaign campaign = new Campaign("TestCampaign", "Romania", "Bucharest", "This is a test campaign",
                "", 100, new Date(), 50);
        campaignRepository.save(campaign);
        campaignId = campaign.getId();
    }

    @Test
    public void testShowCampaignDetailsMvcNotFound() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(campaignController).build();
        mockMvc.perform(get("/campaignDetails").param("id", campaignId + 1))
                .andExpect(status().isNotFound())
                .andExpect(view().name("notfound"));
    }

    @Test
    public void testShowCampaignDetailsMvcOk() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(campaignController).build();
        mockMvc.perform(get("/campaignDetails").param("id", campaignId))
                .andExpect(status().isOk())
                .andExpect(view().name("campaignDetails"));
    }
}
