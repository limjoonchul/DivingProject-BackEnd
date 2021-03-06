package com.diving.pungdong.controller.sign;

import com.diving.pungdong.advice.exception.CEmailSigninFailedException;
import com.diving.pungdong.config.EmbeddedRedisConfig;
import com.diving.pungdong.config.RestDocsConfiguration;
import com.diving.pungdong.config.security.JwtTokenProvider;
import com.diving.pungdong.domain.account.Account;
import com.diving.pungdong.domain.account.Gender;
import com.diving.pungdong.domain.account.Role;
import com.diving.pungdong.dto.auth.AuthToken;
import com.diving.pungdong.service.AccountService;
import com.diving.pungdong.service.AuthService;
import com.diving.pungdong.service.kafka.AccountKafkaProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static com.diving.pungdong.controller.sign.SignController.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureRestDocs
@Import({RestDocsConfiguration.class, EmbeddedRedisConfig.class})
@Transactional
class SignControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @MockBean
    AccountService accountService;

    @MockBean
    AuthService authService;

    @MockBean
    AccountKafkaProducer producer;

    public Account createAccount(Role role) {
        Account account = Account.builder()
                .id(1L)
                .email("yechan@gmail.com")
                .password("1234")
                .userName("yechan")
                .age(27)
                .gender(Gender.MALE)
                .roles(Set.of(role))
                .build();

        given(accountService.loadUserByUsername(String.valueOf(account.getId())))
                .willReturn(new User(account.getEmail(), account.getPassword(), authorities(account.getRoles())));

        return account;
    }

    @Test
    @DisplayName("???????????? ?????? - ????????? ??????????????? ?????????")
    public void signupInstructorSuccess() throws Exception {
        SignUpReq signUpReq = SignUpReq.builder()
                .email("yechan@gmail.com")
                .password("1234")
                .userName("yechan")
                .age(24)
                .gender(Gender.MALE)
                .build();

        mockMvc.perform(post("/sign/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaTypes.HAL_JSON)
                .content(objectMapper.writeValueAsString(signUpReq)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("email").exists())
                .andExpect(jsonPath("userName").exists())
                .andDo(document("signUp",
                        requestHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("JSON ??????")
                        ),
                        requestFields(
                                fieldWithPath("email").description("?????? ID"),
                                fieldWithPath("password").description("?????? PASSWORD"),
                                fieldWithPath("userName").description("????????? ??????"),
                                fieldWithPath("age").description("????????? ??????"),
                                fieldWithPath("gender").description("????????? ??????")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.LOCATION).description("API ??????"),
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("HAL JSON ??????")
                        ),
                        responseFields(
                                fieldWithPath("email").description("?????? ID"),
                                fieldWithPath("userName").description("????????? ??????"),
                                fieldWithPath("_links.self.href").description("?????? API ??????"),
                                fieldWithPath("_links.profile.href").description("API ?????? ??????"),
                                fieldWithPath("_links.signin.href").description("????????? ??????")
                        )
                ));
    }

    @Test
    @DisplayName("?????? ?????? ?????? ??? ?????? ?????? ??????")
    public void changeToInstructor() throws Exception {
        Account account = createAccount(Role.STUDENT);
        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(account.getId()), account.getRoles());

        AddInstructorRoleReq addInstructorRoleReq = AddInstructorRoleReq.builder()
                .phoneNumber("01011112222")
                .groupName("AIDA")
                .description("?????? ??????")
                .build();

        MockMultipartFile profile1 = new MockMultipartFile("profile", "profile1.png", "image/png", "test data".getBytes());
        MockMultipartFile profile2 = new MockMultipartFile("profile", "profile2.jpg", "image/png", "test data".getBytes());
        MockMultipartFile certificate1 = new MockMultipartFile("certificate", "certificate1.jpg", "image/png", "test data".getBytes());
        MockMultipartFile certificate2 = new MockMultipartFile("certificate", "certificate2.jpg", "image/png", "test data".getBytes());
        MockMultipartFile request =
                new MockMultipartFile("request",
                        "request",
                        MediaType.APPLICATION_JSON_VALUE,
                        objectMapper.writeValueAsString(addInstructorRoleReq).getBytes());
        account.setPhoneNumber(addInstructorRoleReq.getPhoneNumber());
        account.setGroupName(addInstructorRoleReq.getGroupName());
        account.setDescription(addInstructorRoleReq.getDescription());
        account.setRoles(Set.of(Role.STUDENT, Role.INSTRUCTOR));

        given(accountService.updateAccountToInstructor(eq(account.getEmail()), eq(addInstructorRoleReq), anyList(), anyList()))
                .willReturn(account);

        mockMvc.perform(multipart("/sign/addInstructorRole")
                .file(profile1)
                .file(profile2)
                .file(certificate1)
                .file(certificate2)
                .file(request)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .header("IsRefreshToken", "false"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("sign-addInstructorRole",
                        requestHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("multipart form data ??????"),
                                headerWithName(HttpHeaders.AUTHORIZATION).description("access token ???"),
                                headerWithName("IsRefreshToken").description("token??? refresh toekn?????? ??????")
                        ),
                        requestParts(
                                partWithName("request").description("?????? ?????? ??????"),
                                partWithName("profile").description("?????? ????????? ????????????"),
                                partWithName("certificate").description("?????? ????????? ????????????")
                        ),
                        requestPartBody("profile"),
                        requestPartBody("certificate"),
                        requestPartBody("request"),
                        requestPartFields("request",
                                    fieldWithPath("phoneNumber").description("?????? ????????????"),
                                    fieldWithPath("groupName").description("?????? ?????? ??????"),
                                    fieldWithPath("description").description("?????? ?????????")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("HAL JSON ??????")
                        ),
                        responseFields(
                                fieldWithPath("email").description("?????? ?????????"),
                                fieldWithPath("userName").description("?????? ?????? ??????"),
                                fieldWithPath("roles").description("?????? ???????????? ??????"),
                                fieldWithPath("_links.self.href").description("?????? API ??????")
                        )
                ));
    }

