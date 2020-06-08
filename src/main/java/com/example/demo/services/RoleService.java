package com.example.demo.services;

import com.example.demo.classes.Role;
import com.example.demo.classes.UserAccount;
import com.example.demo.repositories.RoleRepository;
import javafx.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class RoleService {
    @Autowired
    private RoleRepository roleRepository;

    public void clearRoleCollection(){
        roleRepository.deleteAll();
    }

    public void insertRole(Role role){
        roleRepository.insert(role);
    }

    public Role findByRole(String role){ return roleRepository.findByRole(role); }

    public void insertMultipleRoles(List<Role> roles){
        roles.forEach((role) -> {
            roleRepository.insert(role);
        });
    }

    public List<Role> getRoles() {
        return roleRepository.findAll();
    }

    //metoda ce verifica daca un cont de utilizator contine rolurile Admin sau User
    public Pair<Boolean, Boolean> checkIfAdminOrUser(UserAccount user){
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
}
