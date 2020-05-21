package com.example.demo.services;

import com.example.demo.classes.Role;
import com.example.demo.repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public void insertMultipleRoles(List<Role> roles){
        roles.forEach((role) -> {
            roleRepository.insert(role);
        });
    }

    public List<Role> getRoles() {
        return roleRepository.findAll();
    }
}
