#!/usr/bin/env node
import {
  registerCase,
  runSuite,
  assertEqual,
  assertStatus,
  assertTrue,
  assertFalse,
  assertHasKeys
} from '../lib/test-harness.mjs';

import {
  sharedState,
  setLlmMockMode,
  runJvmRefreshTokenCleanup,
  getFixtureIds
} from './helpers/backend-psc-helpers.mjs';

// ============================================================================
// API-043: ApiInfoController (PUBLIC)
// ============================================================================
registerCase('API-043', 'GET / - Public API system status and entry info', async ({ apiCall }) => {
  const res = await apiCall('/');
  assertStatus(res.status, 200, 'Entry endpoint must return HTTP 200');
  assertTrue(res.json?.success, 'Response success must be true');
  assertHasKeys(res.json?.data, ['name', 'status', 'swaggerUrl', 'healthUrl']);
  assertEqual(res.json.data.name, 'V-SAT Compass API', 'API name matches');
  assertEqual(res.json.data.status, 'UP', 'API status must be UP');
});

// ============================================================================
// API-044 .. API-052: AuthController
// ============================================================================
registerCase('API-044', 'POST /auth/register - Register new student account', async ({ apiCall }) => {
  const res = await apiCall('/auth/register', {
    method: 'POST',
    body: {
      email: sharedState.tempUser.email,
      password: sharedState.tempUser.password,
      fullName: sharedState.tempUser.fullName,
      phone: sharedState.tempUser.phone
    }
  });

  assertStatus(res.status, 201, 'Registration must return HTTP 201 CREATED');
  assertTrue(res.json?.success, 'Registration success must be true');
  assertHasKeys(res.json?.data, ['accessToken', 'refreshToken', 'user']);
  assertTrue(Boolean(res.json.data.accessToken), 'Access token present');

  sharedState.tempUser.accessToken = res.json.data.accessToken;
  sharedState.tempUser.refreshToken = res.json.data.refreshToken;
});

registerCase('API-045', 'POST /auth/login - Authenticate user and issue JWT token pair', async ({ apiCall }) => {
  const res = await apiCall('/auth/login', {
    method: 'POST',
    body: {
      email: sharedState.tempUser.email,
      password: sharedState.tempUser.password,
      deviceInfo: 'AcceptanceHarness/API-045'
    }
  });

  assertStatus(res.status, 200, 'Login must return HTTP 200');
  assertTrue(res.json?.success, 'Login success must be true');
  assertHasKeys(res.json?.data, ['accessToken', 'refreshToken', 'user']);
  assertTrue(Boolean(res.json.data.accessToken), 'Access token returned on login');

  sharedState.tempUser.accessToken = res.json.data.accessToken;
  sharedState.tempUser.refreshToken = res.json.data.refreshToken;
  sharedState.tempUser.userId = res.json.data.user?.id;
});

registerCase('API-046', 'POST /auth/refresh - Refresh access token using valid refresh token', async ({ apiCall }) => {
  const res = await apiCall('/auth/refresh', {
    method: 'POST',
    body: { refreshToken: sharedState.tempUser.refreshToken }
  });

  assertStatus(res.status, 200, 'Refresh must return HTTP 200');
  assertTrue(res.json?.success, 'Refresh response must succeed');
  assertHasKeys(res.json?.data, ['accessToken', 'refreshToken']);
  assertTrue(Boolean(res.json.data.accessToken), 'New access token must be present');

  sharedState.tempUser.accessToken = res.json.data.accessToken;
  sharedState.tempUser.refreshToken = res.json.data.refreshToken;
});

registerCase('API-047', 'POST /auth/logout - Revoke refresh token and terminate session', async ({ apiCall }) => {
  const res = await apiCall('/auth/logout', {
    method: 'POST',
    body: { refreshToken: sharedState.tempUser.refreshToken }
  });

  assertStatus(res.status, 200, 'Logout must return HTTP 200');
  assertTrue(res.json?.success, 'Logout success must be true');
});

registerCase('API-048', 'GET /auth/me - Authenticated user profile and RBAC enforcement', async ({ apiCall }) => {
  // 1. Unauthenticated request must fail
  const unauthRes = await apiCall('/auth/me');
  assertStatus(unauthRes.status, 401, 'Anonymous request must return HTTP 401');

  // 2. Authenticated request as STUDENT
  const res = await apiCall('/auth/me', {}, 'STUDENT');
  assertStatus(res.status, 200, 'Student profile request must return HTTP 200');
  assertTrue(res.json?.success, 'Profile response success must be true');
  assertHasKeys(res.json?.data, ['id', 'email', 'fullName', 'role']);
  assertEqual(res.json.data.email, 'student@vsat.com', 'Matches student fixture email');
  assertEqual(res.json.data.role, 'STUDENT', 'User role must be STUDENT');
});

