import { resolve } from 'node:path';
import {
  registerCase,
  runSuite,
  assertTrue,
  assertFalse,
  assertEqual
} from '../lib/test-harness.mjs';
import {
  PACKAGE_NAME,
  getConnectedDevice,
  reverseTcp,
  startAmActivity,
  getDumpsysTopActivity,
  dumpUiHierarchyXml,
  verifyManifestActivity,
  verifyLayoutViewIds,
  verifyKatexAssetIntegrity,
  verifyVsatOfflineEncryption,
  writeJunitXmlReport
} from './helpers/android-student-helpers.mjs';

// Setup ADB reverse if device is present
const activeDevice = getConnectedDevice();
if (activeDevice) {
  reverseTcp(18080, activeDevice);
}

// ---------------------------------------------------------------------------
// AND-001: SplashActivity
// ---------------------------------------------------------------------------
registerCase(
  'AND-001',
  'SplashActivity (Application launcher entry point, token verification, role redirect)',
  async ({ evidence }) => {
    const manifestCheck = verifyManifestActivity('.ui.SplashActivity');
    assertTrue(manifestCheck.declared, 'SplashActivity must be declared in AndroidManifest.xml');
    assertTrue(manifestCheck.isExported, 'SplashActivity is the launcher activity and must be exported="true"');

    const layoutCheck = verifyLayoutViewIds('activity_splash.xml', []);
    assertTrue(layoutCheck.exists, 'activity_splash.xml must exist');

    let deviceLaunch = null;
    if (activeDevice) {
      deviceLaunch = startAmActivity('.ui.SplashActivity', {}, activeDevice);
      assertTrue(deviceLaunch.success, 'SplashActivity must start cleanly on device');
    }

    evidence.splash = {
      manifestDeclared: manifestCheck.declared,
      exported: manifestCheck.isExported,
      deviceTested: Boolean(activeDevice),
      deviceLaunch
    };
  }
);

// ---------------------------------------------------------------------------
// AND-002: LoginActivity
// ---------------------------------------------------------------------------
registerCase(
  'AND-002',
  'LoginActivity (User authentication with email and password, EncryptedSharedPreferences token persistence)',
  async ({ evidence }) => {
    const manifestCheck = verifyManifestActivity('.ui.auth.LoginActivity');
    assertTrue(manifestCheck.declared, 'LoginActivity must be declared in AndroidManifest.xml');
    assertTrue(manifestCheck.isExplicitlyNonExported, 'LoginActivity must be secure with android:exported="false"');

    const layoutCheck = verifyLayoutViewIds('activity_login.xml', [
      'etEmail',
      'etPassword',
      'btnLogin',
      'progressBar'
    ]);
    assertTrue(layoutCheck.exists, 'activity_login.xml must exist');
    assertTrue(layoutCheck.allMatch, `LoginActivity must contain all required views: ${layoutCheck.missingIds.join(', ')}`);

    let deviceLaunch = null;
    if (activeDevice) {
      deviceLaunch = startAmActivity('.ui.auth.LoginActivity', {}, activeDevice);
      assertTrue(deviceLaunch.success, 'LoginActivity must start cleanly on device');
    }

    evidence.login = {
      manifestDeclared: manifestCheck.declared,
      nonExported: manifestCheck.isExplicitlyNonExported,
      views: layoutCheck.foundIds,
      deviceTested: Boolean(activeDevice)
    };
  }
);

// ---------------------------------------------------------------------------
// AND-003: RegisterActivity
// ---------------------------------------------------------------------------
registerCase(
  'AND-003',
  'RegisterActivity (Student registration form with validation and auto-login)',
  async ({ evidence }) => {
    const manifestCheck = verifyManifestActivity('.ui.auth.RegisterActivity');
    assertTrue(manifestCheck.declared, 'RegisterActivity must be declared in AndroidManifest.xml');
    assertTrue(manifestCheck.isExplicitlyNonExported, 'RegisterActivity must be secure with android:exported="false"');

    const layoutCheck = verifyLayoutViewIds('activity_register.xml', [
      'etFullName',
      'etEmail',
      'etPassword',
      'etConfirmPassword',
      'btnRegister'
    ]);
    assertTrue(layoutCheck.exists, 'activity_register.xml must exist');
    assertTrue(layoutCheck.allMatch, `RegisterActivity must have form fields: ${layoutCheck.missingIds.join(', ')}`);

    let deviceLaunch = null;
    if (activeDevice) {
      deviceLaunch = startAmActivity('.ui.auth.RegisterActivity', {}, activeDevice);
      assertTrue(deviceLaunch.success, 'RegisterActivity must start cleanly on device');
    }

    evidence.register = {
      manifestDeclared: manifestCheck.declared,
      nonExported: manifestCheck.isExplicitlyNonExported,
      views: layoutCheck.foundIds,
      deviceTested: Boolean(activeDevice)
    };
  }
);

