package com.example.demo.embeddedMongo.repositories;

import static org.junit.Assert.*;
import static org.springframework.core.env.AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME;

import com.example.demo.classes.Role;
import com.example.demo.classes.UserAccount;
import com.example.demo.configuration.SystemProfileValueSource2;
import com.example.demo.repositories.RoleRepository;
import com.example.demo.repositories.UserAccountRepository;
import com.mongodb.client.MongoCollection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.Optional;

@ProfileValueSourceConfiguration(value = SystemProfileValueSource2.class)
@IfProfileValue(name = ACTIVE_PROFILES_PROPERTY_NAME, value = "mongo-embedded") //"spring.profiles.active"
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@RunWith(SpringRunner.class)
@DataMongoTest
public class RepositoryTest {

    private String collectionName;
    private Role roleToInsert;
    private UserAccount userToInsert;
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Before
    public void before() {
        collectionName = "testRole";
        roleToInsert = new Role("ROLETEST");
        bCryptPasswordEncoder = new BCryptPasswordEncoder();
        userToInsert = new UserAccount("Ana", "Popescu", "testpass", "test@gmail.com",
                      null);
    }

    @After
    public void after() {
        mongoTemplate.dropCollection(collectionName);
    }


    @Test
    public void checkMongoTemplate() {
        assertNotNull(mongoTemplate);
        MongoCollection createdCollection = mongoTemplate.createCollection(collectionName);
        assertTrue(mongoTemplate.collectionExists(collectionName));
    }

    @Test
    public void checkDocumentAndQuery() {
        mongoTemplate.save(roleToInsert, collectionName);
        Query query = new Query(new Criteria()
                .andOperator(Criteria.where("role").regex(roleToInsert.getRole())));

        Role retrievedRole = mongoTemplate.findOne(query, Role.class, collectionName);
        assertNotNull(retrievedRole);
    }

    @Test
    public void checkRoleRepository() {
        assertNotNull(roleRepository);
        Role savedRole = roleRepository.save(roleToInsert);
        assertNotNull(roleRepository.findById(savedRole.getId()));
    }

    @Test
    public void checkUserAccountRepository(){
        assertNotNull(userAccountRepository);
        userToInsert.setPassword(bCryptPasswordEncoder.encode(userToInsert.getPassword()));
        UserAccount savedUser = userAccountRepository.save(userToInsert);
        Optional<UserAccount> retrievedUser = userAccountRepository.findById(savedUser.getId());
        assertNotNull(retrievedUser);
        UserAccount retrievedUserAccount = retrievedUser.get();
        assertEquals(0, retrievedUserAccount.getRoles().size());
        assertEquals("test@gmail.com", retrievedUserAccount.getEmail());
        assertEquals("Ana", retrievedUserAccount.getFirstName());
        assertEquals("Popescu", retrievedUserAccount.getLastName());
    }
}