registerCase('API-049', 'PUT /auth/me - Update authenticated user personal profile', async ({ apiCall }) => {
  // Log back in with temp user to get active token
  const loginRes = await apiCall('/auth/login', {
    method: 'POST',
    body: {
      email: sharedState.tempUser.email,
      password: sharedState.tempUser.password,
      deviceInfo: 'AcceptanceHarness/API-049'
    }
  });
  sharedState.tempUser.accessToken = loginRes.json.data.accessToken;

  const res = await apiCall('/auth/me', {
    method: 'PUT',
    headers: { Authorization: `Bearer ${sharedState.tempUser.accessToken}` },
    body: { fullName: 'Học Sinh Đã Cập Nhật', phone: '0909999999' }
  });

  assertStatus(res.status, 200, 'Update profile must return HTTP 200');
  assertTrue(res.json?.success, 'Update profile success must be true');
  assertEqual(res.json.data.fullName, 'Học Sinh Đã Cập Nhật', 'Full name updated');
});

registerCase('API-050', 'PUT /auth/me/password - Change user password and verify credentials', async ({ apiCall }) => {
  const newPass = 'NewPassw0rd456!';
  const res = await apiCall('/auth/me/password', {
    method: 'PUT',
    headers: { Authorization: `Bearer ${sharedState.tempUser.accessToken}` },
    body: {
      oldPassword: sharedState.tempUser.password,
      currentPassword: sharedState.tempUser.password,
      newPassword: newPass
    }
  });

  assertStatus(res.status, 200, 'Change password must return HTTP 200');
  assertTrue(res.json?.success, 'Change password success must be true');

  // Verify login with new password succeeds
  const verifyLogin = await apiCall('/auth/login', {
    method: 'POST',
    body: { email: sharedState.tempUser.email, password: newPass }
  });
  assertStatus(verifyLogin.status, 200, 'Login with new password must succeed');
  sharedState.tempUser.password = newPass;
  sharedState.tempUser.accessToken = verifyLogin.json.data.accessToken;
});

registerCase('API-051', 'GET /auth/me/leaderboard - Get leaderboard opt-in state', async ({ apiCall }) => {
  const res = await apiCall('/auth/me/leaderboard', {}, 'STUDENT');
  assertStatus(res.status, 200, 'Leaderboard opt-in query must return HTTP 200');
  assertTrue(res.json?.success, 'Query success must be true');
  const optInVal = res.json?.data?.opt_in ?? res.json?.data?.optIn;
  assertTrue(typeof optInVal === 'boolean', 'optIn must be boolean');
});

registerCase('API-052', 'PATCH /auth/me/leaderboard - Update leaderboard opt-in preference', async ({ apiCall }) => {
  const res = await apiCall('/auth/me/leaderboard', {
    method: 'PATCH',
    headers: { Authorization: `Bearer ${sharedState.tempUser.accessToken}` },
    body: { optIn: true, opt_in: true }
  });

  assertStatus(res.status, 200, 'Updating opt-in must return HTTP 200');
  assertTrue(res.json?.success, 'Update success must be true');
  const optInVal = res.json?.data?.opt_in ?? res.json?.data?.optIn;
  assertEqual(optInVal, true, 'Opt-in must be true');
});