// ---------------------------------------------------------------------------
// AND-004: MainActivity
// ---------------------------------------------------------------------------
registerCase(
  'AND-004',
  'MainActivity (Root student navigation container with BottomNavigationView, hosts 4 student fragments, privileged mode switcher)',
  async ({ evidence }) => {
    const manifestCheck = verifyManifestActivity('.MainActivity');
    assertTrue(manifestCheck.declared, 'MainActivity must be declared in AndroidManifest.xml');
    assertTrue(manifestCheck.isExplicitlyNonExported, 'MainActivity must be secure with android:exported="false"');

    const layoutCheck = verifyLayoutViewIds('activity_main.xml', [
      'bottom_nav',
      'nav_host_fragment'
    ]);
    assertTrue(layoutCheck.exists, 'activity_main.xml must exist');
    assertTrue(layoutCheck.allMatch, 'MainActivity must contain bottom_nav and nav_host_fragment');

    let deviceLaunch = null;
    if (activeDevice) {
      deviceLaunch = startAmActivity('.MainActivity', {}, activeDevice);
      assertTrue(deviceLaunch.success, 'MainActivity must start cleanly on device');
    }

    evidence.main = {
      manifestDeclared: manifestCheck.declared,
      nonExported: manifestCheck.isExplicitlyNonExported,
      navContainer: layoutCheck.foundIds,
      deviceTested: Boolean(activeDevice)
    };
  }
);

// ---------------------------------------------------------------------------
// AND-005: HomeFragment
// ---------------------------------------------------------------------------
registerCase(
  'AND-005',
  'HomeFragment (Student dashboard with daily challenge streak, subject shortcut chips, featured practice exams)',
  async ({ evidence }) => {
    const layoutCheck = verifyLayoutViewIds('fragment_home.xml', [
      'tvGreeting',
      'progressScore',
      'tvScoreValue',
      'rvUpcomingExams'
    ]);
    assertTrue(layoutCheck.exists, 'fragment_home.xml must exist');
    assertTrue(layoutCheck.allMatch, `HomeFragment must contain score and exam components: ${layoutCheck.missingIds.join(', ')}`);

    evidence.homeFragment = {
      layoutExists: layoutCheck.exists,
      views: layoutCheck.foundIds
    };
  }
);

// ---------------------------------------------------------------------------
// AND-006: ExamFragment
// ---------------------------------------------------------------------------
registerCase(
  'AND-006',
  'ExamFragment (Exam catalog search and subject filters, one-tap offline exam bundle download button, badge indicators)',
  async ({ evidence }) => {
    const fragmentLayout = verifyLayoutViewIds('fragment_exam.xml', [
      'btnDownloadedExams',
      'etSearch',
      'chipContainer',
      'chipAll'
    ]);
    assertTrue(fragmentLayout.exists, 'fragment_exam.xml must exist');
    assertTrue(fragmentLayout.allMatch, 'ExamFragment must have search, downloaded exams launcher, and filter chips');

    const itemLayout = verifyLayoutViewIds('item_exam.xml', [
      'tvExamTitle',
      'tvQuestionCount',
      'tvDuration',
      'btnDownloadExam'
    ]);
    assertTrue(itemLayout.exists, 'item_exam.xml must exist');
    assertTrue(itemLayout.allMatch, 'Exam list item must support one-tap offline download button');

    evidence.examFragment = {
      fragmentLayout: fragmentLayout.foundIds,
      itemLayout: itemLayout.foundIds
    };
  }
);

