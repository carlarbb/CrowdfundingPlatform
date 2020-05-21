package com.example.demo.services;

import com.example.demo.classes.CustomUserDetails;
import com.example.demo.classes.Role;
import com.example.demo.classes.UserAccount;
import com.example.demo.repositories.RoleRepository;
import com.example.demo.repositories.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;

@Service
public class UserService implements UserDetailsService{
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    public UserService() {}

    public List<UserAccount> getAllUsers() {
        List<UserAccount> userAccounts = this.userAccountRepository.findAll();
        return userAccounts;
    }

    //saves userAccount with multiple roles received as params
    public void save(UserAccount userAccount, Set<Role> userRoles){
        userAccount.setPassword(bCryptPasswordEncoder.encode(userAccount.getPassword()));
        userAccount.setEnabled(false);
        userAccount.setRoles(userRoles);
        this.userAccountRepository.insert(userAccount);
    }

    //inserare UserAccount in baza de date cu rol de user
   public void insertUser(UserAccount userAccount){
        Role userRole = roleRepository.findByRole("USER");
        save(userAccount, new HashSet<>(Arrays.asList(userRole)));
    }

    //inserare UserAccount in baza de date cu rol de admin
    public void insertAdmin(UserAccount userAccount){
        Role userRole = roleRepository.findByRole("ADMIN");
        save(userAccount, new HashSet<>(Arrays.asList(userRole)));
    }

    public void updateUser(UserAccount userAccount){this.userAccountRepository.save(userAccount);}
    public void deleteUser(String id){
        this.userAccountRepository.deleteById(id);
    }

    public UserAccount getById(String id){
        UserAccount userAccount = this.userAccountRepository.findById(id).get();
        return userAccount;
    }
    public List<UserAccount> getByFirstName(String firstName){
        List<UserAccount> userAccounts = this.userAccountRepository.findByFirstName(firstName);
        return userAccounts;
    }

    public UserAccount getByEmail(String email){
        UserAccount userAccount = this.userAccountRepository.findByEmail(email);
        return userAccount;
    }

    public UserAccount getByTokenId(String tokenId){
        UserAccount userAccount = this.userAccountRepository.findByTokenId(tokenId);
        return userAccount;
    }

    //pt mecanismul de login; verifica si compara username-ul(email) cu user-ul din colectia mongodb
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        UserAccount user = userAccountRepository.findByEmail(email);
        user.getRoles().forEach((p)-> System.out.println(p));
        if(user != null && user.isEnabled() == true) {
            List<GrantedAuthority> authorities = getUserAuthority(user.getRoles());
            return buildUserForAuthentication(user, authorities);
        } else {
            throw new UsernameNotFoundException("username not found");
        }
    }

    //converting the user roles as GrantedAuthority collection
    private List<GrantedAuthority> getUserAuthority(Set<Role> userRoles) {
        Set<GrantedAuthority> roles = new HashSet<>();
        userRoles.forEach((role) -> {
            roles.add(new SimpleGrantedAuthority(role.getRole()));
        });

        List<GrantedAuthority> grantedAuthorities = new ArrayList<>(roles);
        return grantedAuthorities;
    }

    //connecting MongoDB user to Spring Security user as called from the `loadUserByUsername` method
    private UserDetails buildUserForAuthentication(UserAccount user, List<GrantedAuthority> authorities) {
       // return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), authorities);
        return new CustomUserDetails(user, authorities);
    }

    public void clearUserCollection(){
        userAccountRepository.deleteAll();
    }
}
