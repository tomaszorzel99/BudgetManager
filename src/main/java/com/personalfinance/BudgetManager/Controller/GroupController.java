package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.DTO.CreateGroupRequest;
import com.personalfinance.BudgetManager.DTO.GroupDTO;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Model.UserGroup;
import com.personalfinance.BudgetManager.Services.UserGroupService;
import com.personalfinance.BudgetManager.Services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GroupController {

    private final UserService userService;
    private final UserGroupService userGroupService;

    public GroupController(UserService userService, UserGroupService userGroupService) {
        this.userService = userService;
        this.userGroupService = userGroupService;
    }

    @PostMapping("/groups")
    public ResponseEntity<GroupDTO> createGroup(@RequestBody CreateGroupRequest request) {
        userGroupService.createGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
