package com.diving.pungdong.controller.lecture;

import com.diving.pungdong.config.RestDocsConfiguration;
import com.diving.pungdong.config.S3Uploader;
import com.diving.pungdong.config.security.JwtTokenProvider;
import com.diving.pungdong.config.security.UserAccount;
import com.diving.pungdong.domain.account.Account;
import com.diving.pungdong.domain.account.Gender;
import com.diving.pungdong.domain.account.Role;
import com.diving.pungdong.domain.equipment.Equipment;
import com.diving.pungdong.domain.lecture.Lecture;
import com.diving.pungdong.domain.lecture.LectureImage;
import com.diving.pungdong.dto.lecture.create.CreateLectureReq;
import com.diving.pungdong.dto.lecture.create.EquipmentDto;
import com.diving.pungdong.dto.lecture.mylist.LectureInfo;
import com.diving.pungdong.dto.lecture.search.CostCondition;
import com.diving.pungdong.dto.lecture.search.SearchCondition;
import com.diving.pungdong.dto.lecture.update.EquipmentUpdate;
import com.diving.pungdong.dto.lecture.update.LectureImageUpdate;
import com.diving.pungdong.dto.lecture.update.LectureUpdateInfo;
import com.diving.pungdong.service.AccountService;
import com.diving.pungdong.service.LectureImageService;
import com.diving.pungdong.service.LectureService;
import com.diving.pungdong.service.SwimmingPoolService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureRestDocs
@Import(RestDocsConfiguration.class)
class LectureControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    JwtTokenProvider jwtTokenProvider;
    @Autowired
    ModelMapper modelMapper;
    @MockBean
    SwimmingPoolService swimmingPoolService;
    @MockBean
    AccountService accountService;
    @MockBean
    LectureService lectureService;
    @MockBean
    LectureImageService lectureImageService;
    @MockBean
    S3Uploader s3Uploader;

    @Test
    @DisplayName("?????? ??????")
    public void createLecture() throws Exception {
        Account account = createAccount();

        List<EquipmentDto> equipmentList = new ArrayList<>();
        EquipmentDto equipment1 = EquipmentDto.builder()
                .name("?????????")
                .price(3000)
                .build();

        EquipmentDto equipment2 = EquipmentDto.builder()
                .name("?????????")
                .price(3000)
                .build();

        equipmentList.add(equipment1);
        equipmentList.add(equipment2);

        CreateLectureReq createLectureReq = CreateLectureReq.builder()
                .title("??????1")
                .description("??????1")
                .classKind("????????? ?????????")
                .groupName("AIDA")
                .certificateKind("Level1")
                .price(100000)
                .region("??????")
                .equipmentList(equipmentList)
                .build();

        MockMultipartFile file1 = new MockMultipartFile("fileList", "test1.txt", "image/png", "test data".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("fileList", "test2.txt", "image/png", "test data".getBytes());

        MockMultipartFile request =
                new MockMultipartFile("request",
                        "request.json",
                        MediaType.APPLICATION_JSON_VALUE,
                        objectMapper.writeValueAsString(createLectureReq).getBytes());
        String accessToken = jwtTokenProvider.createAccessToken("1", Set.of(Role.INSTRUCTOR));

        Lecture lecture = Lecture.builder()
                .id(1L)
                .title(createLectureReq.getTitle())
                .instructor(account)
                .build();

        given(accountService.findAccountByEmail(account.getEmail())).willReturn(account);
        given(lectureService.createLecture(eq(account.getEmail()), anyList(), any(Lecture.class), anyList())).willReturn(lecture);

        mockMvc.perform(multipart("/lecture/create")
                .file(file1)
                .file(file2)
                .file(request)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .header("IsRefreshToken", "false")
        )
                .andDo(print())
                .andExpect(status().isCreated())
                .andDo(document("create-lecture",
                        requestHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("multipart form data ??????"),
                                headerWithName("Authorization").description("access token ???"),
                                headerWithName("IsRefreshToken").description("token??? refresh token?????? ??????")
                        ),
                        requestParts(
                                partWithName("fileList").description("????????? ?????? ?????????"),
                                partWithName("request").description("?????? ?????? ?????? JSON ?????????")
                        ),
                        requestPartBody("fileList"),
                        requestPartBody("request"),
                        requestPartFields("request",
                                fieldWithPath("title").description("?????? ??????"),
                                fieldWithPath("description").description("?????? ??????"),
                                fieldWithPath("classKind").description("?????? ??????"),
                                fieldWithPath("groupName").description("?????????"),
                                fieldWithPath("certificateKind").description("????????? ??????"),
                                fieldWithPath("price").description("?????? ??????"),
                                fieldWithPath("region").description("?????? ??????"),
                                fieldWithPath("equipmentList[0].name").description("?????? ??????1 ??????"),
                                fieldWithPath("equipmentList[0].price").description("?????? ??????1 ??????")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.LOCATION).description("?????? API URI"),
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("HAL JSON ??????")
                        ),
                        responseFields(
                                fieldWithPath("lectureId").description("?????? ????????? ???"),
                                fieldWithPath("title").description("?????? ??????"),
                                fieldWithPath("instructorName").description("?????? ??????"),
                                fieldWithPath("_links.self.href").description("?????? API URI"),
                                fieldWithPath("_links.profile.href").description("?????? API ?????? ??????")
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
                .willReturn(new UserAccount(account));

        return account;
    }

    @Test
    @DisplayName("?????? ?????? ??????")
    public void update() throws Exception {
        Account account = createAccount();
        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(account.getId()), account.getRoles());

        Lecture lecture = Lecture.builder()
                .id(1L)
                .title("??????1")
                .classKind("??????????????????")
                .groupName("AIDA")
                .certificateKind("LEVEL1")
                .description("?????? ??????")
                .price(300000)
                .region("??????")
                .instructor(account)
                .lectureImages(List.of(LectureImage.builder().fileURI("File URL1").build()))
                .equipmentList(List.of(Equipment.builder().name("??????1").price(3000).build()))
                .build();

        LectureUpdateInfo lectureUpdateInfo = LectureUpdateInfo.builder()
                .id(1L)
                .title("?????? ?????? Update")
                .classKind("?????? ?????????")
                .groupName("AIDA")
                .certificateKind("LEVEL2")
                .description("?????? ??????  Update")
                .price(400000)
                .period(5)
                .studentCount(6)
                .region("??????")
                .lectureImageUpdateList(List.of(LectureImageUpdate.builder().lectureImageURL("File URL1").isDeleted(true).build()))
                .equipmentUpdateList(List.of(EquipmentUpdate.builder().name("??????1").price(5000).isDeleted(false).build()))
                .build();

        Lecture updatedLecture = Lecture.builder()
                .id(1L)
                .title("?????? ?????? Update")
                .classKind("?????? ?????????")
                .groupName("AIDA")
                .certificateKind("LEVEL2")
                .description("?????? ??????  Update")
                .price(400000)
                .region("??????")
                .instructor(Account.builder().id(10L).build())
                .equipmentList(List.of(Equipment.builder().name("??????1").price(5000).build()))
                .build();

        MockMultipartFile file1 = new MockMultipartFile("fileList", "test1.txt", "image/png", "test data".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("fileList", "test2.txt", "image/png", "test data".getBytes());

        MockMultipartFile request =
                new MockMultipartFile("request",
                        "request",
                        MediaType.APPLICATION_JSON_VALUE,
                        objectMapper.writeValueAsString(lectureUpdateInfo).getBytes());

        given(lectureService.getLectureById(1L)).willReturn(lecture);
        given(lectureService.updateLectureTx(eq(account.getEmail()), eq(lectureUpdateInfo), anyList(), eq(lecture))).willReturn(updatedLecture);

        mockMvc.perform(multipart("/lecture/update")
                .file(file1)
                .file(file2)
                .file(request)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .header("IsRefreshToken", "false"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("update-lecture",
                        requestHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("multipart form data ??????"),
                                headerWithName("Authorization").description("access token ???"),
                                headerWithName("IsRefreshToken").description("token??? refresh token?????? ??????")
                        ),
                        requestParts(
                                partWithName("request").description("?????? ?????? ?????? JSON ?????????"),
                                partWithName("fileList").description("????????? ????????? ?????? ?????????")
                        ),
                        requestPartBody("fileList"),
                        requestPartBody("request"),
                        requestPartFields(
                                "request",
                                fieldWithPath("id").description("?????? ?????????"),
                                fieldWithPath("title").description("?????? ??????"),
                                fieldWithPath("classKind").description("?????? ??????"),
                                fieldWithPath("groupName").description("?????????"),
                                fieldWithPath("certificateKind").description("????????? ??????"),
                                fieldWithPath("description").description("?????? ??????"),
                                fieldWithPath("price").description("?????? ??????"),
                                fieldWithPath("period").description("?????? ??????"),
                                fieldWithPath("studentCount").description("?????? ?????? ??????"),
                                fieldWithPath("region").description("?????? ??????"),
                                fieldWithPath("lectureImageUpdateList[0].lectureImageURL").description("????????? ?????? ????????? ??????"),
                                fieldWithPath("lectureImageUpdateList[0].isDeleted").description("?????? ????????? ?????? ?????? ??????"),
                                fieldWithPath("equipmentUpdateList[0].name").description("????????? ?????? ?????? ??????"),
                                fieldWithPath("equipmentUpdateList[0].price").description("????????? ?????? ?????? ??????"),
                                fieldWithPath("equipmentUpdateList[0].isDeleted").description("?????? ?????? ?????? ?????? ?????? ??????")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("HAL JSON ??????")
                        ),
                        responseFields(
                                fieldWithPath("id").description("?????? ?????????"),
                                fieldWithPath("title").description("?????? ??????"),
                                fieldWithPath("_links.self.href").description("?????? API ??????")
                        )
                ));
    }

    @Test
    @DisplayName("?????? ??????")
    public void deleteLecture() throws Exception {
        Account account = createAccount();
        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(account.getId()), account.getRoles());

        Lecture lecture = Lecture.builder()
                .id(1L)
                .instructor(account)
                .build();

        given(lectureService.getLectureById(1L)).willReturn(lecture);

        mockMvc.perform(delete("/lecture/delete")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .header("IsRefreshToken", "false")
                .param("id", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("lectureId").exists())
                .andDo(document("delete-lecture",
                        requestHeaders(
                                headerWithName("Authorization").description("access token ???"),
                                headerWithName("IsRefreshToken").description("token??? refresh token?????? ??????")
                        ),
                        requestParameters(
                                parameterWithName("id").description("?????? ????????? ?????? ????????? ???")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("\"HAL JSON ??????\"")
                        ),
                        responseFields(
                                fieldWithPath("lectureId").description("????????? ?????? ????????? ???"),
                                fieldWithPath("_links.self.href").description("?????? API URI")
                        )
                ));

        verify(lectureService, times(1)).deleteLectureById(anyLong());
    }

    @Test
    @DisplayName("????????? ?????? ?????????")
    public void upload() throws Exception {
        Account account = createAccount();
        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(account.getId()), account.getRoles());
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test data".getBytes());
        given(s3Uploader.upload(file, "lecture", account.getEmail())).willReturn("image file aws s3 url");

        mockMvc.perform(multipart("/lecture/upload")
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .header("IsRefreshToken", "false"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("?????? ?????? ?????? ??????")
    public void getLectureDetail() throws Exception {
        Account account = createAccount();
        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(account.getId()), account.getRoles());

        Lecture lecture = Lecture.builder()
                .title("??????1")
                .classKind("??????????????????")
                .groupName("AIDA")
                .certificateKind("LEVEL1")
                .description("?????? ??????")
                .price(300000)
                .region("??????")
                .instructor(Account.builder().id(10L).build())
                .lectureImages(List.of(LectureImage.builder().fileURI("File URL1").build()))
                .equipmentList(List.of(Equipment.builder().name("??????1").price(3000).build()))
                .build();

        given(lectureService.getLectureById(1L)).willReturn(lecture);

        mockMvc.perform(get("/lecture/detail")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .header("IsRefreshToken", "false")
                .param("id", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("get-lecture-detail",
                        requestHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("application json ??????"),
                                headerWithName("Authorization").description("access token ???"),
                                headerWithName("IsRefreshToken").description("token??? refresh token?????? ??????")
                        ),
                        requestParameters(
                                parameterWithName("id").description("lecture ????????? ???")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("HAL JSON ??????")
                        ),
                        responseFields(
                                fieldWithPath("id").description("?????? ????????? ID"),
                                fieldWithPath("title").description("?????? ??????"),
                                fieldWithPath("classKind").description("?????? ??????"),
                                fieldWithPath("groupName").description("?????? ?????? ??????"),
                                fieldWithPath("certificateKind").description("?????? ?????? ??? ?????? ?????????"),
                                fieldWithPath("description").description("?????? ??????"),
                                fieldWithPath("price").description("?????? ??????"),
                                fieldWithPath("period").description("?????? ??????"),
                                fieldWithPath("studentCount").description("?????? ?????? ?????????"),
                                fieldWithPath("region").description("?????? ??????"),
                                fieldWithPath("instructorId").description("?????? ????????? ID"),
                                fieldWithPath("lectureUrlList[0]").description("?????? ????????? URL"),
                                fieldWithPath("equipmentList[0].name").description("?????? ?????? ??????"),
                                fieldWithPath("equipmentList[0].price").description("?????? ?????? ??????"),
                                fieldWithPath("_links.self.href").description("?????? API URL")
                        )

                ));
    }

    @Test
    @DisplayName("?????? ????????? ?????? ??????")
    public void searchList() throws Exception {
        CostCondition costCondition = CostCondition.builder()
                .min(10000)
                .max(50000)
                .build();

        SearchCondition searchCondition = SearchCondition.builder()
                .groupName("AIDA")
                .certificateKind("LEVEL1")
                .region("??????")
                .costCondition(costCondition)
                .build();

        Pageable pageable = PageRequest.of(1, 5);
        Page<Lecture> lecturePage = createLecturePage(pageable);
        given(lectureService.searchListByCondition(searchCondition, pageable)).willReturn(lecturePage);

        mockMvc.perform(post("/lecture/list")
                .param("page", String.valueOf(pageable.getPageNumber()))
                .param("size", String.valueOf(pageable.getPageSize()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchCondition)))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("get-lecture-by-condition",
                        requestParameters(
                                parameterWithName("page").description("??? ?????? ?????????"),
                                parameterWithName("size").description("??? ???????????? ??????")
                        ),
                        requestFields(
                                fieldWithPath("groupName").description("?????? ?????? ??????"),
                                fieldWithPath("certificateKind").description("????????? ??????"),
                                fieldWithPath("region").description("?????? ??????"),
                                fieldWithPath("costCondition.max").description("????????? ?????? ??????"),
                                fieldWithPath("costCondition.min").description("????????? ?????? ??????")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("HAL JSON ??????")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.lectureSearchResultList[0].id").description("?????? ????????? ID"),
                                fieldWithPath("_embedded.lectureSearchResultList[0].title").description("?????? ??????"),
                                fieldWithPath("_embedded.lectureSearchResultList[0].classKind").description("?????? ??????"),
                                fieldWithPath("_embedded.lectureSearchResultList[0].groupName").description("?????? ??????"),
                                fieldWithPath("_embedded.lectureSearchResultList[0].certificateKind").description("????????? ??????"),
                                fieldWithPath("_embedded.lectureSearchResultList[0].price").description("?????? ??????"),
                                fieldWithPath("_embedded.lectureSearchResultList[0].region").description("?????? ??????"),
                                fieldWithPath("_embedded.lectureSearchResultList[0].imageURL").description("?????? ????????????"),
                                fieldWithPath("_links.first.href").description("??? ?????? ????????? URL"),
                                fieldWithPath("_links.prev.href").description("?????? ?????? ????????? URL"),
                                fieldWithPath("_links.self.href").description("?????? ????????? URL"),
                                fieldWithPath("_links.next.href").description("?????? ????????? URL"),
                                fieldWithPath("_links.last.href").description("????????? ????????? URL"),
                                fieldWithPath("page.size").description("??? ???????????? ??????"),
                                fieldWithPath("page.totalElements").description("?????? ?????? ?????? ?????? ???"),
                                fieldWithPath("page.totalPages").description("?????? ????????? ???"),
                                fieldWithPath("page.number").description("?????? ????????? ??????")
                        )
                ));
    }

    private Page<Lecture> createLecturePage(Pageable pageable) {
        List<Lecture> lectureList = new ArrayList<>();
        for (long i = 0; i < 15; i++) {
            LectureImage lectureImage = LectureImage.builder()
                    .fileURI("Image URL ??????")
                    .build();

            Lecture lecture = Lecture.builder()
                    .id(i)
                    .title("??????")
                    .description("??????")
                    .classKind("????????? ?????????")
                    .groupName("AIDA")
                    .certificateKind("Level1")
                    .price(100000)
                    .region("??????")
                    .lectureImages(List.of(lectureImage))
                    .build();
            lectureList.add(lecture);
        }

        long endIndex = (pageable.getOffset() + pageable.getPageSize()) > lectureList.size() ?
                lectureList.size() : pageable.getOffset() + pageable.getPageSize();

        return new PageImpl<>(lectureList.subList((int) pageable.getOffset(), (int) endIndex), pageable, lectureList.size());
    }

    @Test
    @DisplayName("?????? ????????? ?????? ?????? ??????")
    public void getLectureList() throws Exception {
        Account account = createAccount();
        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(account.getId()), account.getRoles());

        Pageable pageable = PageRequest.of(0, 5);
        Page<LectureInfo> lectureInfoPage = createLectureInfoPage(pageable);

        given(lectureService.getMyLectureInfoList(account, pageable)).willReturn(lectureInfoPage);

        mockMvc.perform(get("/lecture/manage/list")
                .param("page", String.valueOf(pageable.getPageNumber()))
                .param("size", String.valueOf(pageable.getPageSize()))
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .header("IsRefreshToken", "false"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("lecture-get-list-per-instructor",
                        requestHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("application json ??????"),
                                headerWithName("Authorization").description("access token ???"),
                                headerWithName("IsRefreshToken").description("token??? refresh token?????? ??????")
                        ),
                        requestParameters(
                                parameterWithName("page").description("??? ?????? ?????????"),
                                parameterWithName("size").description("??? ???????????? ??????")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.lectureInfoList[].lectureId").description("?????? ????????? ???"),
                                fieldWithPath("_embedded.lectureInfoList[].title").description("?????? ??????"),
                                fieldWithPath("_embedded.lectureInfoList[].groupName").description("?????? ??????"),
                                fieldWithPath("_embedded.lectureInfoList[].certificateKind").description("????????? ??????"),
                                fieldWithPath("_embedded.lectureInfoList[].cost").description("?????? ??????"),
                                fieldWithPath("_embedded.lectureInfoList[].isRentEquipment").description("?????? ?????? ??????"),
                                fieldWithPath("_embedded.lectureInfoList[].upcomingScheduleCount").description("???????????? ????????? ???"),
                                fieldWithPath("_links.self.href").description("?????? ????????? URL"),
                                fieldWithPath("page.size").description("??? ???????????? ??????"),
                                fieldWithPath("page.totalElements").description("?????? ?????? ?????? ?????? ???"),
                                fieldWithPath("page.totalPages").description("?????? ????????? ???"),
                                fieldWithPath("page.number").description("?????? ????????? ??????")
                        )
                ));
    }

    private Page<LectureInfo> createLectureInfoPage(Pageable pageable) {
        List<LectureInfo> lectureInfoList = new ArrayList<>();

        LectureInfo lectureInfo = LectureInfo.builder()
                .title("?????? ????????? ??????")
                .groupName("AIDA")
                .certificateKind("LEVEL1")
                .cost(100000)
                .upcomingScheduleCount(5)
                .isRentEquipment(true)
                .build();

        lectureInfoList.add(lectureInfo);

        return new PageImpl<>(lectureInfoList, pageable, lectureInfoList.size());
    }
}