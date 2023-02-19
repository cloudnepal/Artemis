package de.tum.in.www1.artemis.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileInputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.ResourceUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tum.in.www1.artemis.AbstractSpringIntegrationBambooBitbucketJiraTest;
import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.exam.Exam;
import de.tum.in.www1.artemis.domain.exam.ExamUser;
import de.tum.in.www1.artemis.domain.exam.StudentExam;
import de.tum.in.www1.artemis.programmingexercise.ProgrammingExerciseTestService;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.util.LocalRepository;
import de.tum.in.www1.artemis.web.rest.dto.ExamUserAttendanceCheckDTO;
import de.tum.in.www1.artemis.web.rest.dto.ExamUserDTO;
import de.tum.in.www1.artemis.web.rest.dto.ExamUsersNotFoundDTO;

class ExamUserIntegrationTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    private static final String TEST_PREFIX = "examuser";

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private StudentExamRepository studentExamRepository;

    @Autowired
    private ProgrammingExerciseTestService programmingExerciseTestService;

    private Course course1;

    private Exam exam1;

    private Course course2;

    private Exam exam2;

    private final List<LocalRepository> studentRepos = new ArrayList<>();

    private static final int NUMBER_OF_STUDENTS = 4;

    @BeforeEach
    void initTestCase() throws Exception {
        database.addUsers(TEST_PREFIX, NUMBER_OF_STUDENTS, 0, 0, 1);
        // Add users that are not in the course

        var student1 = database.getUserByLogin(TEST_PREFIX + "student1");
        var student2 = database.getUserByLogin(TEST_PREFIX + "student2");
        var student3 = database.getUserByLogin(TEST_PREFIX + "student3");
        var student4 = database.getUserByLogin(TEST_PREFIX + "student4");
        course1 = database.addEmptyCourse();

        // same registration number as in test pdf file
        student1.setGroups(Set.of(course1.getStudentGroupName()));
        student1.setRegistrationNumber("03756882");
        userRepo.save(student1);
        student2.setGroups(Set.of(course1.getStudentGroupName()));
        student2.setRegistrationNumber("03756883");
        userRepo.save(student2);
        student3.setGroups(Set.of(course1.getStudentGroupName()));
        student3.setRegistrationNumber("03756884");
        userRepo.save(student3);
        student4.setGroups(Set.of(course1.getStudentGroupName()));
        student4.setRegistrationNumber("03756885");
        userRepo.save(student4);

        exam1 = database.addActiveExamWithRegisteredUser(course1, student2);
        exam1 = database.addExerciseGroupsAndExercisesToExam(exam1, false);
        exam1 = examRepository.save(exam1);

        programmingExerciseTestService.setup(this, versionControlService, continuousIntegrationService, programmingExerciseStudentParticipationRepository);
        bitbucketRequestMockProvider.enableMockingOfRequests(true);
        bambooRequestMockProvider.enableMockingOfRequests(true);
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "instructor1", roles = "INSTRUCTOR")
    void testUpdateExamUser_DidCheckFields() throws Exception {
        ExamUserDTO examUserDTO = new ExamUserDTO(TEST_PREFIX + "student2", "", "", "", "", "", "", "", true, true, true, true, "");
        var examUserResponse = request.getMvc().perform(buildUpdateExamUser(examUserDTO, false, course1.getId(), exam1.getId())).andExpect(status().isOk()).andReturn();
        ExamUser examUser = mapper.readValue(examUserResponse.getResponse().getContentAsString(), ExamUser.class);
        assertThat(examUser.getDidCheckRegistrationNumber()).isTrue();
        assertThat(examUser.getDidCheckImage()).isTrue();
        assertThat(examUser.getDidCheckName()).isTrue();
        assertThat(examUser.getDidCheckLogin()).isTrue();
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "instructor1", roles = "INSTRUCTOR")
    void testAddStudentsToExamWithSeatAndRoomFields() throws Exception {
        List<ExamUserDTO> examUserDTOs = new ArrayList<>();
        ExamUserDTO examUserDTO1 = new ExamUserDTO(TEST_PREFIX + "student1", "", "", "03756882", "", "", "101", "11", true, true, true, true, "");
        ExamUserDTO examUserDTO2 = new ExamUserDTO(TEST_PREFIX + "student2", "", "", "03756883", "", "", "102", "11", true, true, true, true, "");
        examUserDTOs.add(examUserDTO1);
        examUserDTOs.add(examUserDTO2);

        List<ExamUserDTO> responseNotFoundExamUsers = request.postListWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam1.getId() + "/students", examUserDTOs,
                ExamUserDTO.class, HttpStatus.OK);
        assertThat(responseNotFoundExamUsers.size()).isEqualTo(0);
        Exam exam = examRepository.findWithExamUsersById(exam1.getId()).orElseThrow();
        var examUsers = exam.getExamUsers();
        assertThat(examUsers.size()).isEqualTo(2);

        examUsers.forEach(eu -> {
            assertThat(eu.getSigningImagePath()).isNullOrEmpty();
            assertThat(eu.getStudentImagePath()).isNullOrEmpty();
            assertThat(eu.getPlannedRoom()).isNotNull();
            assertThat(eu.getPlannedSeat()).isNotNull();
        });
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "instructor1", roles = "INSTRUCTOR")
    void testUploadExamUserImages() throws Exception {
        // registration number is important for the test, exam users should have same registration number as in the test pdf file
        // student2 already exists in the exam and there is no need to add it again
        List<ExamUserDTO> examUserDTOs = new ArrayList<>();
        ExamUserDTO examUserDTO1 = new ExamUserDTO(TEST_PREFIX + "student1", "", "", "03756882", "", "", "101", "11", true, true, true, true, "");
        ExamUserDTO examUserDTO3 = new ExamUserDTO(TEST_PREFIX + "student3", "", "", "03756884", "", "", "101", "11", true, true, true, true, "");
        ExamUserDTO examUserDTO4 = new ExamUserDTO(TEST_PREFIX + "student4", "", "", "03756885", "", "", "102", "11", true, true, true, true, "");
        examUserDTOs.add(examUserDTO1);
        examUserDTOs.add(examUserDTO3);
        examUserDTOs.add(examUserDTO4);

        // add students to exam with respective registration numbers, same as in pdf test file
        List<ExamUserDTO> responseNotFoundExamUsers = request.postListWithResponseBody("/api/courses/" + course1.getId() + "/exams/" + exam1.getId() + "/students", examUserDTOs,
                ExamUserDTO.class, HttpStatus.OK);
        assertThat(responseNotFoundExamUsers.size()).isEqualTo(0);

        // upload exam user images
        var imageUploadResponse = request.getMvc().perform(buildUploadExamUserImages(course1.getId(), exam1.getId())).andExpect(status().isOk()).andReturn();
        ExamUsersNotFoundDTO examUsersNotFoundDTO = mapper.readValue(imageUploadResponse.getResponse().getContentAsString(), ExamUsersNotFoundDTO.class);

        assertThat(examUsersNotFoundDTO.numberOfUsersNotFound()).isEqualTo(0);

        // check if exam users have been updated with the images
        Exam exam = examRepository.findByIdWithExamUsersElseThrow(exam1.getId());
        // 4 exam users, 3 new and 1 already existing
        assertThat(exam.getExamUsers().size()).isEqualTo(4);
        exam.getExamUsers().forEach(eu -> {
            assertThat(eu.getStudentImagePath()).isNotNull();
            assertThat(eu.getStudentImagePath()).isNotNull();
        });
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "instructor1", roles = "INSTRUCTOR")
    void testUpdateExamUserDidCheckFieldsAndSigningImage() throws Exception {
        ExamUserDTO examUserDTO = new ExamUserDTO(TEST_PREFIX + "student2", "", "", "", "", "", "", "", true, true, true, true, "");
        var examUserResponse = request.getMvc().perform(buildUpdateExamUser(examUserDTO, true, course1.getId(), exam1.getId())).andExpect(status().isOk()).andReturn();
        ExamUser examUser = mapper.readValue(examUserResponse.getResponse().getContentAsString(), ExamUser.class);
        assertThat(examUser.getDidCheckRegistrationNumber()).isTrue();
        assertThat(examUser.getDidCheckImage()).isTrue();
        assertThat(examUser.getDidCheckName()).isTrue();
        assertThat(examUser.getDidCheckLogin()).isTrue();
        assertThat(examUser.getSigningImagePath()).isNotNull();
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "instructor1", roles = "INSTRUCTOR")
    void testVerifyExamUserAttendance() throws Exception {
        List<StudentExam> studentExams = prepareStudentExamsForConduction(false, true);

        final HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "foo");
        headers.set("X-Artemis-Client-Fingerprint", "bar");
        headers.set("X-Forwarded-For", "10.0.28.1");

        for (var studentExam : studentExams) {
            var user = studentExam.getUser();
            database.changeUser(user.getLogin());
            var response = request.get("/api/courses/" + course2.getId() + "/exams/" + exam2.getId() + "/student-exams/" + studentExam.getId() + "/conduction", HttpStatus.OK,
                    StudentExam.class, headers);
            assertThat(response).isEqualTo(studentExam);
            assertThat(response.isStarted()).isTrue();
            assertThat(response.getExercises()).hasSize(exam2.getNumberOfExercisesInExam());

            assertThat(studentExamRepository.findById(studentExam.getId()).get().isStarted()).isTrue();
        }

        // change back to instructor user
        database.changeUser(TEST_PREFIX + "instructor1");
        // update exam user attendance
        ExamUserDTO examUserDTO = new ExamUserDTO(TEST_PREFIX + "student1", "", "", "", "", "", "", "", true, true, true, true, "");
        var examUserResponse = request.getMvc().perform(buildUpdateExamUser(examUserDTO, true, course2.getId(), exam2.getId())).andExpect(status().isOk()).andReturn();
        ExamUser examUser = mapper.readValue(examUserResponse.getResponse().getContentAsString(), ExamUser.class);
        assertThat(examUser.getDidCheckRegistrationNumber()).isTrue();
        assertThat(examUser.getDidCheckImage()).isTrue();
        assertThat(examUser.getDidCheckName()).isTrue();
        assertThat(examUser.getDidCheckLogin()).isTrue();

        // as instructor, verify the attendance of the students
        List<ExamUserAttendanceCheckDTO> examUsersWhoDidNotSign = request.getList("/api/courses/" + course2.getId() + "/exams/" + exam2.getId() + "/verify-exam-users",
                HttpStatus.OK, ExamUserAttendanceCheckDTO.class);
        // one student (student1) signed, the other 3 did not
        assertThat(examUsersWhoDidNotSign).hasSize(3);
        for (var examUserAttendanceCheckDTO : examUsersWhoDidNotSign) {
            assertThat(examUserAttendanceCheckDTO.started()).isTrue();
            assertThat(examUserAttendanceCheckDTO.login()).isNotEqualTo(TEST_PREFIX + "student1");
        }
    }

    private MockHttpServletRequestBuilder buildUpdateExamUser(@NotNull ExamUserDTO examUserDTO, boolean hasSigned, long courseId, long examId) throws Exception {
        var examUserPart = new MockMultipartFile("examUserDTO", "", MediaType.APPLICATION_JSON_VALUE, mapper.writeValueAsString(examUserDTO).getBytes());
        if (hasSigned) {
            var signingImage = loadFile("classpath:test-data/exam-users", "examUserSigningImage.png");
            return MockMvcRequestBuilders.multipart(HttpMethod.POST, "/api/courses/" + courseId + "/exams/" + examId + "/exam-users").file(examUserPart).file(signingImage)
                    .contentType(MediaType.MULTIPART_FORM_DATA_VALUE);
        }
        else {
            return MockMvcRequestBuilders.multipart(HttpMethod.POST, "/api/courses/" + courseId + "/exams/" + examId + "/exam-users").file(examUserPart)
                    .contentType(MediaType.MULTIPART_FORM_DATA_VALUE);
        }
    }

    private MockHttpServletRequestBuilder buildUploadExamUserImages(long courseId, long examId) throws Exception {
        var signingImage = loadFile("classpath:test-data/exam-users", "studentsWithImages.pdf");

        return MockMvcRequestBuilders.multipart(HttpMethod.POST, "/api/courses/" + courseId + "/exams/" + examId + "/exam-users-save-images").file(signingImage)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE);
    }

    private MockMultipartFile loadFile(String path, String fileName) throws Exception {
        File signingImage = new File(ResourceUtils.getFile(path), fileName);
        FileInputStream input = new FileInputStream(signingImage);
        return new MockMultipartFile("file", signingImage.getName(), "image/png", IOUtils.toByteArray(input));
    }

    private List<StudentExam> prepareStudentExamsForConduction(boolean early, boolean setFields) throws Exception {
        for (int i = 1; i <= NUMBER_OF_STUDENTS; i++) {
            bitbucketRequestMockProvider.mockUserExists(TEST_PREFIX + "student" + i);
        }

        ZonedDateTime examVisibleDate;
        ZonedDateTime examStartDate;
        ZonedDateTime examEndDate;
        if (early) {
            examStartDate = ZonedDateTime.now().plusHours(1);
            examEndDate = ZonedDateTime.now().plusHours(3);
        }
        else {
            // If the exam is prepared only 5 minutes before the release date, the repositories of the students are unlocked as well.
            examStartDate = ZonedDateTime.now().plusMinutes(6);
            examEndDate = ZonedDateTime.now().plusMinutes(8);
        }

        examVisibleDate = ZonedDateTime.now().minusMinutes(15);
        // --> 2 min = 120s working time

        // all registered students
        Set<User> registeredStudents = new HashSet<>();
        for (int i = 1; i <= NUMBER_OF_STUDENTS; i++) {
            registeredStudents.add(database.getUserByLogin(TEST_PREFIX + "student" + i));
        }

        List<StudentExam> studentExams = database.prepareStudentExamsForConduction(TEST_PREFIX, this, bitbucketRequestMockProvider, programmingExerciseTestService, request,
                examVisibleDate, examStartDate, examEndDate, registeredStudents, studentRepos);
        Exam exam = examRepository.findByIdElseThrow(studentExams.get(0).getExam().getId());
        Course course = exam.getCourse();

        if (!early) {
            // simulate "wait" for exam to start
            exam.setStartDate(ZonedDateTime.now());
            exam.setEndDate(ZonedDateTime.now().plusMinutes(2));
            examRepository.save(exam);
        }

        bitbucketRequestMockProvider.reset();

        if (setFields) {
            exam2 = exam;
            course2 = course;
        }
        return studentExams;
    }

}