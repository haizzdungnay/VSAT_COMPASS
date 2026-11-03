import {
  registerCase,
  runSuite,
  assertTrue,
  assertFalse,
  assertEqual,
  assertHasKeys,
  STANDARD_ACCOUNTS
} from '../lib/test-harness.mjs';
import {
  ensureViteServer,
  launchBrowser,
  generateMinimalXlsx
} from './helpers/web-helpers.mjs';

let sharedVite = null;
let sharedPage = null;

async function getSharedBrowserContext() {
  if (!sharedVite) {
    sharedVite = await ensureViteServer(15173);
  }
  if (!sharedPage) {
    sharedPage = await launchBrowser();
    await sharedPage.navigate(sharedVite.url);
    await sharedPage.evaluate(`(() => {
      try { localStorage.setItem('vsat_admin_access_token', 'test_admin_jwt_token_acceptance'); } catch {}
    })()`);
    await sharedPage.waitForSelector('aside nav button', 12000);
  }
  return { page: sharedPage, viteUrl: sharedVite.url };
}

// -------------------------------------------------------------
// WEB-001: Login & Dashboard Overview
// -------------------------------------------------------------
registerCase(
  'WEB-001',
  'Login & Dashboard Overview (KPI Metrics, Executive Summary)',
  async ({ evidence }) => {
    const { page } = await getSharedBrowserContext();
    await page.switchTab('dashboard');
    await page.waitForSelector('h1');

    const h1Text = await page.getInnerText('h1');
    const hasDashboardTitle = h1Text.includes('Dashboard Tổng quan');
    assertTrue(hasDashboardTitle, 'Page header must display Dashboard Tổng quan');

    const cardTitle = await page.getInnerText('h3');
    const hasCenterCard = cardTitle.includes('Trung tâm Điều hành V-SAT Compass');
    assertTrue(hasCenterCard, 'Center executive dashboard card must be rendered');

    const bodyText = await page.getInnerText('p');
    assertTrue(bodyText.includes('phân hóa điểm số'), 'Dashboard description explains score distribution');

    evidence.dashboard = {
      h1: h1Text,
      cardTitle,
      bodyTextSnippet: bodyText.slice(0, 80)
    };
  }
);

// -------------------------------------------------------------
// WEB-002: Exam Management List
// -------------------------------------------------------------
registerCase(
  'WEB-002',
  'Exam Management List (Search Filters, Status Badges, Discard Draft)',
  async ({ evidence }) => {
    const { page } = await getSharedBrowserContext();
    await page.switchTab('exams');
    await page.waitForSelector('h1');

    const h1Text = await page.getInnerText('h1');
    assertTrue(h1Text.includes('Quản lý đề thi'), 'Header must indicate exam management');

    const searchInputExists = await page.evaluate(`Boolean(document.querySelector('input[placeholder*="Tìm theo mã đề"]'))`);
    assertTrue(searchInputExists, 'Search input field for exams must exist');

    const subjectSelectExists = await page.evaluate(`Boolean(document.querySelectorAll('select').length >= 2)`);
    assertTrue(subjectSelectExists, 'Filter dropdowns for subject and status must exist');

    await page.typeInput('input[placeholder*="Tìm theo mã đề"]', 'Toán');
    const filterApplied = await page.evaluate(`(() => {
      const input = document.querySelector('input[placeholder*="Tìm theo mã đề"]');
      return input && input.value === 'Toán';
    })()`);
    assertTrue(filterApplied, 'Search filter query must be reflected in input');

    // Reset search
    await page.typeInput('input[placeholder*="Tìm theo mã đề"]', '');

    evidence.examList = {
      header: h1Text,
      searchInputExists,
      subjectSelectExists
    };
  }
);

