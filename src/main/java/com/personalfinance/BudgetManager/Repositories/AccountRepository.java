package com.personalfinance.BudgetManager.Repositories;

import com.personalfinance.BudgetManager.Model.Account;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Model.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
//    List<Account> findByUserEmail(String email);
//    boolean existsByAccountNumberAndIdNot(String accountNumber, Long id);
    List<Account> findAllByGroupIn(Set<UserGroup> groups);

}