// ============================================================================
// API-053 .. API-057: CollaboratorQuestionController
// ============================================================================
registerCase('API-053', 'POST /collaborator/questions - Create authoring question draft and enforce RBAC', async ({ apiCall, queryDb }) => {
  const fixtures = getFixtureIds(queryDb);

  // 1. RBAC: STUDENT cannot create collaborator questions (403)
  const studentRes = await apiCall('/collaborator/questions', {
    method: 'POST',
    body: { subjectId: fixtures.subjectId, topicId: fixtures.topicId }
  }, 'STUDENT');
  assertStatus(studentRes.status, 403, 'STUDENT role must be forbidden (HTTP 403)');

  // 2. COLLABORATOR creates draft question
  const createPayload = {
    subjectId: fixtures.subjectId,
    topicId: fixtures.topicId,
    difficulty: 'MEDIUM',
    questionType: 'SINGLE_CHOICE',
    questionText: `Câu hỏi kiểm thử cộng tác viên [${Date.now()}]: Giải phương trình bậc nhất $2x - 4 = 0$.`,
    options: [
      { optionLabel: 'A', optionText: 'x = 1', isCorrect: false, displayOrder: 1 },
      { optionLabel: 'B', optionText: 'x = 2', isCorrect: true, displayOrder: 2 },
      { optionLabel: 'C', optionText: 'x = 3', isCorrect: false, displayOrder: 3 },
      { optionLabel: 'D', optionText: 'x = 4', isCorrect: false, displayOrder: 4 }
    ]
  };

  const res = await apiCall('/collaborator/questions', {
    method: 'POST',
    body: createPayload
  }, 'COLLABORATOR');

  assertStatus(res.status, 201, 'Question creation must return HTTP 201 CREATED');
  assertTrue(res.json?.success, 'Creation success must be true');
  assertHasKeys(res.json?.data, ['id', 'status', 'questionText', 'questionType']);
  assertEqual(res.json.data.status, 'DRAFT', 'Initial status must be DRAFT');

  sharedState.createdCollabQuestionId = res.json.data.id;
});

registerCase('API-054', 'GET /collaborator/questions - List collaborator questions and RBAC', async ({ apiCall }) => {
  // 1. RBAC check
  const rbacRes = await apiCall('/collaborator/questions', {}, 'STUDENT');
  assertStatus(rbacRes.status, 403, 'STUDENT must be forbidden');

  // 2. Fetch list as COLLABORATOR
  const res = await apiCall('/collaborator/questions', {}, 'COLLABORATOR');
  assertStatus(res.status, 200, 'Listing questions must return HTTP 200');
  assertTrue(res.json?.success, 'Response success must be true');
  assertTrue(Array.isArray(res.json.data.content), 'Content must be an array');
});

registerCase('API-055', 'GET /collaborator/questions/{id} - Get collaborator question detail and RBAC', async ({ apiCall }) => {
  const qId = sharedState.createdCollabQuestionId;
  assertTrue(Boolean(qId), 'Created question ID must exist from previous step');

  // 1. RBAC check
  const rbacRes = await apiCall(`/collaborator/questions/${qId}`, {}, 'STUDENT');
  assertStatus(rbacRes.status, 403, 'STUDENT must be forbidden');

  // 2. Fetch detail as COLLABORATOR
  const res = await apiCall(`/collaborator/questions/${qId}`, {}, 'COLLABORATOR');
  assertStatus(res.status, 200, 'Detail query must return HTTP 200');
  assertTrue(res.json?.success, 'Success must be true');
  assertEqual(Number(res.json.data.id), Number(qId), 'Returned question ID matches');
});

registerCase('API-056', 'PUT /collaborator/questions/{id} - Update draft question and RBAC', async ({ apiCall, queryDb }) => {
  const qId = sharedState.createdCollabQuestionId;
  const fixtures = getFixtureIds(queryDb);

  // 1. RBAC check
  const rbacRes = await apiCall(`/collaborator/questions/${qId}`, {
    method: 'PUT',
    body: { questionText: 'Hacked by student' }
  }, 'STUDENT');
  assertStatus(rbacRes.status, 403, 'STUDENT must be forbidden');

  // 2. Update as COLLABORATOR
  const updatePayload = {
    subjectId: fixtures.subjectId,
    topicId: fixtures.topicId,
    difficulty: 'MEDIUM',
    questionType: 'SINGLE_CHOICE',
    questionText: `Câu hỏi đã cập nhật nội dung: Tìm $x$ sao cho $2x - 4 = 0$.`,
    options: [
      { optionLabel: 'A', optionText: 'x = 1', isCorrect: false, displayOrder: 1 },
      { optionLabel: 'B', optionText: 'x = 2', isCorrect: true, displayOrder: 2 },
      { optionLabel: 'C', optionText: 'x = 3', isCorrect: false, displayOrder: 3 },
      { optionLabel: 'D', optionText: 'x = 4', isCorrect: false, displayOrder: 4 }
    ]
  };

  const res = await apiCall(`/collaborator/questions/${qId}`, {
    method: 'PUT',
    body: updatePayload
  }, 'COLLABORATOR');

  assertStatus(res.status, 200, 'Update must return HTTP 200');
  assertTrue(res.json?.success, 'Update success must be true');
});