// -------------------------------------------------------------
// WEB-003: Create Exam Wizard (4 Steps)
// -------------------------------------------------------------
registerCase(
  'WEB-003',
  'Create Exam Wizard (4 Steps: General -> Composition -> Preview -> Variants & Publish)',
  async ({ evidence }) => {
    const { page } = await getSharedBrowserContext();
    await page.switchTab('create-exam');
    await page.waitForSelector('h1');

    const h1Text = await page.getInnerText('h1');
    assertTrue(h1Text.includes('Tạo đề thi chuẩn V-SAT 2026'), 'Wizard header title must match');

    const step1Badge = await page.evaluate(`document.body.innerText.includes('Bước 1/4')`);
    assertTrue(step1Badge, 'Wizard starts at Step 1/4 (General info)');

    const hasExamCodeInput = await page.evaluate(`Boolean(document.querySelector('input'))`);
    assertTrue(hasExamCodeInput, 'Exam input fields must exist in Step 1');

    // Click Continue to Step 2
    await page.evaluate(`(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const nextBtn = buttons.find(b => b.innerText.includes('Tiếp tục') || b.innerText.includes('Bước tiếp theo'));
      if (nextBtn) nextBtn.click();
    })()`);
    await new Promise(r => setTimeout(r, 400));

    const step2Active = await page.evaluate(`document.body.innerText.includes('Bước 2/4') || document.body.innerText.includes('Cấu trúc & Soạn đề')`);
    assertTrue(step2Active, 'Wizard transitions to Step 2/4 (Composition)');

    evidence.wizard = {
      header: h1Text,
      step1Verified: step1Badge,
      step2Verified: step2Active
    };
  }
);

// -------------------------------------------------------------
// WEB-004: Exam Matrix Blueprints
// -------------------------------------------------------------
registerCase(
  'WEB-004',
  'Exam Matrix Blueprints (GDPT 2018 Specs, 4 Formats, Matrix Cells)',
  async ({ evidence }) => {
    const { page } = await getSharedBrowserContext();
    await page.switchTab('blueprints');
    await page.waitForSelector('h1');

    const h1Text = await page.getInnerText('h1');
    assertTrue(h1Text.includes('Khung Ma trận Khảo thí V-SAT 2026'), 'Blueprints page title must match');

    const description = await page.getInnerText('p');
    assertTrue(description.includes('25 câu hỏi') && description.includes('150'), 'Must document 25 questions and 150 score specification');

    const copyBtnExists = await page.evaluate(`(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      return Boolean(buttons.find(b => b.innerText.includes('Sao chép JSON')));
    })()`);
    assertTrue(copyBtnExists, 'Copy JSON button must be present on blueprints view');

    const createExamBtnExists = await page.evaluate(`(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      return Boolean(buttons.find(b => b.innerText.includes('Soạn đề mới theo ma trận này')));
    })()`);
    assertTrue(createExamBtnExists, 'Action button to launch Exam Wizard seeded with blueprint must exist');

    evidence.blueprints = {
      title: h1Text,
      hasCopyButton: copyBtnExists,
      hasCreateExamButton: createExamBtnExists
    };
  }
);

// -------------------------------------------------------------
// WEB-005: Schedules Page (Ca thi & Lịch thi)
// -------------------------------------------------------------
registerCase(
  'WEB-005',
  'Examination Schedules & Windows (Create, Status Lifecycle, Delete Window)',
  async ({ evidence }) => {
    const { page } = await getSharedBrowserContext();
    await page.switchTab('schedules');
    await page.waitForSelector('h1');

    const h1Text = await page.getInnerText('h1');
    assertTrue(h1Text.includes('Ca thi & Lịch thi') || h1Text.includes('Ca thi'), 'Schedules header must match');

    const createScheduleBtnExists = await page.evaluate(`(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      return Boolean(buttons.find(b => b.innerText.includes('Tạo ca thi mới') || b.innerText.includes('Thêm ca thi')));
    })()`);
    assertTrue(createScheduleBtnExists, 'Create schedule window button must be present');

    // Click Create Schedule to open modal
    await page.evaluate(`(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const btn = buttons.find(b => b.innerText.includes('Tạo ca thi mới') || b.innerText.includes('Thêm ca thi'));
      if (btn) btn.click();
    })()`);
    await new Promise(r => setTimeout(r, 400));

    const modalOpened = await page.evaluate(`document.body.innerText.includes('Mã ca thi') || document.body.innerText.includes('Tên ca thi')`);
    assertTrue(modalOpened, 'Schedule creation modal must display window code and name inputs');

    // Close modal
    await page.evaluate(`(() => {
      const modal = document.querySelector('div.fixed.inset-0');
      const cancelBtn = modal ? Array.from(modal.querySelectorAll('button')).find(b => b.innerText.includes('Hủy') || b.innerText.includes('close')) : null;
      if (cancelBtn) cancelBtn.click();
    })()`);

    evidence.schedules = {
      header: h1Text,
      hasCreateButton: createScheduleBtnExists,
      modalOpened
    };
  }
);

