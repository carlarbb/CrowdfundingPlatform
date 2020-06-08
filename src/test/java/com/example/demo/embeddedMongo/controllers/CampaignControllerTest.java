package com.example.demo.embeddedMongo.controllers;

import com.example.demo.classes.*;
import com.example.demo.configuration.SystemProfileValueSource2;
import com.example.demo.controllers.CampaignController;
import com.example.demo.services.*;
import javafx.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.Assert.assertEquals;
import static org.springframework.core.env.AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME;

@ProfileValueSourceConfiguration(value = SystemProfileValueSource2.class)
@IfProfileValue(name = ACTIVE_PROFILES_PROPERTY_NAME, value = "mongo-embedded") //"spring.profiles.active"
@RunWith(SpringRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS) //close and recreate ApplicationContext
@DataMongoTest
public class CampaignControllerTest {

    @TestConfiguration
    static class CampaignControllerTestContextConfiguration {

        @Bean
        public CampaignController campaignController() {
            return new CampaignController();
        }
    }

    @Autowired
    CampaignController campaignController;

    @Mock
    private ModelMap model;
    @MockBean
    private CampaignServiceImpl campaignService;
    @MockBean
    private DonationService donationService;
    @MockBean
    private CategoryService categoryService;
    @MockBean
    private MessageSource messageSource;
    @MockBean
    private UserService userService;
    @MockBean
    private ProfileService profileService;
    @MockBean
    private CloudinaryService cloudinaryService;
    @MockBean
    private RoleService roleService;

    static String campaignId;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        campaignId = "5d1321dcdb68c43234b449a9";
    }

    @Test
    public void testShowCampaignDetails(){
        Campaign campaign = new Campaign("TestCampaign", "Romania", "Bucharest", "This is a test campaign",
                "", 100, new Date(), 50);
        Calendar current = Calendar.getInstance();
        Role userRole = new Role("USER");
        UserAccount currentUserAccount = new UserAccount("Ana", "Popescu", "testpass",
                "test@gmail.com", new HashSet<>(Arrays.asList(userRole)));
        Profile creator = new Profile(currentUserAccount, "Bucharest", "Romania", "MyDescription", "");
        campaign.setCreator(creator);
        campaign.setId(campaignId);
        Category category1 = new Category("TestCategory1", creator, new Date(), "Category1");
        Category category2 = new Category("TestCategory2", creator, new Date(), "Category2");
        campaign.setCategories(Arrays.asList(category1, category2));

        when(userService.getCurrentUserAccount()).thenReturn(currentUserAccount);
        when(roleService.checkIfAdminOrUser(currentUserAccount)).thenReturn(new Pair<>(false, true));
        when(campaignService.getById(campaignId)).thenReturn(campaign);
        when(campaignService.checkIfCampaignIsCompleted(campaign, current)).thenReturn(50L);
        when(donationService.getByCampaign(campaign)).thenReturn(new ArrayList<>());
        when(categoryService.getAllCategories()).thenReturn(Arrays.asList(category1, category2));

        ModelAndView returnedModelAndView = campaignController.campaignDetails(model, campaignId, null, null);

        assertEquals("campaignDetails", returnedModelAndView.getViewName());

        verify(userService, times(1)).getCurrentUserAccount();
        verify(roleService, times(1)).checkIfAdminOrUser(currentUserAccount);
        verify(campaignService, times(1)).getById(campaignId);
        verify(donationService, times(1)).getByCampaign(campaign);
        verify(categoryService, times(1)).getAllCategories();

        ArgumentCaptor<Campaign> argumentCaptor = ArgumentCaptor.forClass(Campaign.class);
        verify(model, times(1)).put(eq("campaign"), argumentCaptor.capture());
        Campaign campaignArg = argumentCaptor.getValue();
        assertEquals(campaign.getId(), campaignArg.getId());
    }
}