registerCase('API-057', 'POST /collaborator/questions/{id}/submit-for-review - Submit question for admin review', async ({ apiCall }) => {
  const qId = sharedState.createdCollabQuestionId;

  // 1. RBAC check
  const rbacRes = await apiCall(`/collaborator/questions/${qId}/submit-for-review`, {
    method: 'POST'
  }, 'STUDENT');
  assertStatus(rbacRes.status, 403, 'STUDENT must be forbidden');

  // 2. Submit for review as COLLABORATOR
  const res = await apiCall(`/collaborator/questions/${qId}/submit-for-review`, {
    method: 'POST'
  }, 'COLLABORATOR');

  assertStatus(res.status, 200, 'Submit for review must return HTTP 200');
  assertTrue(res.json?.success, 'Submission success must be true');
  assertEqual(res.json.data.status, 'PENDING_REVIEW', 'Status transitions to PENDING_REVIEW');
});

// ============================================================================
// API-058 .. API-059: ExamPublicController (PUBLIC)
// ============================================================================
registerCase('API-058', 'GET /exams - Browse published exams catalog', async ({ apiCall }) => {
  const res = await apiCall('/exams');
  assertStatus(res.status, 200, 'Public exams list must return HTTP 200');
  assertTrue(res.json?.success, 'Response success must be true');
  assertTrue(Array.isArray(res.json.data.content), 'Content must be an array');
  assertTrue(res.json.data.content.length > 0, 'Catalog contains published exams');
});

registerCase('API-059', 'GET /exams/{id} - Get public overview of published exam', async ({ apiCall, queryDb }) => {
  const fixtures = getFixtureIds(queryDb);
  const res = await apiCall(`/exams/${fixtures.examId}`);

  assertStatus(res.status, 200, 'Exam detail query must return HTTP 200');
  assertTrue(res.json?.success, 'Detail success must be true');
  assertHasKeys(res.json?.data, ['id', 'examCode', 'title', 'difficulty', 'pricingType']);
  assertEqual(res.json.data.examCode, 'CLEAN_ROOM_MOCK_001', 'Exam code matches clean room fixture');
});

// ============================================================================
// API-060: AiController (/ai/explain)
// ============================================================================
registerCase('API-060', 'POST /ai/explain - Generate AI pedagogical explanation with LaTeX', async ({ apiCall, queryDb }) => {
  await setLlmMockMode('normal');
  const fixtures = getFixtureIds(queryDb);

  // 1. RBAC check: Anonymous (401) and COLLABORATOR (403)
  const unauthRes = await apiCall('/ai/explain', {
    method: 'POST',
    body: { questionId: fixtures.questionId }
  });
  assertStatus(unauthRes.status, 401, 'Anonymous access must return HTTP 401');

  const collabRes = await apiCall('/ai/explain', {
    method: 'POST',
    body: { questionId: fixtures.questionId }
  }, 'COLLABORATOR');
  assertStatus(collabRes.status, 403, 'COLLABORATOR role must return HTTP 403');

  // 2. STUDENT requests explanation
  const res = await apiCall('/ai/explain', {
    method: 'POST',
    body: {
      questionId: fixtures.questionId,
      selectedOptionId: fixtures.optionId
    }
  }, 'STUDENT');

  assertStatus(res.status, 200, 'AI explanation must return HTTP 200');
  assertTrue(res.json?.success, 'AI explain success must be true');
  assertHasKeys(res.json?.data, ['questionId', 'explanation']);
  assertTrue(Boolean(res.json.data.explanation?.trim()), 'Explanation must not be empty');
});

// ============================================================================
// API-061: ExamDownloadController (/exams/{id}/download-bundle)
// ============================================================================
registerCase('API-061', 'GET /exams/{id}/download-bundle - Download offline exam bundle and enforce RBAC', async ({ apiCall, queryDb }) => {
  const fixtures = getFixtureIds(queryDb);

  // 1. RBAC check: COLLABORATOR is forbidden (403)
  const collabRes = await apiCall(`/exams/${fixtures.examId}/download-bundle`, {}, 'COLLABORATOR');
  assertStatus(collabRes.status, 403, 'COLLABORATOR role forbidden from downloading bundle');

  // 2. STUDENT downloads bundle
  const res = await apiCall(`/exams/${fixtures.examId}/download-bundle`, {}, 'STUDENT');
  assertStatus(res.status, 200, 'Student download bundle must return HTTP 200');
  assertTrue(res.json?.success, 'Bundle response success must be true');
  assertHasKeys(res.json?.data, ['id', 'examCode', 'questions']);
  assertEqual(res.json.data.examCode, 'CLEAN_ROOM_MOCK_001', 'Bundle matches exam code');
  assertTrue(Array.isArray(res.json.data.questions), 'Questions array is present');
  assertTrue(res.json.data.questions.length > 0, 'Bundle contains questions');
});

