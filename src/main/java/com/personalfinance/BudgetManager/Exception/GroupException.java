package com.personalfinance.BudgetManager.Exception;

public class GroupException extends RuntimeException{
    public GroupException(Long groupId){
        super("Group not found with id: " + groupId);
    }
}
