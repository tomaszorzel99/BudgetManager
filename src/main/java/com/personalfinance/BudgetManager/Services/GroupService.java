package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.Exception.UserException;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Model.UserGroup;
import com.personalfinance.BudgetManager.Repositories.UserGroupRepository;
import com.personalfinance.BudgetManager.Repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class GroupService {

    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;

    public GroupService(UserGroupRepository userGroupRepository, UserRepository userRepository) {
        this.userGroupRepository = userGroupRepository;
        this.userRepository = userRepository;
    }

    public UserGroup createGroup(String groupName) {
        UserGroup userGroup = new UserGroup();
        userGroup.setName(groupName);
        return userGroupRepository.save(userGroup);
    }

    @Transactional
    public boolean isUserInGroup(String userEmail, Long groupId) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new UserException(userEmail));
        return user.getUserGroups().stream().anyMatch(group -> group.getId().equals(groupId));

    }
}
