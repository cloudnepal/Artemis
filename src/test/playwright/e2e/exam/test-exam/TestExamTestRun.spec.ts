import { Course } from 'app/entities/course.model';
import { Exam } from 'app/entities/exam.model';

import javaBuildErrorSubmission from '../../../fixtures/exercise/programming/java/build_error/submission.json';
import { Exercise, ExerciseType } from '../../../support/constants';
import { admin, instructor } from '../../../support/users';
import { generateUUID } from '../../../support/utils';
import { test } from '../../../support/fixtures';
import { StudentExam } from 'app/entities/student-exam.model';
import { expect } from '@playwright/test';

// Common primitives
const textFixture = 'loremIpsum.txt';
const examTitle = 'exam' + generateUUID();

test.describe('Test exam test run', () => {
    let course: Course;
    let exam: Exam;
    let testRun: StudentExam;
    let exerciseArray: Array<Exercise> = [];

    test.beforeEach('Create course', async ({ login, courseManagementAPIRequests, examAPIRequests, examExerciseGroupCreation }) => {
        await login(admin);
        course = await courseManagementAPIRequests.createCourse({ customizeGroups: true });
        const examConfig = {
            course,
            title: examTitle,
            testExam: true,
            examMaxPoints: 40,
            numberOfExercisesInExam: 4,
        };
        exam = await examAPIRequests.createExam(examConfig);
        Promise.all([
            await examExerciseGroupCreation.addGroupWithExercise(exam, ExerciseType.TEXT, { textFixture }),
            await examExerciseGroupCreation.addGroupWithExercise(exam, ExerciseType.PROGRAMMING, { submission: javaBuildErrorSubmission, practiceMode: true }),
            await examExerciseGroupCreation.addGroupWithExercise(exam, ExerciseType.QUIZ, { quizExerciseID: 0 }),
            await examExerciseGroupCreation.addGroupWithExercise(exam, ExerciseType.MODELING),
        ]).then((responses) => {
            exerciseArray = responses;
        });
    });

    test('Create a test run', async ({ login, page, examManagement, examTestRun }) => {
        await login(instructor);

        const minutes = 40;
        const seconds = 30;

        await page.goto(`/course-management/${course.id}/exams/${exam.id}`);
        await examManagement.openTestRun();
        await examTestRun.createTestRun();
        await examTestRun.setWorkingTimeMinutes(minutes);
        await examTestRun.setWorkingTimeSeconds(seconds);
        const testRunResponse = await examTestRun.confirmTestRun();
        const testRun: StudentExam = await testRunResponse.json();
        const testRunID = testRun.id!;

        expect(testRunResponse.status()).toBe(200);
        expect(testRun.testRun).toBe(true);
        expect(testRun.submitted).toBe(false);
        expect(testRun.workingTime).toBe(minutes * 60 + seconds);

        await expect(examTestRun.getWorkingTime(testRunID).getByText(`${minutes}min ${seconds}s`)).toBeVisible();
        await expect(examTestRun.getStarted(testRunID).getByText('No')).toBeVisible();
        await expect(examTestRun.getSubmitted(testRunID).getByText('No')).toBeVisible();
    });

    test.describe('Manage a test run', () => {
        test.beforeEach('Create test run instance', async ({ login, courseManagementAPIRequests }) => {
            await login(instructor);
            testRun = await courseManagementAPIRequests.createExamTestRun(exam, exerciseArray);
        });

        test('Change test run working time', async ({ login, examTestRun }) => {
            const hour = 1;
            const minutes = 20;
            const seconds = 45;

            await login(instructor);
            await examTestRun.openTestRunPage(course, exam);
            await examTestRun.changeWorkingTime(testRun.id!);
            await examTestRun.setWorkingTimeHours(hour);
            await examTestRun.setWorkingTimeMinutes(minutes);
            await examTestRun.setWorkingTimeSeconds(seconds);
            const testRunResponse = await examTestRun.saveTestRun();
            const updatedTestRun: StudentExam = await testRunResponse.json();

            expect(testRunResponse.status()).toBe(200);
            expect(updatedTestRun.id).toBe(testRun.id);
            expect(updatedTestRun.workingTime).toBe(hour * 3600 + minutes * 60 + seconds);

            await examTestRun.openTestRunPage(course, exam);
            await expect(examTestRun.getWorkingTime(testRun.id!).getByText(`${hour}h ${minutes}min ${seconds}s`)).toBeVisible();
            await expect(examTestRun.getStarted(testRun.id!).getByText('No')).toBeVisible();
            await expect(examTestRun.getSubmitted(testRun.id!).getByText('No')).toBeVisible();
        });

        test('Conducts a test run', async ({ examTestRun, examNavigation, examParticipation }) => {
            await examTestRun.startParticipation(instructor, course, exam, testRun.id!);
            await expect(examTestRun.getTestRunRibbon().getByText('Test Run')).toBeVisible();

            for (let j = 0; j < exerciseArray.length; j++) {
                const exercise = exerciseArray[j];
                await examNavigation.openExerciseAtIndex(j);
                await examParticipation.makeSubmission(exercise.id!, exercise.type!, exercise.additionalData);
            }
            await examParticipation.handInEarly();
            for (let j = 0; j < exerciseArray.length; j++) {
                const exercise = exerciseArray[j];
                await examParticipation.verifyExerciseTitleOnFinalPage(exercise.id!, exercise.exerciseGroup!.title!);
                if (exercise.type === ExerciseType.TEXT) {
                    await examParticipation.verifyTextExerciseOnFinalPage(exercise.id!, exercise.additionalData!.textFixture!);
                }
            }
            await examParticipation.checkExamTitle(examTitle);
            await examTestRun.openTestRunPage(course, exam);
            await expect(examTestRun.getStarted(testRun.id!).getByText('Yes')).toBeVisible();
            await expect(examTestRun.getSubmitted(testRun.id!).getByText('Yes')).toBeVisible();
        });

        test('Deletes a test run', async ({ login, examTestRun }) => {
            await login(instructor);
            await examTestRun.openTestRunPage(course, exam);
            await expect(examTestRun.getTestRunIdElement(testRun.id!)).toBeVisible();
            await examTestRun.deleteTestRun(testRun.id!);
            await expect(examTestRun.getTestRun(testRun.id!)).not.toBeAttached();
        });
    });

    test.afterEach('Delete course', async ({ courseManagementAPIRequests }) => {
        await courseManagementAPIRequests.deleteCourse(course, admin);
    });
});