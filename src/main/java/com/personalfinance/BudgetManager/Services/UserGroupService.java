package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.CreateGroupRequest;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Model.UserGroup;
import com.personalfinance.BudgetManager.Repositories.UserGroupRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserGroupService {

    private final UserGroupRepository userGroupRepository;
    private final UserService userService;

    public UserGroupService(UserGroupRepository userGroupRepository, UserService userService) {
        this.userGroupRepository = userGroupRepository;
        this.userService = userService;
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

    @Transactional
    public UserGroup createGroup(CreateGroupRequest request) {
        User currentUser = userService.getCurrentUser();

        UserGroup userGroup = new UserGroup();
        userGroup.setName(request.getGroupName());
        userGroupRepository.save(userGroup);

        currentUser.getUserGroups().add(userGroup);
        userGroup.getUsers().add(currentUser);

        return userGroup;
    }
}
