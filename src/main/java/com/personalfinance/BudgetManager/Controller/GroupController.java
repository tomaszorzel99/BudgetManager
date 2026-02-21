package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.DTO.CreateGroupRequest;
import com.personalfinance.BudgetManager.DTO.GroupDTO;
import com.personalfinance.BudgetManager.Services.UserGroupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GroupController {

    private final UserGroupService userGroupService;

    public GroupController(UserGroupService userGroupService) {
        this.userGroupService = userGroupService;
    }

    @PostMapping("/groups")
    public ResponseEntity<GroupDTO> createGroup(@RequestBody CreateGroupRequest request) {
        userGroupService.createGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
