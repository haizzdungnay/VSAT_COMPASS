export type SubjectCode = 'MATH' | 'PHY' | 'CHEM' | 'BIO' | 'HIS' | 'GEO' | 'ENG';

export type ExamStatus = 'DRAFT' | 'COMPOSING' | 'PENDING_REVIEW' | 'PUBLISHED' | 'HIDDEN' | 'ARCHIVED';

export type QuestionStatus = 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'NEEDS_REVISION' | 'ARCHIVED';

export type QuestionType = 'TRUE_FALSE' | 'SINGLE_CHOICE' | 'MATCHING' | 'SHORT_ANSWER';

export type CognitiveLevel = 'RECOGNITION' | 'UNDERSTANDING' | 'APPLICATION' | 'HIGH_APPLICATION';

export type DifficultyLevel = 'EASY' | 'MEDIUM' | 'HARD' | 'VERY_HARD';

export type DeliveryMode = 'PRACTICE' | 'SCHEDULED_MOCK';

export interface ExamItem {
  id: number;
  examCode: string;
  title: string;
  subjectId: number;
  subjectName?: string;
  questionCount: number;
  durationMinutes: number;
  difficulty: DifficultyLevel;
  pricingType: 'FREE' | 'PAID';
  status: ExamStatus;
  tags?: string;
  totalAttempts?: number;
  avgScore?: number;
  updatedAt?: string;
}

export interface ExamDetail extends ExamItem {
  description?: string;
  scoringProfileCode?: string;
  deliveryMode?: DeliveryMode;
  startAt?: string;
  endAt?: string;
  gracePeriodMinutes?: number;
  maxAttempts?: number;
  resultPolicy?: string;
  variantsCount?: number;
}

export interface QuestionBankItem {
  id: number;
  questionCode: string;
  subjectCode: string;
  topicName: string;
  questionType: QuestionType;
  cognitiveLevel: CognitiveLevel;
  difficulty: DifficultyLevel;
  questionText: string;
  rawPoints: number;
}

export interface QuestionPickerItem {
  id: number;
  questionCode: string;
  questionTextSnippet: string;
  subjectId: number;
  topicId?: number;
  subtopicId?: number;
  questionType: QuestionType;
  difficulty: DifficultyLevel;
  status: string;
  imageUrl?: string;
}

export interface FullQuestionItem {
  id: number;
  questionCode: string;
  subjectId: number;
  subjectName?: string;
  topicId?: number;
  topicName?: string;
  questionType: QuestionType;
  cognitiveLevel: CognitiveLevel;
  difficulty: DifficultyLevel;
  questionText: string;
  explanation?: string;
  status: QuestionStatus;
  rawPoints?: number;
  updatedAt?: string;
  trueFalseItems?: Array<{ key: string; text: string; isCorrect: boolean }>;
  options?: Array<{ label: string; text: string; isCorrect: boolean }>;
  matchingPairs?: Array<{ left: string; right: string }>;
  shortAnswerKey?: { acceptedAnswer: string; tolerance?: number; unit?: string };
}

export interface ExamVariantItem {
  id: number;
  examId: number;
  variantCode: string;
  randomSeed: string;
  contentHash: string;
  questionCount: number;
  createdAt?: string;
}

export interface BlueprintCellItem {
  id: number;
  topicId: number;
  topicName: string;
  questionType: QuestionType;
  cognitiveLevel: CognitiveLevel;
  difficulty: DifficultyLevel;
  questionCount: number;
}

export interface BlueprintItem {
  id: number;
  code: string;
  name: string;
  subjectId: number;
  subjectCode?: string;
  subjectName?: string;
  totalQuestions: number;
  maxRawScore: number;
  version: number;
  cells: BlueprintCellItem[];
}

export interface OptionItem {
  id: number;
  optionLabel: string;
  optionText: string;
  isCorrect: boolean;
}

export interface ExamQuestionDetail {
  id: number;
  examId: number;
  questionId: number;
  questionOrder: number;
  questionCode: string;
  questionText: string;
  questionType: QuestionType;
  difficulty: DifficultyLevel;
  maxRawScore: number;
  options: OptionItem[];
}