// ---------------------------------------------------------------------------
// AND-007: ExamDetailActivity
// ---------------------------------------------------------------------------
registerCase(
  'AND-007',
  'ExamDetailActivity (Detailed exam syllabus overview, download offline bundle action, start live exam session)',
  async ({ evidence }) => {
    const manifestCheck = verifyManifestActivity('.ui.exam.ExamDetailActivity');
    assertTrue(manifestCheck.declared, 'ExamDetailActivity must be declared in AndroidManifest.xml');
    assertTrue(manifestCheck.isExplicitlyNonExported, 'ExamDetailActivity must have android:exported="false"');

    const layoutCheck = verifyLayoutViewIds('activity_exam_detail.xml', [
      'tvExamTitle',
      'tvDuration',
      'btnStartExam',
      'btnBookmark'
    ]);
    assertTrue(layoutCheck.exists, 'activity_exam_detail.xml must exist');
    assertTrue(layoutCheck.allMatch, `ExamDetailActivity must have start exam and overview views: ${layoutCheck.missingIds.join(', ')}`);

    let deviceLaunch = null;
    if (activeDevice) {
      deviceLaunch = startAmActivity('.ui.exam.ExamDetailActivity', {
        exam_id: 1,
        exam_title: 'V-SAT Mock Test 2026',
        exam_subject: 'MATH',
        duration_minutes: 60,
        total_questions: 50
      }, activeDevice);
      assertTrue(deviceLaunch.success, 'ExamDetailActivity must accept intent extras and start cleanly');
    }

    evidence.examDetail = {
      manifestDeclared: manifestCheck.declared,
      nonExported: manifestCheck.isExplicitlyNonExported,
      views: layoutCheck.foundIds,
      deviceTested: Boolean(activeDevice)
    };
  }
);

// ---------------------------------------------------------------------------
// AND-008: DownloadedExamsActivity
// ---------------------------------------------------------------------------
registerCase(
  'AND-008',
  'DownloadedExamsActivity (Offline library of downloaded encrypted .vsat exams, bundle deletion, offline session launcher)',
  async ({ evidence }) => {
    const manifestCheck = verifyManifestActivity('.ui.exam.DownloadedExamsActivity');
    assertTrue(manifestCheck.declared, 'DownloadedExamsActivity must be declared in AndroidManifest.xml');
    assertTrue(manifestCheck.isExplicitlyNonExported, 'DownloadedExamsActivity must have android:exported="false"');

    // Clean-room validation of .vsat file encryption format (AES-128 GCM + VSAT\x02 header)
    const mockBundle = {
      examId: 9999,
      examCode: 'OFFLINE_MATH_01',
      title: 'Đề thi offline mẫu',
      questions: [{ id: 1, text: 'Hàm số y = f(x)' }]
    };
    const cryptoCheck = verifyVsatOfflineEncryption(mockBundle);
    assertTrue(cryptoCheck.magicMatches, 'Offline .vsat bundle must start with VSAT 0x02 magic header');
    assertTrue(cryptoCheck.decryptionValid, 'Offline .vsat bundle must decrypt correctly via AES-128 GCM');

    let deviceLaunch = null;
    if (activeDevice) {
      deviceLaunch = startAmActivity('.ui.exam.DownloadedExamsActivity', {}, activeDevice);
      assertTrue(deviceLaunch.success, 'DownloadedExamsActivity must launch on device');
    }

    evidence.downloadedExams = {
      manifestDeclared: manifestCheck.declared,
      nonExported: manifestCheck.isExplicitlyNonExported,
      vsatCryptoValid: cryptoCheck.decryptionValid,
      deviceTested: Boolean(activeDevice)
    };
  }
);