//    @Test
//    @DisplayName("?????? ?????? ?????? - ???????????? ?????????")
//    public void signupInputNull() throws Exception {
//        SignUpReq signUpReq = SignUpReq.builder().build();
//
//        mockMvc.perform(post("/sign/signup")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaTypes.HAL_JSON)
//                .content(objectMapper.writeValueAsString(signUpReq)))
//                .andDo(print())
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("code").value(-1004))
//                .andExpect(jsonPath("success").value(false));
//    }

    @Test
    @DisplayName("????????? ??????")
    public void signinSuccess() throws Exception {
        String email = "yechan@gmail.com";
        String password = "1234";

        SignInReq signInReq = new SignInReq(email, password);

        Account account = Account.builder()
                .id(1L)
                .email("yechan@gmail.com")
                .password(passwordEncoder.encode(password))
                .userName("yechan")
                .age(27)
                .gender(Gender.MALE)
                .roles(Set.of(Role.STUDENT))
                .build();

        AuthToken authToken = AuthToken.builder()
                .access_token("accessToken")
                .refresh_token("refreshToken")
                .token_type("tokenType")
                .scope("read")
                .expires_in(10000)
                .jti("jti")
                .build();

        given(authService.getAuthToken(String.valueOf(account.getId()), signInReq.getPassword())).willReturn(authToken);
        given(accountService.findAccountByEmail(signInReq.getEmail())).willReturn(account);

        mockMvc.perform(post("/sign/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signInReq)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("access_token").exists())
                .andExpect(jsonPath("_links.self").exists())
                .andDo(document("signIn",
                        requestHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("JSON ??????")
                        ),
                        requestFields(
                                fieldWithPath("email").description("?????? ID"),
                                fieldWithPath("password").description("?????? PASSWORD")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("HAL JSON ??????")
                        ),
                        responseFields(
                                fieldWithPath("access_token").description("JWT ?????? ?????? ???"),
                                fieldWithPath("refresh_token").description("JWT Refresh Token ???"),
                                fieldWithPath("token_type").description("?????? ??????"),
                                fieldWithPath("scope").description("?????? ?????? ??????"),
                                fieldWithPath("expires_in").description("?????? ??????"),
                                fieldWithPath("jti").description("JWT ?????? ?????????"),
                                fieldWithPath("_links.self.href").description("?????? API ??????"),
                                fieldWithPath("_links.profile.href").description("API ?????? ??????")
                        )
                ));
    }

//    @Test
//    @DisplayName("????????? ?????? - ?????????(ID)??? ?????? ??????")
//    public void signInNotFoundEmail() throws Exception {
//        String email = "yechan@gmail.com";
//        String password = "1234";
//
//        doThrow(new CEmailSigninFailedException()).when(accountService).findAccountByEmail(email);
//
//        mockMvc.perform(post("/sign/signin")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(new SignInReq(email, password))))
//                .andDo(print())
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("success").value(false))
//                .andExpect(jsonPath("code").value(-1001));
//    }

