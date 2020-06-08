package com.example.demo.embeddedMongo.services;

import com.example.demo.classes.Campaign;
import com.example.demo.configuration.SystemProfileValueSource2;
import com.example.demo.interfaces.CampaignService;
import com.example.demo.repositories.CampaignRepository;
import com.example.demo.services.CampaignServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.*;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.core.env.AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME;

@ProfileValueSourceConfiguration(value = SystemProfileValueSource2.class)
@IfProfileValue(name = ACTIVE_PROFILES_PROPERTY_NAME, value = "mongo-embedded") //"spring.profiles.active"
@RunWith(SpringRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS) //close and recreate ApplicationContext
@DataMongoTest
public class CampaignServiceImplTest {
    private Campaign campaign;

    //components and configuration specific for this test
    @TestConfiguration
    static class CampaignServiceImplTestContextConfiguration {

        @Bean
        public CampaignService campaignService() {
            return new CampaignServiceImpl();
        }
    }

    @Autowired
    private CampaignService campaignService;

    @MockBean
    private CampaignRepository campaignRepository;

    @Before
    public void setUp(){
        campaign = new Campaign("TestCampaign", "Romania", "Bucharest", "This is a test campaign",
                "", 100, new Date(), 50);
        when(campaignRepository.findAll()).thenReturn(Arrays.asList(campaign));
    }

    @Test
    public void checkGetAllCampaigns(){
        List<Campaign> allCampaigns = campaignService.getAllCampaigns();
        assertEquals(1, allCampaigns.size());
        assertEquals("TestCampaign", allCampaigns.get(0).getTitle());
        verify(campaignRepository, times(1)).findAll();
    }

    @Test
    public void checkIfCampaignIsCompleted(){
        Calendar current = Calendar.getInstance();
        long daysToBeCompleted = campaignService.checkIfCampaignIsCompleted(campaign, current);
        assertThat(daysToBeCompleted, greaterThan(0L));
    }
}
