package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Model.UserGroup;
import com.personalfinance.BudgetManager.Repositories.UserGroupRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class GroupService {

    private final UserGroupRepository userGroupRepository;

    public GroupService(UserGroupRepository userGroupRepository) {
        this.userGroupRepository = userGroupRepository;
    }

    public UserGroup createGroup(String groupName) {
        UserGroup userGroup = new UserGroup();
        userGroup.setName(groupName);
        return userGroupRepository.save(userGroup);
    }
}
