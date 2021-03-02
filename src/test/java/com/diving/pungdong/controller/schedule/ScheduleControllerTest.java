package com.diving.pungdong.controller.schedule;

import com.diving.pungdong.config.RestDocsConfiguration;
import com.diving.pungdong.config.security.JwtTokenProvider;
import com.diving.pungdong.domain.Location;
import com.diving.pungdong.domain.account.Account;
import com.diving.pungdong.domain.account.Gender;
import com.diving.pungdong.domain.account.Role;
import com.diving.pungdong.domain.lecture.Lecture;
import com.diving.pungdong.domain.schedule.Schedule;
import com.diving.pungdong.domain.schedule.ScheduleDetail;
import com.diving.pungdong.dto.schedule.create.ScheduleCreateReq;
import com.diving.pungdong.dto.schedule.create.ScheduleDetailReq;
import com.diving.pungdong.service.AccountService;
import com.diving.pungdong.service.LectureService;
import com.diving.pungdong.service.ScheduleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureRestDocs
@Import(RestDocsConfiguration.class)
@Transactional
class ScheduleControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @MockBean
    private ScheduleService scheduleService;

    @MockBean
    private LectureService lectureService;

    @Test
    @DisplayName("일정 등록")
    public void create() throws Exception {
        Account account = createAccount();
        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(account.getId()), Set.of(Role.INSTRUCTOR));

        ScheduleCreateReq req = ScheduleCreateReq.builder()
                .lectureId(1L)
                .period(3)
                .build();

        List<ScheduleDetailReq> detailReqs = new ArrayList<>();
        for (int i = 1; i <= req.getPeriod(); i++) {
            Location location = Location.builder()
                    .address("상세 주소")
                    .latitude(37.0)
                    .longitude(127.0)
                    .build();
            List<LocalTime> startTimes = new ArrayList<>();
            startTimes.add(LocalTime.of(13, 0));
            startTimes.add(LocalTime.of(15, 0));

            ScheduleDetailReq scheduleDetailReq = ScheduleDetailReq.builder()
                    .date(LocalDate.of(2021, 2, i))
                    .startTimes(startTimes)
                    .lectureTime(LocalTime.of(1, 30))
                    .location(location)
                    .build();

            detailReqs.add(scheduleDetailReq);
        }
        req.setDetailReqList(detailReqs);

        given(lectureService.getLectureById(req.getLectureId())).willReturn(Lecture.builder().id(1L).build());
        given(scheduleService.saveScheduleTx(any(), eq(req))).willReturn(Schedule.builder().id(1L).build());
        mockMvc.perform(post("/schedule")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .header("IsRefreshToken", "false")
                .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andDo(document("schedule-create",
                        requestHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("multipart form data 타입"),
                                headerWithName("Authorization").description("access token 값"),
                                headerWithName("IsRefreshToken").description("token이 refresh token인지 확인")
                        ),
                        requestFields(
                                fieldWithPath("lectureId").description("강의 식별자 값"),
                                fieldWithPath("period").description("강의 기간"),
                                fieldWithPath("detailReqList").description("강의 한 날에 대한 세부사항 리스트"),
                                fieldWithPath("detailReqList[].date").description("강의 날짜"),
                                fieldWithPath("detailReqList[].startTimes[]").description("강의 시작 시간 리스트"),
                                fieldWithPath("detailReqList[].lectureTime").description("강의 시간"),
                                fieldWithPath("detailReqList[].location.latitude").description("위치 위도"),
                                fieldWithPath("detailReqList[].location.longitude").description("위치 경도"),
                                fieldWithPath("detailReqList[].location.address").description("위치 상세 주소")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("HAL JSON 타입")
                        ),
                        responseFields(
                                fieldWithPath("lectureId").description("강의 식별자 값"),
                                fieldWithPath("scheduleId").description("일정 식별자 값"),
                                fieldWithPath("_links.self.href").description("해당 API 주소")
                        )
                ));
    }

    public Account createAccount() {
        Account account = Account.builder()
                .id(1L)
                .email("yechan@gmail.com")
                .password("1234")
                .userName("yechan")
                .age(27)
                .gender(Gender.MALE)
                .roles(Set.of(Role.INSTRUCTOR))
                .build();

        given(accountService.loadUserByUsername(String.valueOf(account.getId())))
                .willReturn(new User(account.getEmail(), account.getPassword(), authorities(account.getRoles())));

        return account;
    }

    private Collection<? extends GrantedAuthority> authorities(Set<Role> roles) {
        return roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                .collect(Collectors.toList());
    }

    @Test
    @DisplayName("해당 강의 일정 조회")
    public void getScheduleByLectureId() throws Exception {
        Long lectureId = 1L;

        List<Schedule> schedules = createSchedules();

        given(scheduleService.getByLectureId(lectureId)).willReturn(schedules);

        mockMvc.perform(get("/schedule")
                .param("lectureId", String.valueOf(lectureId)))
                .andDo(print())
                .andExpect(status().isOk());

        //                                fieldWithPath("schedules").description("강의 일정 리스트"),
//                                fieldWithPath("schedules[].period").description("강의 총 진행 날짜 수"),
//                                fieldWithPath("schedules[].scheduleDetails[]").description("각 강의 날짜 상세 내용"),
//                                fieldWithPath("schedules[].scheduleDetails[].date").description("강의 진행 날짜"),
//                                fieldWithPath("schedules[].scheduleDetails[].startTimes[]").description("해당 날짜 강의 가능 시간 리스트"),
//                                fieldWithPath("schedules[].scheduleDetails[].lectureTime").description("한 강의 당 진행시간"),
//                                fieldWithPath("schedules[].scheduleDetails[].location.latitude").description("강의 진행 장소 위도"),
//                                fieldWithPath("schedules[].scheduleDetails[].location.longitude").description("강의 진행 장소 경도"),
//                                fieldWithPath("schedules[].scheduleDetails[].location.address").description("강의 진행 장소 상세주소"),
    }

    public List<Schedule> createSchedules() {
        List<Schedule> schedules = new ArrayList<>();
        Schedule schedule = Schedule.builder()
                .period(3)
                .build();

        Location location = Location.builder()
                .latitude(37.0)
                .longitude(127.0)
                .address("상세 주소")
                .build();

        List<ScheduleDetail> scheduleDetails = createScheduleDetails(location);

        schedule.setScheduleDetails(scheduleDetails);
        schedules.add(schedule);
        return schedules;
    }

    public List<ScheduleDetail> createScheduleDetails(Location location) {
        List<ScheduleDetail> scheduleDetails = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            ScheduleDetail scheduleDetail = ScheduleDetail.builder()
                    .date(LocalDate.of(2021, 3, 1).plusDays(i))
                    .startTimes(List.of(LocalTime.of(13, 0), LocalTime.of(15, 0)))
                    .lectureTime(LocalTime.of(1, 30))
                    .location(location)
                    .build();
            scheduleDetails.add(scheduleDetail);
        }
        return scheduleDetails;
    }
}