//    @Test
//    @DisplayName("????????? ?????? - PASSWORD??? ?????? ??????")
//    public void signInNotMatchPassword() throws Exception {
//        String email = "yechan@gmail.com";
//        String password = "1234";
//        String encodedPassword = passwordEncoder.encode(password);
//
//        Account account = Account.builder()
//                .email(email)
//                .password(encodedPassword)
//                .build();
//
//        given(accountService.findAccountByEmail(email)).willReturn(account);
//        doThrow(new CEmailSigninFailedException()).when(accountService).checkCorrectPassword(any(), any());
//
//        mockMvc.perform(post("/sign/signin")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(new SignInReq(email, "wrongPassword"))))
//                .andDo(print())
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("success").value(false))
//                .andExpect(jsonPath("code").value(-1001));
//    }

    @Test
    @DisplayName("RefreshToken?????? ?????????")
    public void refresh() throws Exception {
        Long id = 1L;
        Account account = Account.builder()
                .id(id)
                .email("yechan@gmail.com")
                .roles(Set.of(Role.INSTRUCTOR))
                .build();

        given(accountService.findAccountById(id)).willReturn(account);
        String refreshToken = jwtTokenProvider.createRefreshToken(String.valueOf(id));

        mockMvc.perform(get("/sign/refresh")
                .header("Authorization", refreshToken)
                .header("IsRefreshToken", "true"))
                .andDo(print())
                .andExpect(jsonPath("accessToken").exists())
                .andExpect(jsonPath("refreshToken").exists())
                .andDo(document("refresh",
                        requestHeaders(
                                headerWithName("Authorization").description("refresh token ???"),
                                headerWithName("IsRefreshToken").description("token??? refresh token?????? ??????")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("HAL JSON ??????")
                        ),
                        responseFields(
                                fieldWithPath("accessToken").description("???????????? access token"),
                                fieldWithPath("refreshToken").description("???????????? refresh token"),
                                fieldWithPath("_links.self.href").description("?????? API ??????"),
                                fieldWithPath("_links.profile.href").description("?????? API ?????? ??????")
                        )
                ));
    }

    @Test
    @DisplayName("???????????? ??????")
    public void logout() throws Exception {
        Account account = Account.builder()
                .id(1L)
                .email("yechan@gmail.com")
                .password("1234")
                .roles(Set.of(Role.INSTRUCTOR))
                .build();

        given(accountService.loadUserByUsername(String.valueOf(account.getId())))
                .willReturn(new User(account.getEmail(), account.getPassword(), authorities(account.getRoles())));

        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(account.getId()), account.getRoles());
        String refreshToken = jwtTokenProvider.createRefreshToken(String.valueOf(account.getId()));

        LogoutReq logoutReq = LogoutReq.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        mockMvc.perform(post("/sign/logout")
                .header("Authorization", accessToken)
                .header("IsRefreshToken", "false")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoutReq)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("message").exists())
                .andDo(document("logout",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("access token ???"),
                                headerWithName("IsRefreshToken").description("refresh token ?????? ???????????? ?????? ???")),
                        requestFields(
                                fieldWithPath("accessToken").description("access token ???"),
                                fieldWithPath("refreshToken").description("refresh token ???")),
                        responseFields(
                                fieldWithPath("message").description("?????? ?????????"),
                                fieldWithPath("_links.self.href").description("?????? API ??????"),
                                fieldWithPath("_links.profile.href").description("?????? API ?????? ??????")
                        )
                ));
    }

//    @Test
//    @DisplayName("????????? ???????????? ????????? ?????? ?????????")
//    public void forbidden() throws Exception {
//        Account account = Account.builder()
//                .id(1L)
//                .email("yechan@gmail.com")
//                .password("1234")
//                .roles(Set.of(Role.INSTRUCTOR))
//                .build();
//
//        given(accountService.loadUserByUsername(String.valueOf(account.getId())))
//                .willReturn(new User(account.getEmail(), account.getPassword(), authorities(account.getRoles())));
//
//        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(account.getId()), account.getRoles());
//        String refreshToken = jwtTokenProvider.createRefreshToken(String.valueOf(account.getId()));
//
//        given(accountService.checkValidToken(accessToken)).willReturn("false");
//
//        LogoutReq logoutReq = LogoutReq.builder()
//                .accessToken(accessToken)
//                .refreshToken(refreshToken)
//                .build();
//
//        mockMvc.perform(get("/sign/logout")
//                .header("Authorization", accessToken)
//                .header("IsRefreshToken", "false")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(logoutReq)))
//                .andDo(print())
//                .andExpect(status().isForbidden())
//                .andExpect(jsonPath("code").value(-1007));
//    }

    private Collection<? extends GrantedAuthority> authorities(Set<Role> roles) {
        return roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                .collect(Collectors.toList());
    }

}