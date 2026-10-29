import type { FullQuestionItem, QuestionStatus } from '../../types/exam';

export function validateReviewAction(
  action: 'APPROVE' | 'REQUEST_REVISION' | 'REJECT',
  status: QuestionStatus,
  comment?: string
): { allowed: boolean; error?: string; normalizedComment?: string } {
  if (status !== 'PENDING_REVIEW') {
    return { allowed: false, error: `Không thể thao tác trên câu hỏi trạng thái ${status}` };
  }

  const trimmed = comment ? comment.trim() : '';

  if (trimmed.length > 2000) {
    return { allowed: false, error: 'Nhận xét không được vượt quá 2000 ký tự' };
  }

  if (action === 'REQUEST_REVISION' || action === 'REJECT') {
    if (!trimmed) {
      return { allowed: false, error: 'Vui lòng nhập nhận xét/lý do bắt buộc' };
    }
  }

  return { allowed: true, normalizedComment: trimmed || undefined };
}

export function filterQuestions(
  questions: FullQuestionItem[],
  filters: {
    status?: string;
    subject?: string;
    type?: string;
    search?: string;
  }
): FullQuestionItem[] {
  const { status = 'ALL', subject = 'ALL', type = 'ALL', search = '' } = filters;
  const sLow = search.toLowerCase();

  return questions.filter(q => {
    const matchesStatus = status === 'ALL' || q.status === status;
    const matchesSubject = subject === 'ALL' ||
      (subject === 'MATH' && q.subjectId === 1) ||
      (subject === 'ENG' && q.subjectId === 2) ||
      (subject === 'PHY' && q.subjectId === 3);
    const matchesType = type === 'ALL' || q.questionType === type;
    const matchesSearch = !search ||
      q.questionCode.toLowerCase().includes(sLow) ||
      q.questionText.toLowerCase().includes(sLow);

    return matchesStatus && matchesSubject && matchesType && matchesSearch;
  });
}

export function runReviewQueueSelfCheck(): boolean {
  // Test 1: Action validation for PENDING_REVIEW
  const appOk = validateReviewAction('APPROVE', 'PENDING_REVIEW', '  Ok  ');
  if (!appOk.allowed || appOk.normalizedComment !== 'Ok') {
    throw new Error('APPROVE validation failed');
  }

  const appEmptyComment = validateReviewAction('APPROVE', 'PENDING_REVIEW');
  if (!appEmptyComment.allowed) {
    throw new Error('APPROVE with empty comment should be allowed');
  }

  // Test 2: REQUEST_REVISION and REJECT require comments
  const revNoComment = validateReviewAction('REQUEST_REVISION', 'PENDING_REVIEW', '   ');
  if (revNoComment.allowed) {
    throw new Error('REQUEST_REVISION without comment should be rejected');
  }

  const rejNoComment = validateReviewAction('REJECT', 'PENDING_REVIEW', '');
  if (rejNoComment.allowed) {
    throw new Error('REJECT without comment should be rejected');
  }

  // Test 3: Comment length guard (> 2000)
  const longComment = 'a'.repeat(2001);
  const longErr = validateReviewAction('REJECT', 'PENDING_REVIEW', longComment);
  if (longErr.allowed) {
    throw new Error('Comment exceeding 2000 chars should be rejected');
  }

  // Test 4: Cannot review non-PENDING_REVIEW
  const nonPending = validateReviewAction('APPROVE', 'APPROVED', 'Great');
  if (nonPending.allowed) {
    throw new Error('Action on non-PENDING_REVIEW should be forbidden');
  }

  // Test 5: Filtering
  const sample: FullQuestionItem[] = [
    {
      id: 1,
      questionCode: 'Q-MATH-01',
      subjectId: 1,
      questionType: 'SINGLE_CHOICE',
      cognitiveLevel: 'UNDERSTANDING',
      difficulty: 'MEDIUM',
      questionText: 'Tính tích phân f(x)',
      status: 'PENDING_REVIEW',
      rawPoints: 6
    },
    {
      id: 2,
      questionCode: 'Q-ENG-02',
      subjectId: 2,
      questionType: 'TRUE_FALSE',
      cognitiveLevel: 'UNDERSTANDING',
      difficulty: 'EASY',
      questionText: 'Grammar check',
      status: 'APPROVED',
      rawPoints: 6
    }
  ];

  const pendings = filterQuestions(sample, { status: 'PENDING_REVIEW' });
  if (pendings.length !== 1 || pendings[0].id !== 1) {
    throw new Error('Filter by status failed');
  }

  const searchMath = filterQuestions(sample, { search: 'tích phân' });
  if (searchMath.length !== 1 || searchMath[0].id !== 1) {
    throw new Error('Filter by search keyword failed');
  }

  return true;
}

// ponytail: assert-based self-check for review queue invariants. Add Vitest when suite > 20 tests.
runReviewQueueSelfCheck();
