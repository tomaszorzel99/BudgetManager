package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.CreateGroupRequest;
import com.personalfinance.BudgetManager.Exception.GroupException;
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
    private final CategoryService categoryService;

    public UserGroupService(UserGroupRepository userGroupRepository, UserService userService,
                            CategoryService categoryService) {
        this.userGroupRepository = userGroupRepository;
        this.userService = userService;
        this.categoryService = categoryService;
    }

    public UserGroup getById(Long id) {
        return userGroupRepository.findById(id)
                .orElseThrow(() -> new GroupException(id));
    }

    public static boolean isMember(User user, UserGroup group) {
        return user.getUserGroups().stream()
                .anyMatch(g -> g.getId() != null && g.getId().equals(group.getId()));
    }

    public void ensureUserIsMember(User user, UserGroup group) {
        if (!isMember(user, group)) {
            throw new AccessDeniedException("User is not member of this group");
        }
    }

    /** Zaproszenia, archiwizacja i usuwanie kont: tylko właściciel grupy. */
    public static void ensureUserIsOwner(User user, UserGroup group) {
        if (!group.isOwnedBy(user.getId())) {
            throw new AccessDeniedException("Only the group owner can do this");
        }
    }

    @Transactional
    public UserGroup createGroup(CreateGroupRequest request) {
        User currentUser = userService.getCurrentUser();

        UserGroup userGroup = new UserGroup();
        userGroup.setName(request.getGroupName());
        userGroup.setOwner(currentUser);
        userGroupRepository.save(userGroup);

        currentUser.getUserGroups().add(userGroup);
        userGroup.getUsers().add(currentUser);

        // Kategorie należą do grupy, więc nowa grupa dostaje podstawowy zestaw na start.
        categoryService.createDefaultCategories(userGroup);
        return userGroup;
    }
}