// ============================================================================
// API-062: LeaderboardController (/leaderboard)
// ============================================================================
registerCase('API-062', 'GET /leaderboard - Get top ranked students and enforce RBAC', async ({ apiCall }) => {
  // 1. RBAC: COLLABORATOR forbidden (403)
  const collabRes = await apiCall('/leaderboard', {}, 'COLLABORATOR');
  assertStatus(collabRes.status, 403, 'COLLABORATOR role forbidden from student leaderboard');

  // 2. STUDENT gets leaderboard
  const res = await apiCall('/leaderboard?limit=10', {}, 'STUDENT');
  assertStatus(res.status, 200, 'Leaderboard must return HTTP 200');
  assertTrue(res.json?.success, 'Leaderboard success must be true');
  assertTrue(Array.isArray(res.json.data), 'Leaderboard data must be an array');
});

// ============================================================================
// API-063 .. API-064: MyStatsController
// ============================================================================
registerCase('API-063', 'GET /my-stats/topics - Student performance metrics by syllabus topic', async ({ apiCall }) => {
  const res = await apiCall('/my-stats/topics', {}, 'STUDENT');
  assertStatus(res.status, 200, 'My stats topics query must return HTTP 200');
  assertTrue(res.json?.success, 'Stats query success must be true');
  assertTrue(Array.isArray(res.json.data), 'Topic stats must be an array');
});

registerCase('API-064', 'GET /my-stats/weak-topics - Weakest syllabus topics identification', async ({ apiCall }) => {
  const res = await apiCall('/my-stats/weak-topics?limit=5', {}, 'STUDENT');
  assertStatus(res.status, 200, 'Weak topics query must return HTTP 200');
  assertTrue(res.json?.success, 'Weak topics query success must be true');
  assertTrue(Array.isArray(res.json.data), 'Weak topics data must be an array');
});

// ============================================================================
// API-065 .. API-066: PracticeController
// ============================================================================
registerCase('API-065', 'GET /practice/questions - Fetch randomized practice questions and RBAC', async ({ apiCall, queryDb }) => {
  const fixtures = getFixtureIds(queryDb);

  // 1. RBAC: COLLABORATOR forbidden
  const collabRes = await apiCall(`/practice/questions?topicId=${fixtures.topicId}`, {}, 'COLLABORATOR');
  assertStatus(collabRes.status, 403, 'COLLABORATOR forbidden from practice questions');

  // 2. STUDENT queries practice questions
  const res = await apiCall(`/practice/questions?topicId=${fixtures.topicId}&limit=2`, {}, 'STUDENT');
  assertStatus(res.status, 200, 'Practice questions must return HTTP 200');
  assertTrue(res.json?.success, 'Practice query success must be true');
  assertTrue(Array.isArray(res.json.data), 'Practice questions must be an array');
});

registerCase('API-066', 'POST /practice/grade - Instantly grade submitted practice answer', async ({ apiCall, queryDb }) => {
  const fixtures = getFixtureIds(queryDb);

  // 1. RBAC: COLLABORATOR forbidden
  const collabRes = await apiCall('/practice/grade', {
    method: 'POST',
    body: { questionId: fixtures.questionId, selectedOptionId: fixtures.optionId }
  }, 'COLLABORATOR');
  assertStatus(collabRes.status, 403, 'COLLABORATOR forbidden from grading practice');

  // 2. STUDENT submits answer for instant grading
  const res = await apiCall('/practice/grade', {
    method: 'POST',
    body: {
      questionId: fixtures.questionId,
      selectedOptionId: fixtures.optionId
    }
  }, 'STUDENT');

  assertStatus(res.status, 200, 'Grading response must return HTTP 200');
  assertTrue(res.json?.success, 'Grading success must be true');
  assertHasKeys(res.json?.data, ['correct', 'explanation']);
  assertEqual(res.json.data.correct, true, 'Graded answer is correct');
});

