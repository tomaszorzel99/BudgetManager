package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;


@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                true,
                true,
                true,
                Collections.emptyList());
    }
}


//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