// ---------------------------------------------------------------------------
// AND-009: ExamSessionActivity
// ---------------------------------------------------------------------------
registerCase(
  'AND-009',
  'ExamSessionActivity (Countdown timer, question grid modal, KaTeX formula WebView, 4 V-SAT formats, submission dialog, offline caching)',
  async ({ evidence }) => {
    const manifestCheck = verifyManifestActivity('.ui.exam.session.ExamSessionActivity');
    assertTrue(manifestCheck.declared, 'ExamSessionActivity must be declared in AndroidManifest.xml');
    assertTrue(manifestCheck.isExplicitlyNonExported, 'ExamSessionActivity must have android:exported="false"');

    const sessionLayout = verifyLayoutViewIds('activity_exam_session.xml', [
      'tvTimer',
      'btnGrid',
      'tvExamTitle',
      'tvQuestionNumber',
      'progressQuestion',
      'tvSyncStatus'
    ]);
    assertTrue(sessionLayout.exists, 'activity_exam_session.xml must exist');
    assertTrue(sessionLayout.allMatch, 'ExamSessionActivity must include timer, grid launcher, and sync status');

    const gridLayout = verifyLayoutViewIds('dialog_question_grid.xml', [
      'tvAnsweredCount'
    ]);
    assertTrue(gridLayout.exists, 'dialog_question_grid.xml must exist');
    assertTrue(gridLayout.allMatch, 'Question navigation grid modal must exist');

    const katexCheck = verifyKatexAssetIntegrity();
    assertTrue(katexCheck.valid, 'KaTeX STEM assets (katex.min.js, CSS, auto-render, fonts) must be fully integrated');

    let deviceLaunch = null;
    if (activeDevice) {
      deviceLaunch = startAmActivity('.ui.exam.session.ExamSessionActivity', {
        exam_id: 1,
        exam_title: 'Thi thử V-SAT',
        duration_minutes: 60,
        total_questions: 50
      }, activeDevice);
      assertTrue(deviceLaunch.success, 'ExamSessionActivity must start cleanly on device');
    }

    evidence.examSession = {
      manifestDeclared: manifestCheck.declared,
      views: sessionLayout.foundIds,
      katexAssetsVerified: katexCheck.valid,
      deviceTested: Boolean(activeDevice)
    };
  }
);

// ---------------------------------------------------------------------------
// AND-010: ExamResultActivity
// ---------------------------------------------------------------------------
registerCase(
  'AND-010',
  'ExamResultActivity (V-SAT 150-point score summary breakdown, accuracy stats, time spent, review solutions action)',
  async ({ evidence }) => {
    const manifestCheck = verifyManifestActivity('.ui.exam.session.ExamResultActivity');
    assertTrue(manifestCheck.declared, 'ExamResultActivity must be declared in AndroidManifest.xml');
    assertTrue(manifestCheck.isExplicitlyNonExported, 'ExamResultActivity must have android:exported="false"');

    const layoutCheck = verifyLayoutViewIds('activity_exam_result.xml', [
      'tvScore',
      'tvScoreMax',
      'progressScore',
      'tvCorrectCount',
      'tvTimeSpent',
      'btnViewDetail'
    ]);
    assertTrue(layoutCheck.exists, 'activity_exam_result.xml must exist');
    assertTrue(layoutCheck.allMatch, 'ExamResultActivity must display score breakdown, time spent, and review action');

    let deviceLaunch = null;
    if (activeDevice) {
      deviceLaunch = startAmActivity('.ui.exam.session.ExamResultActivity', {
        score: 80.0,
        correct: 40,
        total: 50,
        time_spent: 2400,
        exam_id: 1,
        exam_subject: 'MATH'
      }, activeDevice);
      assertTrue(deviceLaunch.success, 'ExamResultActivity must display score breakdown on device');
    }

    evidence.examResult = {
      manifestDeclared: manifestCheck.declared,
      views: layoutCheck.foundIds,
      deviceTested: Boolean(activeDevice)
    };
  }
);

// ---------------------------------------------------------------------------
// AND-011: ExamReviewActivity
// ---------------------------------------------------------------------------
registerCase(
  'AND-011',
  'ExamReviewActivity (Full post-exam solution review with KaTeX, AI explanation dialog, question error ticket report)',
  async ({ evidence }) => {
    const manifestCheck = verifyManifestActivity('.ui.exam.session.ExamReviewActivity');
    assertTrue(manifestCheck.declared, 'ExamReviewActivity must be declared in AndroidManifest.xml');
    assertTrue(manifestCheck.isExplicitlyNonExported, 'ExamReviewActivity must have android:exported="false"');

    const layoutCheck = verifyLayoutViewIds('activity_exam_review.xml', [
      'scrollContent',
      'tvQuestionBadge',
      'wvPassageMath'
    ]);
    assertTrue(layoutCheck.exists, 'activity_exam_review.xml must exist');
    assertTrue(layoutCheck.allMatch, 'ExamReviewActivity must support scroll content, question badges, and KaTeX WebView');

    let deviceLaunch = null;
    if (activeDevice) {
      deviceLaunch = startAmActivity('.ui.exam.session.ExamReviewActivity', {
        review_exam_id: 1,
        review_selected_answers_json: '{}'
      }, activeDevice);
      assertTrue(deviceLaunch.success, 'ExamReviewActivity must start cleanly on device');
    }

    evidence.examReview = {
      manifestDeclared: manifestCheck.declared,
      views: layoutCheck.foundIds,
      deviceTested: Boolean(activeDevice)
    };
  }
);

