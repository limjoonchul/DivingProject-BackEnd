package com.diving.pungdong.controller.reservation;

import com.diving.pungdong.config.RestDocsConfiguration;
import com.diving.pungdong.config.security.JwtTokenProvider;
import com.diving.pungdong.config.security.UserAccount;
import com.diving.pungdong.domain.Location;
import com.diving.pungdong.domain.account.Account;
import com.diving.pungdong.domain.account.Gender;
import com.diving.pungdong.domain.account.Role;
import com.diving.pungdong.domain.reservation.Reservation;
import com.diving.pungdong.domain.reservation.ReservationDate;
import com.diving.pungdong.domain.schedule.Schedule;
import com.diving.pungdong.domain.schedule.ScheduleDetail;
import com.diving.pungdong.dto.reservation.ReservationCreateReq;
import com.diving.pungdong.dto.reservation.ReservationDateDto;
import com.diving.pungdong.dto.reservation.ReservationInfo;
import com.diving.pungdong.dto.reservation.ReservationSubInfo;
import com.diving.pungdong.dto.schedule.read.ScheduleTimeInfo;
import com.diving.pungdong.service.AccountService;
import com.diving.pungdong.service.LectureService;
import com.diving.pungdong.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureRestDocs
@Import(RestDocsConfiguration.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private LectureService lectureService;

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
                .willReturn(new UserAccount(account));

        return account;
    }

    private Collection<? extends GrantedAuthority> authorities(Set<Role> roles) {
        return roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                .collect(Collectors.toList());
    }

    @Test
    @DisplayName("?????? ??????")
    public void createReservation() throws Exception {
        Account account = createAccount(Role.STUDENT);
        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(account.getId()), Set.of(Role.STUDENT));

        List<String> equipmentList = createEquipmentNameList();

        List<ReservationDateDto> reservationDateDtoList = createReservationDateDtoList();

        ReservationCreateReq req = ReservationCreateReq.builder()
                .scheduleId(1L)
                .description("??? ????????? 260, ??? ????????? L")
                .equipmentList(equipmentList)
                .reservationDateList(reservationDateDtoList)
                .build();

        Reservation reservation = Reservation.builder()
                .id(1L)
                .schedule(Schedule.builder().id(1L).build())
                .account(account)
                .build();

        given(reservationService.makeReservation(any(), any())).willReturn(reservation);

        mockMvc.perform(post("/reservation")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .header("IsRefreshToken", "false")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andDo(document("reservation-create",
                        requestHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("application json ??????"),
                                headerWithName("Authorization").description("access token ???"),
                                headerWithName("IsRefreshToken").description("token??? refresh token?????? ??????")
                        ),
                        requestFields(
                                fieldWithPath("scheduleId").description("?????? ?????? ????????? ???"),
                                fieldWithPath("reservationDateList[].scheduleDetailId").description("?????? ?????? ?????? ????????? ???"),
                                fieldWithPath("reservationDateList[].scheduleTimeId").description("?????? ?????? ?????? ????????? ???"),
                                fieldWithPath("reservationDateList[].date").description("?????? ??????"),
                                fieldWithPath("reservationDateList[].time").description("?????? ??????"),
                                fieldWithPath("equipmentList[]").description("?????? ?????? ?????? ?????????"),
                                fieldWithPath("description").description("?????? ?????? ?????? ????????? ?????? ??? ?????? ??????")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("HAL JSON ??????")
                        ),
                        responseFields(
                                fieldWithPath("reservationId").description("?????? ?????? ?????????"),
                                fieldWithPath("scheduleId").description("?????? ?????? ?????? ?????????"),
                                fieldWithPath("accountId").description("?????? ????????? ?????????"),
                                fieldWithPath("_links.self.href").description("?????? API URL")
                        )
                ));
    }

    public List<ReservationDateDto> createReservationDateDtoList() {
        List<ReservationDateDto> reservationDateDtoList = new ArrayList<>();

        ReservationDateDto reservationDateDto = ReservationDateDto.builder()
                .scheduleDetailId(1L)
                .scheduleTimeId(1L)
                .date(LocalDate.of(2021, 4, 20))
                .time(LocalTime.of(14, 0))
                .build();
        reservationDateDtoList.add(reservationDateDto);
        return reservationDateDtoList;
    }

    public List<String> createEquipmentNameList() {
        List<String> equipmentList = new ArrayList<>();
        equipmentList.add("?????????");
        equipmentList.add("??????");
        return equipmentList;
    }

    @Test
    @DisplayName("???????????? ?????? ????????? ??????")
    public void searchReservationList() throws Exception {
        Account account = createAccount(Role.STUDENT);
        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(account.getId()), Set.of(Role.STUDENT));

        List<ReservationSubInfo> reservationSubInfoList = new ArrayList<>();
        ReservationSubInfo reservationSubInfo = ReservationSubInfo.builder()
                .reservationId(1L)
                .lectureTitle("?????? ????????? ?????? 1")
                .isMultipleCourse(false)
                .totalCost(100000)
                .dateOfReservation(LocalDate.of(2021, 3, 4))
                .build();
        reservationSubInfoList.add(reservationSubInfo);
        Pageable pageable = PageRequest.of(0, 5);
        Page<ReservationSubInfo> reservationSubInfoPage = new PageImpl<>(reservationSubInfoList, pageable, reservationSubInfoList.size());

        given(reservationService.findMyReservationList(account.getId(), pageable)).willReturn(reservationSubInfoPage);

        mockMvc.perform(get("/reservation/list")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .header("IsRefreshToken", "false")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .param("page", String.valueOf(pageable.getPageNumber()))
                .param("size", String.valueOf(pageable.getPageSize())))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("reservation-get-list",
                        requestHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("application json ??????"),
                                headerWithName("Authorization").description("access token ???"),
                                headerWithName("IsRefreshToken").description("token??? refresh token?????? ??????")
                        ),
                        requestParameters(
                                parameterWithName("page").description("????????? ??????"),
                                parameterWithName("size").description("??? ???????????? ?????????")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.reservationSubInfoList[].reservationId").description("?????? ????????? ???"),
                                fieldWithPath("_embedded.reservationSubInfoList[].lectureTitle").description("?????? ?????? ??????"),
                                fieldWithPath("_embedded.reservationSubInfoList[].totalCost").description("?????? ??????"),
                                fieldWithPath("_embedded.reservationSubInfoList[].isMultipleCourse").description("????????? ?????? ???????????? ??????"),
                                fieldWithPath("_embedded.reservationSubInfoList[].dateOfReservation").description("?????? ??????"),
                                fieldWithPath("_links.self.href").description("?????? API URL"),
                                fieldWithPath("page.size").description("??? ???????????? ??????"),
                                fieldWithPath("page.totalElements").description("?????? ?????? ??????"),
                                fieldWithPath("page.totalPages").description("?????? ????????? ??????"),
                                fieldWithPath("page.number").description("?????? ????????? ??????")
                        )
                ));
    }

    @Test
    @DisplayName("?????? ?????? ??????")
    public void getReservationDetail() throws Exception {
        Account account = createAccount(Role.STUDENT);
        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(account.getId()), Set.of(Role.STUDENT));

        Long reservationId = 1L;

        Location location = Location.builder()
                .latitude(36.568)
                .longitude(137.546)
                .address("????????? ?????? ?????????")
                .build();

        ReservationDate reservationDate = ReservationDate.builder()
                .date(LocalDate.of(2021, 3, 4))
                .time(LocalTime.of(18, 0))
                .scheduleDetail(ScheduleDetail.builder().location(location).build())
                .build();

        Reservation reservation = Reservation.builder()
                .account(account)
                .reservationDateList(List.of(reservationDate))
                .equipmentList(List.of("?????????", "??????"))
                .description("????????? 270, ?????? L")
                .build();

        given(reservationService.getDetailById(reservationId)).willReturn(reservation);

        mockMvc.perform(RestDocumentationRequestBuilders.get("/reservation/{id}", reservationId)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .header("IsRefreshToken", "false")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("reservation-get-detail",
                        requestHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("application json ??????"),
                                headerWithName("Authorization").description("access token ???"),
                                headerWithName("IsRefreshToken").description("token??? refresh token?????? ??????")
                        ),
                        pathParameters(
                                parameterWithName("id").description("?????? ????????? ???")
                        ),
                        responseFields(
                                fieldWithPath("reservationScheduleList[].date").description("?????? ??????"),
                                fieldWithPath("reservationScheduleList[].time").description("?????? ??????"),
                                fieldWithPath("reservationScheduleList[].location.latitude").description("?????? ?????? ??????"),
                                fieldWithPath("reservationScheduleList[].location.longitude").description("?????? ?????? ??????"),
                                fieldWithPath("reservationScheduleList[].location.address").description("?????? ?????? ??????"),
                                fieldWithPath("equipmentNameList[]").description("?????? ?????? ??????"),
                                fieldWithPath("description").description("?????? ?????? ????????? ??? ????????????"),
                                fieldWithPath("_links.self.href").description("?????? API URL")
                        )
                ));
    }

    @Test
    @DisplayName("?????? ?????? ??????")
    public void cancelReservation() throws Exception {
        Account account = createAccount(Role.STUDENT);
        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(account.getId()), Set.of(Role.STUDENT));
        Long reservationId = 1L;

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .build();

        given(reservationService.getDetailById(reservationId)).willReturn(reservation);

        mockMvc.perform(delete("/reservation/{id}", reservationId)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .header("IsRefreshToken", "false")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("reservation-delete",
                        requestHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("application json ??????"),
                                headerWithName("Authorization").description("access token ???"),
                                headerWithName("IsRefreshToken").description("token??? refresh token?????? ??????")
                        ),
                        pathParameters(
                                parameterWithName("id").description("?????? ????????? ???")
                        ),
                        responseFields(
                                fieldWithPath("reservationCancelId").description("????????? ?????? ????????? ???"),
                                fieldWithPath("success").description("???????????? ?????? ??????")
                        )
                ));
    }

    @Test
    @DisplayName("????????? ??? ????????? ?????? ?????? ??????")
    public void getReservationInfoForSchedule() throws Exception {
        Account account = createAccount(Role.INSTRUCTOR);
        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(account.getId()), Set.of(Role.INSTRUCTOR));
        ScheduleTimeInfo scheduleTimeInfo = ScheduleTimeInfo.builder()
                .lectureId(1L)
                .scheduleTimeId(1L)
                .build();

        ReservationInfo reservationInfo = ReservationInfo.builder()
                .userName("?????????")
                .equipmentList(List.of("?????????", "??????"))
                .description("????????? ????????? 260, ?????? ????????? L")
                .build();
        List<ReservationInfo> reservationInfos = new ArrayList<>();
        reservationInfos.add(reservationInfo);

        doNothing().when(lectureService).checkRightInstructor(account, scheduleTimeInfo.getLectureId());
        given(reservationService.getReservationForSchedule(scheduleTimeInfo.getScheduleTimeId())).willReturn(reservationInfos);

        mockMvc.perform(get("/reservation/students")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .header("IsRefreshToken", "false")
                .content(objectMapper.writeValueAsString(scheduleTimeInfo)))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("reservation-get-list-for-schedule",
                        requestHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("application json ??????"),
                                headerWithName("Authorization").description("access token ???"),
                                headerWithName("IsRefreshToken").description("token??? refresh token?????? ??????")
                        ),
                        requestFields(
                                fieldWithPath("lectureId").description("?????? ?????? ????????? ???"),
                                fieldWithPath("scheduleTimeId").description("?????? ?????? ?????? ????????? ???")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.reservationInfoList[].userName").description("????????? ????????? ??????"),
                                fieldWithPath("_embedded.reservationInfoList[].equipmentList[]").description("????????? ????????? ?????? ?????? ??????"),
                                fieldWithPath("_embedded.reservationInfoList[].description").description("????????? ????????? ?????? ?????? ????????? ?????? ??? ????????????")
                        )
                ));
    }
}