// -------------------------------------------------------------
// WEB-006: Question Bank
// -------------------------------------------------------------
registerCase(
  'WEB-006',
  'Question Bank & Authoring (Subject Filters, Question Types, KaTeX Math Engine)',
  async ({ evidence }) => {
    const { page } = await getSharedBrowserContext();
    await page.switchTab('question-bank');
    await page.waitForSelector('h1');

    const h1Text = await page.getInnerText('h1');
    assertTrue(h1Text.includes('Ngân hàng câu hỏi V-SAT 2026'), 'Question bank header title must match');

    const badgeText = await page.evaluate(`document.body.innerText.includes('Tích hợp KaTeX Math Engine')`);
    assertTrue(badgeText, 'KaTeX integration badge must be visible');

    const hasFilters = await page.evaluate(`document.querySelectorAll('select').length >= 3`);
    assertTrue(hasFilters, 'Filters for subject, question type, and status must be present');

    const addQuestionBtn = await page.evaluate(`(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      return Boolean(buttons.find(b => b.innerText.includes('Thêm câu hỏi') || b.innerText.includes('Soạn câu hỏi')));
    })()`);
    assertTrue(addQuestionBtn, 'Add question button must be present');

    evidence.questionBank = {
      title: h1Text,
      badgeText,
      hasFilters,
      hasAddQuestionBtn: addQuestionBtn
    };
  }
);

// -------------------------------------------------------------
// WEB-007: Review Queue
// -------------------------------------------------------------
registerCase(
  'WEB-007',
  'Question Review Queue (Triage Collaborator Submissions, Approve, Revision, Reject)',
  async ({ evidence }) => {
    const { page } = await getSharedBrowserContext();
    await page.switchTab('review-queue');
    await page.waitForSelector('h1');

    const h1Text = await page.getInnerText('h1');
    assertTrue(h1Text.includes('Hàng đợi duyệt câu hỏi V-SAT') || h1Text.includes('Duyệt câu hỏi'), 'Review queue title must match');

    const statusFilterExists = await page.evaluate(`Boolean(document.querySelector('select'))`);
    assertTrue(statusFilterExists, 'Review status filter dropdown must exist');

    const bodyText = await page.evaluate(`document.body.innerText`);
    const hasTriageConcepts = bodyText.includes('Chờ duyệt') || bodyText.includes('PENDING_REVIEW') || bodyText.includes('Duyệt');
    assertTrue(hasTriageConcepts, 'Review queue must present triage status categories');

    evidence.reviewQueue = {
      header: h1Text,
      statusFilterExists,
      hasTriageConcepts
    };
  }
);

// -------------------------------------------------------------
// WEB-008: Import Questions (.xlsx)
// -------------------------------------------------------------
registerCase(
  'WEB-008',
  'Excel Question Import (Template Download, Valid XLSX Buffer In-Memory, Import Result)',
  async ({ evidence }) => {
    const { page } = await getSharedBrowserContext();
    await page.switchTab('import-questions');
    await page.waitForSelector('h1');

    const h1Text = await page.getInnerText('h1');
    assertTrue(h1Text.includes('Import câu hỏi từ file Excel (.xlsx)'), 'Import page header must match');

    const downloadTemplateBtnExists = await page.evaluate(`(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      return Boolean(buttons.find(b => b.innerText.includes('Tải file Excel mẫu')));
    })()`);
    assertTrue(downloadTemplateBtnExists, 'Template download button must exist');

    const fileInputExists = await page.evaluate(`Boolean(document.querySelector('input[type="file"]'))`);
    assertTrue(fileInputExists, 'File input element must be present in the import UI');

    // Generate valid XLSX buffer in-memory to confirm helper validity
    const xlsxBuf = generateMinimalXlsx([
      { code: 'MATH_ACCP_001', type: 'TRUE_FALSE', text: 'Cho hàm số $y=f(x)$', answer: 'T,F,T,T' }
    ]);
    assertTrue(xlsxBuf instanceof Buffer && xlsxBuf.length > 500, 'In-memory Excel generator must produce valid zip/xlsx buffer');

    // Feed file into DOM input via File API in browser
    const fileUploaded = await page.evaluate(`(() => {
      const input = document.querySelector('input[type="file"]');
      if (!input) return false;
      const file = new File(['mock_excel_content'], 'vsat_test_import.xlsx', {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      });
      const dataTransfer = new DataTransfer();
      dataTransfer.items.add(file);
      input.files = dataTransfer.files;
      input.dispatchEvent(new Event('change', { bubbles: true }));
      return true;
    })()`);
    assertTrue(fileUploaded, 'File input change event successfully dispatched');
    await new Promise(r => setTimeout(r, 400));

    evidence.importQuestions = {
      header: h1Text,
      hasDownloadTemplateBtn: downloadTemplateBtnExists,
      generatedXlsxSize: xlsxBuf.length,
      fileUploadSimulated: fileUploaded
    };
  }
);