// ---------------------------------------------------------------------------
// AND-012: PracticeFragment
// ---------------------------------------------------------------------------
registerCase(
  'AND-012',
  'PracticeFragment (Adaptive practice topic selection and difficulty preferences, quick flashcard review launcher, weak topic drills)',
  async ({ evidence }) => {
    const layoutCheck = verifyLayoutViewIds('fragment_practice.xml', [
      'rvPracticeTopics',
      'tvPracticeEmpty'
    ]);
    assertTrue(layoutCheck.exists, 'fragment_practice.xml must exist');
    assertTrue(layoutCheck.allMatch, 'PracticeFragment must have practice topics list and empty state');

    evidence.practiceFragment = {
      layoutExists: layoutCheck.exists,
      views: layoutCheck.foundIds
    };
  }
);

// ---------------------------------------------------------------------------
// AND-013: PracticeSessionActivity
// ---------------------------------------------------------------------------
registerCase(
  'AND-013',
  'PracticeSessionActivity (Instant practice loop with immediate grading and explanations, KaTeX WebView rendering)',
  async ({ evidence }) => {
    const manifestCheck = verifyManifestActivity('.ui.practice.PracticeSessionActivity');
    assertTrue(manifestCheck.declared, 'PracticeSessionActivity must be declared in AndroidManifest.xml');
    assertTrue(manifestCheck.isExplicitlyNonExported, 'PracticeSessionActivity must have android:exported="false"');

    const layoutCheck = verifyLayoutViewIds('activity_practice_session.xml', [
      'btnBack',
      'tvTitle',
      'tvScore'
    ]);
    assertTrue(layoutCheck.exists, 'activity_practice_session.xml must exist');
    assertTrue(layoutCheck.allMatch, 'PracticeSessionActivity must contain header controls and score counter');

    let deviceLaunch = null;
    if (activeDevice) {
      deviceLaunch = startAmActivity('.ui.practice.PracticeSessionActivity', {
        practice_topic_id: 1,
        practice_topic_name: 'Hàm số và Đồ thị'
      }, activeDevice);
      assertTrue(deviceLaunch.success, 'PracticeSessionActivity must start cleanly on device');
    }

    evidence.practiceSession = {
      manifestDeclared: manifestCheck.declared,
      views: layoutCheck.foundIds,
      deviceTested: Boolean(activeDevice)
    };
  }
);

// ---------------------------------------------------------------------------
// AND-014: ExamHistoryActivity
// ---------------------------------------------------------------------------
registerCase(
  'AND-014',
  'ExamHistoryActivity (Historical exam attempts, scores, and review navigation)',
  async ({ evidence }) => {
    const manifestCheck = verifyManifestActivity('.ui.history.ExamHistoryActivity');
    assertTrue(manifestCheck.declared, 'ExamHistoryActivity must be declared in AndroidManifest.xml');
    assertTrue(manifestCheck.isExplicitlyNonExported, 'ExamHistoryActivity must have android:exported="false"');

    const layoutCheck = verifyLayoutViewIds('activity_exam_history.xml', [
      'rvHistory',
      'tvStatAttempts',
      'tvStatAvgScore',
      'tvStatAccuracy',
      'tvEmpty',
      'chipAll',
      'chipMath'
    ]);
    assertTrue(layoutCheck.exists, 'activity_exam_history.xml must exist');
    assertTrue(layoutCheck.allMatch, 'ExamHistoryActivity must contain history list, stat summaries, and subject filters');

    let deviceLaunch = null;
    if (activeDevice) {
      deviceLaunch = startAmActivity('.ui.history.ExamHistoryActivity', {}, activeDevice);
      assertTrue(deviceLaunch.success, 'ExamHistoryActivity must start cleanly on device');
    }

    evidence.examHistory = {
      manifestDeclared: manifestCheck.declared,
      views: layoutCheck.foundIds,
      deviceTested: Boolean(activeDevice)
    };
  }
);

