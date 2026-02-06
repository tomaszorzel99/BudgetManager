package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Model.UserGroup;
import com.personalfinance.BudgetManager.Repositories.UserGroupRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class UserGroupService {

    private final UserGroupRepository userGroupRepository;

    public UserGroupService(UserGroupRepository userGroupRepository) {
        this.userGroupRepository = userGroupRepository;
    }

    public UserGroup getById(Long id) {
        return userGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));
    }

    public void ensureUserIsMember(User user, UserGroup group) {
        if (!user.getUserGroups().contains(group)) {
            throw new AccessDeniedException("User is not member of this group");
        }
    }
}
