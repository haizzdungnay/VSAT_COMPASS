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
}

export interface StudentTrueFalseItem {
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
  partNumber?: 1 | 2 | 3;
}

export interface SaveSessionAnswerPayload {
  questionId: number;
  selectedOptionId?: number;
  answerPayload?: Record<string, unknown>;
  bookmarked?: boolean;
  timeSpentSeconds?: number;
}

export interface QuestionAnswerKeyItem {
  questionId: number;
  correctOptionIds?: number[];
  correctTrueFalse?: Record<string, boolean>;
  correctShortAnswer?: string;
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
