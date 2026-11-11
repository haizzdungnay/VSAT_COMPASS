export type StudentExamMode = 'MOCK_EXAM' | 'PRACTICE' | 'SCHEDULED_MOCK';

export type StudentQuestionType = 'SINGLE_CHOICE' | 'TRUE_FALSE' | 'SHORT_ANSWER' | 'MATCHING';

export interface PublicExamSummary {
  id: number;
  examCode: string;
  title: string;
  subjectId: number;
  subjectName: string;
  questionCount: number;
  durationMinutes: number;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD' | 'VERY_HARD';
  totalAttempts?: number;
  avgScore?: number;
  description?: string;
  pricingType?: 'FREE' | 'PAID';
}

export interface PublicExamWindow {
  id: number;
  windowCode: string;
  name: string;
  examId: number;
  examCode: string;
  examTitle: string;
  subjectName?: string;
  mode: StudentExamMode;
  startAt?: string;
  endAt?: string;
  status: 'UPCOMING' | 'RUNNING' | 'CLOSED';
  activeCandidates?: number;
}

export interface StartSessionPayload {
  examId: number;
  mode: StudentExamMode;
  totalQuestions?: number;
  examWindowId?: number;
}

export interface StudentSessionInfo {
  id: number;
  examId: number;
  mode: StudentExamMode;
  status: 'IN_PROGRESS' | 'SUBMITTED' | 'EXPIRED';
  score?: number;
  scorePercentage?: number;
  rawScore?: number;
  maxRawScore?: number;
  correctCount?: number;
  totalQuestions: number;
  timeSpentSeconds?: number;
  startedAt?: string;
  submittedAt?: string;
  orderedQuestionIds: number[];
}

export interface QuestionOption {
  id: number;
  optionLabel: string;
  optionText: string;
}

export interface QuestionGroupContent {
  id: number;
  code?: string;
  title?: string;
  passageText?: string;
  passageHtml?: string;
  audioUrl?: string | null;
  audioDurationSec?: number | null;
  maxPlays?: number | null;
}

export interface StudentTrueFalseItem {
  key: string;
  text: string;
}

export interface StudentMatchingChoice {
  key: string;
  text: string;
}

export interface StudentQuestionContent {
  id: number;
  questionCode: string;
  questionGroupId?: number;
  questionGroup?: QuestionGroupContent;
  content: string;
  questionType: StudentQuestionType;
  difficulty?: string;
  order?: number;
  options?: QuestionOption[];
  trueFalseItems?: StudentTrueFalseItem[];
  matchingChoices?: StudentMatchingChoice[];
  partNumber?: 1 | 2 | 3 | 4;
  audioUrl?: string | null;
  audioDurationSec?: number | null;
  maxPlays?: number | null;
}

export interface SaveSessionAnswerPayload {
  questionId: number;
  selectedOptionId?: number;
  answerPayload?: Record<string, unknown>;
  bookmarked?: boolean;
  timeSpentSeconds?: number;
}

export type SubmitAnswerPayload = SaveSessionAnswerPayload;

export interface QuestionAnswerKeyItem {
  questionId: number;
  correctOptionIds?: number[];
  correctTrueFalse?: Record<string, boolean>;
  correctShortAnswer?: string;
  correctMatching?: Record<string, string>;
  explanation?: string;
}

export interface SessionAnswerKeysData {
  sessionId: number;
  examId: number;
  questions: QuestionAnswerKeyItem[];
}

export interface AiExplanationPayload {
  questionId: number;
  selectedOptionId?: number;
}

export interface AiExplanationResponse {
  questionId: number;
  explanation: string;
  cached?: boolean;
}

export interface StudentTicketPayload {
  questionId?: number;
  message: string;
}

export interface StudentTicketResult {
  id: number;
  questionId?: number | null;
  message: string;
  status: string;
  createdAt: string;
}

export interface QuestionStudentState {
  questionId: number;
  selectedOptionId?: number;
  answerPayload?: Record<string, unknown>;
  bookmarked: boolean;
  isSaved: boolean;
  timeSpentSeconds: number;
}

// -------------------------------------------------------------
// P2: Admission Prediction Types (Issue #15)
// -------------------------------------------------------------
export type AdmissionTier = 'SAFE' | 'FIT' | 'RISK';

export interface AdmissionPredictionRequest {
  totalScore: number;
  subjectCombo?: string; // 'ALL' | 'A00' | 'A01' | 'D01' | 'B00' | 'C00'
  universityCode?: string;
  region?: string;
}

export interface AdmissionPredictionItem {
  id?: number;
  universityCode: string;
  universityName: string;
  majorCode: string;
  majorName: string;
  subjectCombo: string;
  year: number;
  cutoffScore: number;
  delta: number;
  tier: AdmissionTier;
  notes?: string | null;
}

export interface AdmissionPredictionResponse {
  totalScore: number;
  subjectCombo?: string;
  items: AdmissionPredictionItem[];
  safeCount: number;
  fitCount: number;
  riskCount: number;
}

// -------------------------------------------------------------
// P4: Proctoring & Violation Types (Issue #17)
// -------------------------------------------------------------
export interface SessionViolationPayload {
  violationType: 'FOCUS_LOST' | 'APP_BACKGROUNDED' | 'FULLSCREEN_EXITED' | 'TAB_SWITCHED' | string;
  details?: string;
}

export interface SessionViolationResponse {
  sessionId: number;
  violationCount: number;
  autoSubmitted: boolean;
  message: string;
}

export interface SessionAudioPlayResponse {
  audioKey: string;
  playCount: number;
  maxPlays: number;
  allowed: boolean;
}