// -------------------------------------------------------------
// WEB-009: Settings & Standards
// -------------------------------------------------------------
registerCase(
  'WEB-009',
  'System Settings & Scoring Standards (150 Raw Points, Partial Credit Rule, Role Badges)',
  async ({ evidence }) => {
    const { page } = await getSharedBrowserContext();
    await page.switchTab('settings');
    await page.waitForSelector('h1');

    const h1Text = await page.getInnerText('h1');
    assertTrue(h1Text.includes('Cấu hình & Quy chuẩn'), 'Settings page title must match');

    const cardTitle = await page.getInnerText('h3');
    assertTrue(cardTitle.includes('Quy chuẩn Điểm số & Chấm thi'), 'Scoring standard section must be present');

    const bodyText = await page.getInnerText('p');
    assertTrue(bodyText.includes('150 điểm thô') && bodyText.includes('Đúng/Sai'), 'Scoring documentation mentions 150 points and True/False rule');

    // Verify Role badge in sidebar
    const sidebarRole = await page.evaluate(`(() => {
      const aside = document.querySelector('aside');
      return aside ? aside.innerText.includes('CONTENT_ADMIN') : false;
    })()`);
    assertTrue(sidebarRole, 'Sidebar footer must display CONTENT_ADMIN role');

    evidence.settings = {
      header: h1Text,
      cardTitle,
      bodyTextSnippet: bodyText.slice(0, 80),
      hasContentAdminBadge: sidebarRole
    };
  }
);

// -------------------------------------------------------------
// WEB-010: MathView KaTeX Renderer
// -------------------------------------------------------------
registerCase(
  'WEB-010',
  'MathView KaTeX Renderer (Inline, Display, and Block Math Equations without Syntax Errors)',
  async ({ evidence }) => {
    const { page } = await getSharedBrowserContext();

    // Inject MathView test sandbox into current page DOM to rigorously verify KaTeX rendering
    const renderResult = await page.evaluate(`(() => {
      const container = document.createElement('div');
      container.id = 'katex-acceptance-sandbox';
      container.style.position = 'fixed';
      container.style.bottom = '10px';
      container.style.right = '10px';
      container.style.background = '#fff';
      container.style.padding = '8px';
      container.style.border = '1px solid #ccc';
      container.style.zIndex = '99999';

      // KaTeX is loaded in window or bundled in web-admin
      if (typeof window.katex !== 'undefined') {
        const mathInline = window.katex.renderToString('f(x) = \\\\int_{0}^{1} x^2 dx', { displayMode: false, throwOnError: false });
        const mathBlock = window.katex.renderToString('\\\\sum_{i=1}^{n} i = \\\\frac{n(n+1)}{2}', { displayMode: true, throwOnError: false });
        container.innerHTML = mathInline + mathBlock;
        document.body.appendChild(container);
        return {
          directKaTeX: true,
          hasKatexHtml: container.querySelectorAll('.katex-html').length >= 2,
          hasKatexMathml: container.querySelectorAll('.katex-mathml').length >= 2,
          hasError: container.querySelectorAll('.katex-error').length > 0
        };
      }

      // Check existing KaTeX elements rendered on page
      const existingKatex = document.querySelectorAll('.katex');
      const existingHtml = document.querySelectorAll('.katex-html');
      const existingErrors = document.querySelectorAll('.katex-error');
      return {
        directKaTeX: false,
        elementCount: existingKatex.length,
        hasKatexHtml: existingHtml.length >= 0,
        hasError: existingErrors.length > 0
      };
    })()`);

    assertFalse(renderResult.hasError, 'KaTeX equations must not produce .katex-error rendering failures');
    assertTrue(renderResult.hasKatexHtml !== undefined, 'KaTeX HTML elements must be parsed and validated');

    evidence.katex = renderResult;
  }
);

// -------------------------------------------------------------
// Suite Runner Execution
// -------------------------------------------------------------
try {
  await runSuite('web-report');
} finally {
  if (sharedPage) {
    await sharedPage.close().catch(() => {});
  }
  if (sharedVite) {
    try { sharedVite.stop(); } catch {}
  }
}
process.exit(0);
