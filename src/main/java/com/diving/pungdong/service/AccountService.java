package com.diving.pungdong.service;

import com.diving.pungdong.advice.exception.CEmailSigninFailedException;
import com.diving.pungdong.advice.exception.CUserNotFoundException;
import com.diving.pungdong.advice.exception.EmailDuplicationException;
import com.diving.pungdong.config.security.UserAccount;
import com.diving.pungdong.controller.sign.SignController;
import com.diving.pungdong.domain.account.Account;
import com.diving.pungdong.domain.account.InstructorImgCategory;
import com.diving.pungdong.domain.account.Role;
import com.diving.pungdong.repo.AccountJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.diving.pungdong.controller.sign.SignController.AddInstructorRoleReq;
import static com.diving.pungdong.controller.sign.SignController.SignInReq;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService implements UserDetailsService {
    private final AccountJpaRepo accountJpaRepo;
    private final RedisTemplate<String, String> redisTemplate;
    private final InstructorImageService instructorImageService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
        Account account = accountJpaRepo.findById(Long.valueOf(id)).orElseThrow(CUserNotFoundException::new);
        return new UserAccount(account);
    }

    public Account saveAccount(Account account) {
        return accountJpaRepo.save(account);
    }

    public Account findAccountByEmail(String email) {
        return accountJpaRepo.findByEmail(email).orElseThrow(CEmailSigninFailedException::new);
    }

    public Account findAccountById(Long id) {
        return accountJpaRepo.findById(id).orElseThrow(CUserNotFoundException::new);
    }

    public String checkValidToken(String token) {
        return redisTemplate.opsForValue().get(token);
    }

    public Account updateAccountToInstructor(String email,
                                             AddInstructorRoleReq request,
                                             List<MultipartFile> profiles,
                                             List<MultipartFile> certificates) throws IOException {
        Account account = accountJpaRepo.findByEmail(email).orElseThrow(CEmailSigninFailedException::new);
        account.setPhoneNumber(request.getPhoneNumber());
        account.setGroupName(request.getGroupName());
        account.setDescription(request.getDescription());
        account.getRoles().add(Role.INSTRUCTOR);

        Account updateAccount = accountJpaRepo.save(account);

        instructorImageService.uploadInstructorImages(email, profiles, updateAccount, "profile", InstructorImgCategory.PROFILE);
        instructorImageService.uploadInstructorImages(email, certificates, updateAccount, "certificate", InstructorImgCategory.CERTIFICATE);

        return updateAccount;
    }

    public void checkDuplicationOfEmail(String email) {
        Optional<Account> account = accountJpaRepo.findByEmail(email);
        if (account.isPresent()) {
            throw new EmailDuplicationException();
        }
    }

    public void checkCorrectPassword(SignInReq signInReq, Account account) {
        if (!passwordEncoder.matches(signInReq.getPassword(), account.getPassword())) {
            throw new CEmailSigninFailedException();
        }
    }
}