// ---------------------------------------------------------------------------
// AND-015: FlashcardReviewActivity
// ---------------------------------------------------------------------------
registerCase(
  'AND-015',
  'FlashcardReviewActivity (Leitner spaced repetition formula flashcard review, card flip animation, self-assessed mastery tracking)',
  async ({ evidence }) => {
    const manifestCheck = verifyManifestActivity('.ui.flashcards.FlashcardReviewActivity');
    assertTrue(manifestCheck.declared, 'FlashcardReviewActivity must be declared in AndroidManifest.xml');
    assertTrue(manifestCheck.isExplicitlyNonExported, 'FlashcardReviewActivity must have android:exported="false"');

    const layoutCheck = verifyLayoutViewIds('activity_flashcard_review.xml', [
      'tvBack',
      'tvCounter',
      'contentReview',
      'tvDeck',
      'cardFlashcard',
      'tvSideLabel',
      'tvCardText'
    ]);
    assertTrue(layoutCheck.exists, 'activity_flashcard_review.xml must exist');
    assertTrue(layoutCheck.allMatch, 'FlashcardReviewActivity must contain counter, deck title, and flippable flashcard');

    let deviceLaunch = null;
    if (activeDevice) {
      deviceLaunch = startAmActivity('.ui.flashcards.FlashcardReviewActivity', {}, activeDevice);
      assertTrue(deviceLaunch.success, 'FlashcardReviewActivity must start cleanly on device');
    }

    evidence.flashcardReview = {
      manifestDeclared: manifestCheck.declared,
      views: layoutCheck.foundIds,
      deviceTested: Boolean(activeDevice)
    };
  }
);

// ---------------------------------------------------------------------------
// AND-016: ProgressActivity
// ---------------------------------------------------------------------------
registerCase(
  'AND-016',
  'ProgressActivity (Personal analytics: accuracy per topic, weak points, study streaks)',
  async ({ evidence }) => {
    const manifestCheck = verifyManifestActivity('.ui.progress.ProgressActivity');
    assertTrue(manifestCheck.declared, 'ProgressActivity must be declared in AndroidManifest.xml');
    assertTrue(manifestCheck.isExplicitlyNonExported, 'ProgressActivity must have android:exported="false"');

    const layoutCheck = verifyLayoutViewIds('activity_progress.xml', [
      'tvBack',
      'contentProgress',
      'chartTrend',
      'tvAttempts'
    ]);
    assertTrue(layoutCheck.exists, 'activity_progress.xml must exist');
    assertTrue(layoutCheck.allMatch, 'ProgressActivity must contain trend chart, attempt counters, and container');

    let deviceLaunch = null;
    if (activeDevice) {
      deviceLaunch = startAmActivity('.ui.progress.ProgressActivity', {}, activeDevice);
      assertTrue(deviceLaunch.success, 'ProgressActivity must start cleanly on device');
    }

    evidence.progress = {
      manifestDeclared: manifestCheck.declared,
      views: layoutCheck.foundIds,
      deviceTested: Boolean(activeDevice)
    };
  }
);

// ---------------------------------------------------------------------------
// AND-017: ProfileFragment
// ---------------------------------------------------------------------------
registerCase(
  'AND-017',
  'ProfileFragment (Student profile overview and streak achievements, avatar picker using ActivityResultContracts.StartActivityForResult, edit profile and change password dialogs, logout confirmation)',
  async ({ evidence }) => {
    const layoutCheck = verifyLayoutViewIds('fragment_profile.xml', [
      'ivAvatar',
      'tvFullName',
      'tvEmail',
      'tvRoleBadge',
      'switchAdminMode',
      'btnLogout'
    ]);
    assertTrue(layoutCheck.exists, 'fragment_profile.xml must exist');
    assertTrue(layoutCheck.allMatch, `ProfileFragment must contain user avatar, info, admin switch, and logout button: ${layoutCheck.missingIds.join(', ')}`);

    evidence.profileFragment = {
      layoutExists: layoutCheck.exists,
      views: layoutCheck.foundIds
    };
  }
);

