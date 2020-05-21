package com.example.demo;

import com.example.demo.classes.Role;
import com.example.demo.classes.UserAccount;
import com.example.demo.repositories.RoleRepository;
import com.example.demo.repositories.UserAccountRepository;
import com.example.demo.services.RoleService;
import com.example.demo.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DbSeeder implements CommandLineRunner{
    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    /*
    public DbSeeder(UserAccountRepository userAccountRepository, RoleRepository roleRepository) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
    }
    */

    @Override
    public void run(String... strings) throws Exception{
        //verific daca in bd nu exista adaugate rolurile ADMIN si USER (atunci le adaug, o singura data)
        int okAdmin = 0, okUser = 0;

        List<Role> allRoles = this.roleService.getRoles();
        for(Role role : allRoles){
            String roleName = role.getRole();
            if(roleName.equals("ADMIN")) okAdmin = 1;
            else if(roleName.equals("USER")) okUser = 1;
        }

        if(okAdmin == 0 && okUser == 0) {
            Role adminRole = new Role("ADMIN");
            Role userRole = new Role("USER");
            List<Role> roles = Arrays.asList(adminRole, userRole);
            roleService.insertMultipleRoles(roles);
        }

        //daca in baza de date nu exista UserAccount-ul primului admin, atunci il creez
        UserAccount firstAdminUser = userService.getByEmail("test.springboot24@gmail.com");
        if(firstAdminUser == null){
            firstAdminUser = new UserAccount(
                    "Admin", "Springboot", "pass1crowdfunding", "test.springboot24@gmail.com", null);
            userService.insertAdmin(firstAdminUser);
            firstAdminUser.setEnabled(true);
            userService.updateUser(firstAdminUser);
        }
    }
}
