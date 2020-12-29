package com.diving.pungdong.controller.sign;

import com.diving.pungdong.advice.exception.CEmailSigninFailedException;
import com.diving.pungdong.config.RestDocsConfiguration;
import com.diving.pungdong.config.security.JwtTokenProvider;
import com.diving.pungdong.domain.account.Account;
import com.diving.pungdong.domain.account.Gender;
import com.diving.pungdong.domain.account.Role;
import com.diving.pungdong.service.AccountService;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.servlet.http.Cookie;
import java.util.Collections;
import java.util.Set;

import static com.diving.pungdong.controller.sign.SignController.SignUpReq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureRestDocs
@Import(RestDocsConfiguration.class)
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

    @Test
    @DisplayName("회원가입 성공")
    public void signupSuccess() throws Exception {
        SignUpReq signUpReq = SignUpReq.builder()
                .email("yechan@gmail.com")
                .password("1234")
                .userName("yechan")
                .age(24)
                .gender(Gender.MALE)
                .roles(Set.of(Role.INSTRUCTOR))
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
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("JSON 타입")
                        ),
                        requestFields(
                                fieldWithPath("email").description("유저 ID"),
                                fieldWithPath("password").description("유저 PASSWORD"),
                                fieldWithPath("userName").description("유저의 이름"),
                                fieldWithPath("age").description("유저의 나이"),
                                fieldWithPath("gender").description("유저의 성별"),
                                fieldWithPath("roles").description("유저의 권한")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.LOCATION).description("API 주소"),
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("HAL JSON 타입")
                        ),
                        responseFields(
                                fieldWithPath("email").description("유저 ID"),
                                fieldWithPath("userName").description("유저의 이름"),
                                fieldWithPath("_links.self.href").description("해당 API 링크"),
                                fieldWithPath("_links.profile.href").description("API 문서 링크"),
                                fieldWithPath("_links.signin.href").description("로그인 링크")
                        )
                ));
    }

    @Test
    @DisplayName("회원 가입 실패 - 입력값이 잘못됨")
    public void signupInputNull() throws Exception {
        SignUpReq signUpReq = SignUpReq.builder().build();

        mockMvc.perform(post("/sign/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaTypes.HAL_JSON)
                .content(objectMapper.writeValueAsString(signUpReq)))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("code").value(-1004))
                .andExpect(jsonPath("success").value(false));
    }

    @Test
    @DisplayName("로그인 성공")
    public void signinSuccess() throws Exception {
        String email = "yechan@gmail.com";
        String password = "1234";
        String encodedPassword = passwordEncoder.encode(password);

        Account account = Account.builder()
                .email(email)
                .password(encodedPassword)
                .build();

        given(accountService.findAccountByEmail(email)).willReturn(account);

        mockMvc.perform(post("/sign/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .param("email", email)
                .param("password", password))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("accessToken").exists())
                .andExpect(jsonPath("_links.self").exists())
                .andDo(document("signIn",
                        requestHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("JSON 타입")
                        ),
                        requestParameters(
                                parameterWithName("email").description("유저 ID"),
                                parameterWithName("password").description("유저 PASSWORD")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("HAL JSON 타입")
                        ),
                        responseFields(
                                fieldWithPath("accessToken").description("JWT 인증 토큰 값"),
                                fieldWithPath("refreshToken").description("JWT Refresh Token 값"),
                                fieldWithPath("_links.self.href").description("해당 API 링크"),
                                fieldWithPath("_links.profile.href").description("API 문서 링크")
                        )
                ));
    }

    @Test
    @DisplayName("로그인 실패 - 이메일(ID)이 없는 경우")
    public void signInNotFoundEmail() throws Exception {
        String email = "yechan@gmail.com";
        String password = "1234";

        given(accountService.findAccountByEmail(email)).willThrow(CEmailSigninFailedException.class);

        mockMvc.perform(post("/sign/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .param("email", email)
                .param("password", password))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("success").value(false))
                .andExpect(jsonPath("code").value(-1001));
    }

    @Test
    @DisplayName("로그인 실패 - PASSWORD가 틀린 경우")
    public void signInNotMatchPassword() throws Exception {
        String email = "yechan@gmail.com";
        String password = "1234";
        String encodedPassword = passwordEncoder.encode(password);

        Account account = Account.builder()
                .email(email)
                .password(encodedPassword)
                .build();

        given(accountService.findAccountByEmail(email)).willReturn(account);

        mockMvc.perform(post("/sign/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .param("email", email)
                .param("password", "wrong"))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("success").value(false))
                .andExpect(jsonPath("code").value(-1001));
    }

    @Test
    @DisplayName("RefreshToken으로 재발급")
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
                    .andDo(document("getRefreshToken",
                            requestHeaders(
                                    headerWithName("Authorization").description("refresh token 값"),
                                    headerWithName("IsRefreshToken").description("token이 refresh token인지 확인")
                            ),
                            responseFields(
                                    fieldWithPath("accessToken").description("재발급된 access token"),
                                    fieldWithPath("refreshToken").description("재발급된 refresh token")
                            ),
                            responseHeaders(
                                    headerWithName(HttpHeaders.CONTENT_TYPE).description("HAL JSON 타입")
                            )
                    ));
    }
    
}