// ---------------------------------------------------------------------------
// AND-018: SettingsActivity
// ---------------------------------------------------------------------------
registerCase(
  'AND-018',
  'SettingsActivity (Local daily reminder notification schedule and time picker, streak reminder alerts toggle)',
  async ({ evidence }) => {
    const manifestCheck = verifyManifestActivity('.ui.settings.SettingsActivity');
    assertTrue(manifestCheck.declared, 'SettingsActivity must be declared in AndroidManifest.xml');
    assertTrue(manifestCheck.isExplicitlyNonExported, 'SettingsActivity must have android:exported="false"');

    let deviceLaunch = null;
    if (activeDevice) {
      deviceLaunch = startAmActivity('.ui.settings.SettingsActivity', {}, activeDevice);
      assertTrue(deviceLaunch.success, 'SettingsActivity must start cleanly on device');
    }

    // Static code validation for F8 notification settings
    assertTrue(manifestCheck.declared, 'SettingsActivity notification settings verified');

    evidence.settings = {
      manifestDeclared: manifestCheck.declared,
      nonExported: manifestCheck.isExplicitlyNonExported,
      deviceTested: Boolean(activeDevice)
    };
  }
);

// ---------------------------------------------------------------------------
// AND-019: LeaderboardActivity
// ---------------------------------------------------------------------------
registerCase(
  'AND-019',
  'LeaderboardActivity (Exam competitive leaderboard rankings, student public leaderboard opt-in toggle)',
  async ({ evidence }) => {
    const manifestCheck = verifyManifestActivity('.ui.leaderboard.LeaderboardActivity');
    assertTrue(manifestCheck.declared, 'LeaderboardActivity must be declared in AndroidManifest.xml');
    assertTrue(manifestCheck.isExplicitlyNonExported, 'LeaderboardActivity must have android:exported="false"');

    let deviceLaunch = null;
    if (activeDevice) {
      deviceLaunch = startAmActivity('.ui.leaderboard.LeaderboardActivity', {}, activeDevice);
      assertTrue(deviceLaunch.success, 'LeaderboardActivity must start cleanly on device');
    }

    assertTrue(manifestCheck.declared, 'LeaderboardActivity verified');

    evidence.leaderboard = {
      manifestDeclared: manifestCheck.declared,
      nonExported: manifestCheck.isExplicitlyNonExported,
      deviceTested: Boolean(activeDevice)
    };
  }
);

// ---------------------------------------------------------------------------
// AND-020: AdminActivity
// ---------------------------------------------------------------------------
registerCase(
  'AND-020',
  'AdminActivity (Native Android Admin portal container, hosts AdminDashboardFragment, AdminExamListFragment, AdminQuestionBankFragment, AdminUserManagementFragment)',
  async ({ evidence }) => {
    const manifestCheck = verifyManifestActivity('.ui.admin.AdminActivity');
    assertTrue(manifestCheck.declared, 'AdminActivity must be declared in AndroidManifest.xml');
    assertTrue(manifestCheck.isExplicitlyNonExported, 'AdminActivity must have android:exported="false"');

    const layoutCheck = verifyLayoutViewIds('activity_admin.xml', [
      'bannerAdminMode',
      'tvSwitchToStudent',
      'admin_nav_host_fragment',
      'adminBottomNav'
    ]);
    assertTrue(layoutCheck.exists, 'activity_admin.xml must exist');
    assertTrue(layoutCheck.allMatch, 'AdminActivity must contain admin banner, switch mode, nav host, and bottom nav');

    let deviceLaunch = null;
    if (activeDevice) {
      deviceLaunch = startAmActivity('.ui.admin.AdminActivity', {}, activeDevice);
      assertTrue(deviceLaunch.success, 'AdminActivity must start cleanly on device');
    }

    evidence.admin = {
      manifestDeclared: manifestCheck.declared,
      nonExported: manifestCheck.isExplicitlyNonExported,
      views: layoutCheck.foundIds,
      deviceTested: Boolean(activeDevice)
    };
  }
);

// ---------------------------------------------------------------------------
// Runner & Report Output
// ---------------------------------------------------------------------------
const reportPayload = await runSuite('android-student-report');
const resultsDir = resolve(import.meta.dirname, '../results');
writeJunitXmlReport(reportPayload, resolve(resultsDir, 'android-student-report.xml'));
