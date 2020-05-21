package com.example.demo.repositories;

import com.example.demo.classes.UserAccount;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAccountRepository extends MongoRepository<UserAccount, String>{
    List<UserAccount> findByFirstName(String firstName);
    UserAccount findByEmail(String email);
    UserAccount findByTokenId(String tokenId);
}
