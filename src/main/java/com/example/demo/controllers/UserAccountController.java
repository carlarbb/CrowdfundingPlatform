package com.example.demo.controllers;

import com.example.demo.repositories.UserAccountRepository;
import com.example.demo.classes.UserAccount;
import com.example.demo.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserAccountController {
    @Autowired
    private UserService userService;

    @GetMapping("/all")
    public List<UserAccount>getAll(){
        return this.userService.getAllUsers();
    }

    @PostMapping
    public void insert(@RequestBody UserAccount userAccount){
        this.userService.insertUser(userAccount);
    }
    @PutMapping
    public void update(@RequestBody UserAccount userAccount){this.userService.updateUser(userAccount);
    }
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") String id){
        this.userService.deleteUser(id);
    }

    @GetMapping("/{id}")
    public UserAccount getById(@PathVariable("id") String id){
        return userService.getById(id);
    }

    @GetMapping("/firstName/{firstName}")
    public List<UserAccount> getByFirstName(@PathVariable("firstName") String firstName){
       return userService.getByFirstName(firstName);
    }
}