// ============================================================================
// API-067 .. API-072: SessionController
// ============================================================================
registerCase('API-067', 'POST /sessions/start - Start official exam attempt session', async ({ apiCall, queryDb }) => {
  const fixtures = getFixtureIds(queryDb);
  const res = await apiCall('/sessions/start', {
    method: 'POST',
    body: {
      examId: fixtures.examId,
      mode: 'MOCK_EXAM'
    }
  }, 'STUDENT');

  assertStatus(res.status, 201, 'Starting session must return HTTP 201 CREATED');
  assertTrue(res.json?.success, 'Start session success must be true');
  assertHasKeys(res.json?.data, ['id', 'status', 'examId']);
  assertEqual(res.json.data.status, 'IN_PROGRESS', 'New session status is IN_PROGRESS');

  sharedState.createdSessionId = res.json.data.id;
});

registerCase('API-068', 'POST /sessions/{sessionId}/client-submit - Submit offline-completed session batch', async ({ apiCall, queryDb }) => {
  const fixtures = getFixtureIds(queryDb);

  // Start fresh session to client-submit offline results
  const startRes = await apiCall('/sessions/start', {
    method: 'POST',
    body: { examId: fixtures.examId, mode: 'MOCK_EXAM' }
  }, 'STUDENT');
  const offlineSessionId = startRes.json.data.id;

  const res = await apiCall(`/sessions/${offlineSessionId}/client-submit`, {
    method: 'POST',
    body: {
      score: 100.0,
      correctCount: 1,
      totalQuestions: 1,
      timeSpentSeconds: 120,
      answers: [
        {
          questionId: fixtures.questionId,
          selectedOptionId: fixtures.optionId,
          questionOrder: 1,
          bookmarked: false
        }
      ]
    }
  }, 'STUDENT');

  assertStatus(res.status, 200, 'Client submit must return HTTP 200');
  assertTrue(res.json?.success, 'Client submit success must be true');
  assertEqual(res.json.data.status, 'SUBMITTED', 'Session transitions to SUBMITTED');
});

registerCase('API-069', 'POST /sessions/{sessionId}/answers - Save individual answer in live session', async ({ apiCall, queryDb }) => {
  const sId = sharedState.createdSessionId;
  const fixtures = getFixtureIds(queryDb);

  const res = await apiCall(`/sessions/${sId}/answers`, {
    method: 'POST',
    body: {
      questionId: fixtures.questionId,
      selectedOptionId: fixtures.optionId,
      timeSpentSeconds: 30
    }
  }, 'STUDENT');

  assertStatus(res.status, 200, 'Saving answer must return HTTP 200');
  assertTrue(res.json?.success, 'Save answer success must be true');
});

registerCase('API-070', 'POST /sessions/{sessionId}/submit - Finalize live exam session and score', async ({ apiCall }) => {
  const sId = sharedState.createdSessionId;
  const res = await apiCall(`/sessions/${sId}/submit`, { method: 'POST' }, 'STUDENT');

  assertStatus(res.status, 200, 'Server submit must return HTTP 200');
  assertTrue(res.json?.success, 'Submit success must be true');
  assertEqual(res.json.data.status, 'SUBMITTED', 'Session finalized as SUBMITTED');
  assertTrue(res.json.data.score !== undefined, 'Score must be calculated');
});

registerCase('API-071', 'GET /sessions/{sessionId}/questions/{questionId} - Fetch in-session question content', async ({ apiCall, queryDb }) => {
  const fixtures = getFixtureIds(queryDb);

  // Start active session to fetch in-session question
  const startRes = await apiCall('/sessions/start', {
    method: 'POST',
    body: { examId: fixtures.examId, mode: 'MOCK_EXAM' }
  }, 'STUDENT');
  const activeSessionId = startRes.json.data.id;

  const res = await apiCall(`/sessions/${activeSessionId}/questions/${fixtures.questionId}`, {}, 'STUDENT');
  assertStatus(res.status, 200, 'In-session question fetch must return HTTP 200');
  assertTrue(res.json?.success, 'Question fetch success must be true');
  assertEqual(Number(res.json.data.id || res.json.data.questionId), Number(fixtures.questionId), 'Question ID matches');
  const questionContent = res.json.data.content || res.json.data.questionText;
  assertTrue(Boolean(questionContent), 'Question text is present');
});

