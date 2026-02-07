package com.personalfinance.BudgetManager.Services;

import com.personalfinance.BudgetManager.DTO.CreateAccountRequest;
import com.personalfinance.BudgetManager.Exception.AccountException;
import com.personalfinance.BudgetManager.Exception.UserException;
import com.personalfinance.BudgetManager.Model.Account;
import com.personalfinance.BudgetManager.Model.User;
import com.personalfinance.BudgetManager.Model.UserGroup;
import com.personalfinance.BudgetManager.Repositories.AccountRepository;
import com.personalfinance.BudgetManager.Repositories.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class AccountService {


    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository, UserService userService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    public Account createAccount(CreateAccountRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException(email));

        UserGroup group = user.getUserGroups().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User has no group"));

        Account account = new Account();
        account.setName(request.getName());
        account.setAccountNumber(request.getAccountNumber());
        account.setCurrency(request.getCurrency());
        account.setBalance(request.getBalance());
        account.setGroup(group);
        return accountRepository.save(account);
    }

//    public List<Account> getAccountsByUserEmail(String userEmail) {
//        return accountRepository.findByUserEmail(userEmail);
//    }

    public List<Account> getAccountsVisibleForUser(User user){
        Set<UserGroup> userGroups = user.getUserGroups();
        return accountRepository.findAllByGroupIn(userGroups);
    }

//    public Account updateAccount(Long id, UpdateAccountRequest request, String email) throws AccessDeniedException {
//        Account account = accountRepository.findById(id)
//                .orElseThrow(() -> new AccountException(id));
//        if(!account.getUser().getEmail().equals(email)){
//            throw new AccessDeniedException("You are not owner of this account");
//        }
//        updateExistingAccountWithPatch(account, request);
//        return accountRepository.save(account);
//    }

    public void deleteAccountById(Long id) throws AccessDeniedException {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountException(id));
        User currentUser = userService.getCurrentUser();

        if (!account.getGroup().getUsers().contains(currentUser)) {
            throw new AccessDeniedException("You are not owner of this account");
        }

        accountRepository.delete(account);
    }

//    public void updateExistingAccountWithPatch(Account existingAccount, UpdateAccountRequest request){
//        if(request.getName() != null) existingAccount.setName(request.getName());
//        if(request.getAccountNumber() != null) {
//            if (accountRepository.existsByAccountNumberAndIdNot(request.getAccountNumber(), existingAccount.getId())) {
//                throw new AccountException(request.getAccountNumber());
//            }
//            existingAccount.setAccountNumber(request.getAccountNumber());
//        }
//        if(request.getCurrency() != null) existingAccount.setCurrency(request.getCurrency());
//        if(request.getBalance() != null) existingAccount.setBalance(request.getBalance());
//    }
}
