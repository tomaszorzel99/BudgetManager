package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.DTO.CreateGroupRequest;
import com.personalfinance.BudgetManager.DTO.UserGroupDTO;
import com.personalfinance.BudgetManager.Model.UserGroup;
import com.personalfinance.BudgetManager.Services.UserGroupService;
import jakarta.validation.Valid;
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

    /** Zwraca utworzoną grupę ({@code id}, {@code name}), więc front nie musi dociągać /api/users/me. */
    @PostMapping("/groups")
    public ResponseEntity<UserGroupDTO> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        UserGroup group = userGroupService.createGroup(request);
        UserGroupDTO dto = new UserGroupDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}