registerCase('API-072', 'GET /sessions/{sessionId}/answer-keys - Retrieve answer keys after submission', async ({ apiCall, queryDb }) => {
  let sId = sharedState.createdSessionId; // Completed in API-070
  if (!sId) {
    const fixtures = getFixtureIds(queryDb);
    const startRes = await apiCall('/sessions/start', {
      method: 'POST',
      body: { examId: fixtures.examId, mode: 'MOCK_EXAM' }
    }, 'STUDENT');
    sId = startRes.json?.data?.id;
    if (sId) {
      await apiCall(`/sessions/${sId}/submit`, { method: 'POST' }, 'STUDENT');
      sharedState.createdSessionId = sId;
    }
  }
  const res = await apiCall(`/sessions/${sId}/answer-keys`, {}, 'STUDENT');

  assertStatus(res.status, 200, 'Answer keys retrieval must return HTTP 200');
  assertTrue(res.json?.success, 'Answer keys query success must be true');
  assertHasKeys(res.json?.data, ['sessionId', 'questions']);
  assertTrue(Array.isArray(res.json.data.questions), 'Questions answer keys array returned');
});

// ============================================================================
// API-073: TicketController
// ============================================================================
registerCase('API-073', 'POST /tickets - Submit student feedback / error report ticket and enforce RBAC', async ({ apiCall, queryDb }) => {
  const fixtures = getFixtureIds(queryDb);

  // 1. RBAC check: COLLABORATOR forbidden
  const collabRes = await apiCall('/tickets', {
    method: 'POST',
    body: { questionId: fixtures.questionId, message: 'Báo lỗi từ cộng tác viên' }
  }, 'COLLABORATOR');
  assertStatus(collabRes.status, 403, 'COLLABORATOR forbidden from submitting student ticket');

  // 2. STUDENT creates ticket
  const res = await apiCall('/tickets', {
    method: 'POST',
    body: {
      questionId: fixtures.questionId,
      message: `Báo lỗi đề thi mẫu từ acceptance test suite lúc ${new Date().toISOString()}`
    }
  }, 'STUDENT');

  assertStatus(res.status, 201, 'Ticket creation must return HTTP 201 CREATED');
  assertTrue(res.json?.success, 'Ticket creation success must be true');
  assertHasKeys(res.json?.data, ['id', 'status', 'message']);
  assertEqual(res.json.data.status, 'OPEN', 'Initial ticket status must be OPEN');
});

// ============================================================================
// API-074 .. API-076: SubjectController (PUBLIC)
// ============================================================================
registerCase('API-074', 'GET /subjects - List active examination subjects', async ({ apiCall }) => {
  const res = await apiCall('/subjects');
  assertStatus(res.status, 200, 'Subjects query must return HTTP 200');
  assertTrue(res.json?.success, 'Subjects query success must be true');
  assertTrue(Array.isArray(res.json.data), 'Subjects data is an array');

  const mathSubject = res.json.data.find(s => s.code === 'MATH');
  assertTrue(Boolean(mathSubject), 'MATH subject found in active subjects list');
  assertEqual(mathSubject.name, 'Toán', 'MATH subject title matches');
});

registerCase('API-075', 'GET /subjects/{subjectId}/topics - List curriculum topics under a subject', async ({ apiCall, queryDb }) => {
  const fixtures = getFixtureIds(queryDb);
  const res = await apiCall(`/subjects/${fixtures.subjectId}/topics`);

  assertStatus(res.status, 200, 'Topics query must return HTTP 200');
  assertTrue(res.json?.success, 'Topics query success must be true');
  assertTrue(Array.isArray(res.json.data), 'Topics data is an array');

  const algTopic = res.json.data.find(t => t.code === 'MATH_ALG');
  assertTrue(Boolean(algTopic), 'MATH_ALG topic found under MATH subject');
});

registerCase('API-076', 'GET /subjects/{subjectId}/topics/{topicId}/subtopics - List subtopics under topic', async ({ apiCall, queryDb }) => {
  const fixtures = getFixtureIds(queryDb);
  const res = await apiCall(`/subjects/${fixtures.subjectId}/topics/${fixtures.topicCalcId}/subtopics`);

  assertStatus(res.status, 200, 'Subtopics query must return HTTP 200');
  assertTrue(res.json?.success, 'Subtopics query success must be true');
  assertTrue(Array.isArray(res.json.data), 'Subtopics data is an array');

  const derivSubtopic = res.json.data.find(st => st.code === 'MATH_CALC_DERIVATIVE');
  assertTrue(Boolean(derivSubtopic), 'Derivative subtopic found under calculus topic');
});

// ============================================================================
// JOB-001: RefreshTokenCleanupService (Scheduled Nightly Cron)
// ============================================================================
registerCase('JOB-001', 'RefreshTokenCleanupService - Purge expired tokens via clean-room JVM trigger', async ({ queryDb }) => {
  // 1. Seed deterministic test tokens directly in acceptance database
  const expiredToken = `acc_exp_tok_${Date.now()}`;
  const validToken = `acc_val_tok_${Date.now()}`;

  queryDb(`
    INSERT INTO refresh_tokens (user_id, token, expires_at, revoked, created_at)
    SELECT u.id, '${expiredToken}', NOW() - INTERVAL '2 days', FALSE, NOW() - INTERVAL '3 days'
    FROM users u WHERE u.email = 'student@vsat.com' LIMIT 1;

    INSERT INTO refresh_tokens (user_id, token, expires_at, revoked, created_at)
    SELECT u.id, '${validToken}', NOW() + INTERVAL '10 days', FALSE, NOW()
    FROM users u WHERE u.email = 'student@vsat.com' LIMIT 1;
  `);

  // Assert both seeded tokens exist before trigger
  const expBefore = Number(queryDb(`SELECT COUNT(*) FROM refresh_tokens WHERE token = '${expiredToken}';`));
  const valBefore = Number(queryDb(`SELECT COUNT(*) FROM refresh_tokens WHERE token = '${validToken}';`));
  assertEqual(expBefore, 1, 'Expired test token seeded before cleanup execution');
  assertEqual(valBefore, 1, 'Valid test token seeded before cleanup execution');

  // 2. Trigger execution using JVM runner (TriggerRefreshTokenCleanup.java compiled with JDK jbr)
  const triggerResult = runJvmRefreshTokenCleanup();
  assertEqual(triggerResult.exitCode, 0, `JVM Trigger exited with status 0 (stdout: ${triggerResult.stdout}, stderr: ${triggerResult.stderr})`);

  // 3. Assert post-condition in database: expired token deleted, valid token retained
  const expAfter = Number(queryDb(`SELECT COUNT(*) FROM refresh_tokens WHERE token = '${expiredToken}';`));
  const valAfter = Number(queryDb(`SELECT COUNT(*) FROM refresh_tokens WHERE token = '${validToken}';`));
  assertEqual(expAfter, 0, 'Expired token MUST be deleted by RefreshTokenCleanupService');
  assertEqual(valAfter, 1, 'Valid token MUST NOT be deleted and remains intact');

  // Clean up the remaining valid test token
  queryDb(`DELETE FROM refresh_tokens WHERE token = '${validToken}';`);
});

// ============================================================================
// JOB-002: OpenAiCompatibleLlmClient (AI Tutor External Integration)
// ============================================================================
registerCase('JOB-002', 'OpenAiCompatibleLlmClient - AI explanation integration and error fallback handling', async ({ apiCall, queryDb }) => {
  const fixtures = getFixtureIds(queryDb);

  // 1. Normal mode: verify LLM mock responses succeed with LaTeX formatting
  await setLlmMockMode('normal');
  const normalRes = await apiCall('/ai/explain', {
    method: 'POST',
    body: { questionId: fixtures.questionId }
  }, 'STUDENT');

  assertStatus(normalRes.status, 200, 'Normal LLM mock mode returns HTTP 200');
  assertTrue(Boolean(normalRes.json?.data?.explanation), 'Explanation is returned in normal mode');

  // 2. Error mode: switch mock server to simulate 500 internal server error
  // Use optionId: 2 to ensure cache miss against in-memory AiExplanationServiceImpl cache
  await setLlmMockMode('error');
  const errorRes = await apiCall('/ai/explain', {
    method: 'POST',
    body: { questionId: fixtures.questionId, selectedOptionId: 2 }
  }, 'STUDENT');

  // OpenAiCompatibleLlmClient catches RestClientResponseException and throws AppException.serviceUnavailable (HTTP 503)
  assertStatus(errorRes.status, 503, 'AI provider error triggers HTTP 503 SERVICE_UNAVAILABLE');
  assertFalse(errorRes.json?.success, 'Error response success is false');

  // 3. Restore mock mode to normal for downstream suites
  await setLlmMockMode('normal');
});

// ============================================================================
// SUITE EXECUTION ENTRYPOINT
// ============================================================================
if (process.argv[1] && process.argv[1].endsWith('backend-public-student-collab.mjs')) {
  runSuite('backend-psc-report', {
    resultsDir: process.env.RESULTS_DIR
  });
}
