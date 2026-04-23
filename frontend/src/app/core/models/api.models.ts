// ================================================================
// DTOs API — correspondent exactement aux records Java du backend
// ================================================================

export interface ActivityDto {
  id: number;
  name: string;
  description: string;
  imageUrl: string | null;
  ageMin: number;
  ageMax: number;
  capacity: number;
  status: string | null;
  type: string | null;
}

export interface ActivityRequestDto {
  name: string;
  description: string;
  imageUrl: string | null;
  ageMin: number;
  ageMax: number;
  capacity: number;
  status: string;
  type: string;
}

export interface AnimateurSummaryDto {
  id: number;
  nom: string;
  prenom: string;
}

export interface AnimationCapacityDto {
  animationId: number | null;
  capacity: number;
  approved: number;
  pending: number;
  waitlist: number;
  remaining: number;
  fillRate: number;
  full: boolean;
  waitlistPosition: number;
}

export interface AnimationDto {
  id: number;
  startTime: string;
  endTime: string;
  role: string | null;
  status: string | null;
  activity: ActivityDto;
  animateur: AnimateurSummaryDto;
}

export interface AnimationRequestDto {
  activityId: number;
  animateurId: number;
  role: string;
  status: string;
  startTime: string;
  endTime: string;
}

export interface AdminOptionsDto {
  activityStatuses: string[];
  activityTypes: string[];
  animationRoles: string[];
  animationStatuses: string[];
}

export interface AdminAnimationRowDto {
  animation: AnimationDto;
  capacity: AnimationCapacityDto;
}

export interface UserDto {
  id: number;
  email: string;
  role: string;
  enabled: boolean;
  avatarUrl: string | null;
}

export interface EnfantDto {
  id: number;
  nom: string;
  prenom: string;
  dateDeNaissance: string;
  active: boolean;
  parentId: number | null;
  parent?: { nom: string; prenom: string } | null;
}

export interface EnfantSummaryDto {
  id: number;
  nom: string;
  prenom: string;
}

export interface ParentDto {
  id: number;
  nom: string;
  prenom: string;
  adresse: string | null;
  user: UserDto | null;
  enfants: EnfantDto[];
}

export interface AnimateurDto {
  id: number;
  nom: string;
  prenom: string;
  user: UserDto | null;
}

export interface AnimateurRequestDto {
  nom: string;
  prenom: string;
  email?: string;
  password?: string;
}

export interface InscriptionDto {
  id: number;
  statusInscription: string;
  presenceStatus: string;
  incidentNote: string | null;
  enfant: EnfantDto;
  animation: AnimationDto;
}

export interface AdminInscriptionReviewDto {
  inscription: InscriptionDto;
  capacity: AnimationCapacityDto;
}

export interface AdminDemandesDto {
  pendingParents: ParentDto[];
  pendingEnfants: EnfantDto[];
  pendingInscriptions: AdminInscriptionReviewDto[];
  processedInscriptions: AdminInscriptionReviewDto[];
}

export interface AdminNotificationDto {
  id: number;
  source: string;
  type: string;
  message: string;
  createdAt: string;
}

export interface ParentNotificationDto {
  id: number;
  category: string;
  title: string;
  message: string;
  createdAt: string;
  readStatus: boolean;
  archivedStatus?: boolean;
}

export interface AnimateurNotificationDto {
  id: number;
  category: string;
  title: string;
  message: string;
  createdAt: string;
  readStatus: boolean;
  archivedStatus?: boolean;
}

export interface ChatParticipantDto {
  userId: number;
  profileId: number | null;
  accountType: string;
  displayName: string;
  email: string;
}

export interface AppointmentSlotDto {
  id: number;
  animateurUserId: number | null;
  animateurName: string | null;
  parentUserId: number | null;
  parentName: string | null;
  startTime: string;
  endTime: string;
  status: string;
  bookedAt: string | null;
}

export interface AppointmentSlotCreateDto {
  startTime: string;
  endTime: string;
  status: string;
}

export interface BookingDto {
  id: number;
  slotId: number | null;
  animateurUserId: number | null;
  animateurName: string | null;
  parentUserId: number | null;
  parentName: string | null;
  childName: string | null;
  date: string;
  startTime: string;
  endTime: string;
  status: string;
  createdAt: string;
  updatedAt: string | null;
  cancelledAt: string | null;
}

export interface ChatConversationSummaryDto {
  id: number;
  participant: ChatParticipantDto;
  lastMessagePreview: string | null;
  lastMessageAt: string | null;
  unreadCount: number;
}

export interface ChatMessageDto {
  id: number;
  conversationId: number;
  sender: ChatParticipantDto;
  recipient: ChatParticipantDto;
  body: string;
  createdAt: string;
  mine: boolean;
  readStatus: boolean;
}

export interface ChatConversationDetailDto {
  id: number;
  participant: ChatParticipantDto;
  unreadCount: number;
  messages: ChatMessageDto[];
}

export interface SendChatMessageRequestDto {
  conversationId: number | null;
  recipientUserId: number | null;
  body: string;
}

export interface SidebarCountsDto {
  notifications: number;
  messages: number;
}

export interface AnimationWithCapacityDto {
  animation: AnimationDto;
  capacity: AnimationCapacityDto;
  enrolledChildren: EnfantSummaryDto[];
}

export interface ParentActivityViewDto {
  activity: ActivityDto;
  animations: AnimationWithCapacityDto[];
}

export interface ParentActivitiesResponseDto {
  enfants: EnfantSummaryDto[];
  inscriptions: InscriptionDto[];
  activities: ParentActivityViewDto[];
}

export interface ActionResponseDto {
  success: boolean;
  message: string;
  id: number | null;
}

export interface QuizCreateRequestDto {
  title: string;
  sourceNotes: string;
  animationId: number | null;
}

export interface SportPracticePlanCreateRequestDto {
  title: string;
  sourceNotes: string;
  animationId: number | null;
}

export interface SportPracticePlanItemDto {
  id: number;
  title: string;
  instructions: string;
  purpose: string;
  durationLabel: string | null;
  safetyTip: string | null;
  position: number;
}

export interface SportPracticePlanDto {
  id: number;
  animationId: number | null;
  activityName: string | null;
  title: string;
  summary: string;
  sourceNotes: string | null;
  createdAt: string;
  items: SportPracticePlanItemDto[];
}

export interface QuizQuestionDto {
  id: number;
  angle: string;
  type: string;
  questionText: string;
  expectedAnswer: string;
  position: number;
  options: string[];
}

export interface QuizAxisDto {
  id: number;
  title: string;
  summary: string;
  position: number;
  questions: QuizQuestionDto[];
}

export interface QuizDto {
  id: number;
  title: string;
  sourceNotes: string;
  createdAt: string;
  animationId: number | null;
  activityName: string | null;
  axes: QuizAxisDto[];
}

export interface TutorAxisProgressDto {
  axisTitle: string;
  quizCount: number;
  questionCount: number;
  scorePercent: number | null;
  averageResponseTimeSeconds: number | null;
  status: string;
  latestQuizTitle: string | null;
  latestQuizCreatedAt: string | null;
}

export interface TutorDashboardDto {
  enrolledChildrenCount: number;
  quizResponderCount: number;
  quizAttemptCount: number;
  quizParticipationPercent: number | null;
  averageStudentAge: number | null;
  quizCount: number;
  axisCount: number;
  questionCount: number;
  globalProgressPercent: number | null;
  averageResponseTimeSeconds: number | null;
  progressStatus: string;
  nextSessionSuggestion: string;
  lastQuizCreatedAt: string | null;
  axes: TutorAxisProgressDto[];
  persistentAxes: TutorAxisProgressDto[];
}

export interface TutorQuizAnswerDto {
  questionId: number;
  axisTitle: string;
  angle: string;
  questionText: string;
  expectedAnswer: string;
  answerText: string;
  options: string[];
  scorePercent: number | null;
  correct: boolean;
  feedback: string;
}

export interface TutorQuizSubmissionDto {
  id: number;
  quizId: number;
  quizTitle: string;
  animationId: number | null;
  activityName: string | null;
  enfantId: number;
  enfantName: string;
  submittedAt: string;
  elapsedSeconds: number | null;
  scorePercent: number | null;
  status: string;
  answers: TutorQuizAnswerDto[];
}

export interface ParentQuizDto {
  quiz: QuizDto;
  eligibleChildren: EnfantSummaryDto[];
  alreadySubmitted: boolean;
  latestSubmittedAt: string | null;
}

export interface QuizAnswerSubmitDto {
  questionId: number;
  answerText: string;
}

export interface QuizAttemptSubmitDto {
  enfantId: number;
  elapsedSeconds: number | null;
  answers: QuizAnswerSubmitDto[];
}

export interface QuizAttemptDto {
  id: number;
  quizId: number;
  quizTitle: string;
  enfantId: number;
  enfantName: string;
  submittedAt: string;
  elapsedSeconds: number | null;
  scorePercent: number | null;
  status: string;
}

export interface ParentQuizAttemptDetailDto extends QuizAttemptDto {
  animationId: number | null;
  activityName: string | null;
  answers: TutorQuizAnswerDto[];
}

export interface HomeworkExerciseDto {
  id: number;
  axisTitle: string;
  type: string;
  difficulty: string;
  questionText: string;
  expectedAnswer: string;
  targetMistake: string | null;
  position: number;
  options: string[];
}

export interface HomeworkDto {
  id: number;
  enfantId: number;
  enfantName: string;
  animationId: number | null;
  activityName: string | null;
  title: string;
  summary: string;
  status: string;
  createdAt: string;
  dueDate: string | null;
  exercises: HomeworkExerciseDto[];
  latestScorePercent: number | null;
  latestSubmittedAt: string | null;
}

export interface HomeworkAnswerSubmitDto {
  exerciseId: number;
  answerText: string;
}

export interface HomeworkAttemptSubmitDto {
  elapsedSeconds: number | null;
  answers: HomeworkAnswerSubmitDto[];
}

export interface HomeworkAttemptDto {
  id: number;
  assignmentId: number;
  assignmentTitle: string;
  enfantId: number;
  enfantName: string;
  submittedAt: string;
  elapsedSeconds: number | null;
  scorePercent: number | null;
  status: string;
  answers: TutorQuizAnswerDto[];
}

export interface AnimateurHomeworkStudentRowDto {
  enfantId: number;
  enfantName: string;
  assignedCount: number;
  submittedCount: number;
  remainingCount: number;
  averageScorePercent: number | null;
  latestSubmittedAt: string | null;
  difficultyStatus: string;
  difficultyLabel: string;
  weakestAxisTitle: string | null;
}

export interface AnimateurHomeworkOverviewDto {
  assignedCount: number;
  submittedCount: number;
  remainingCount: number;
  studentCount: number;
  strugglingStudentCount: number;
  weakestAxisTitle: string | null;
  weakestAxisScorePercent: number | null;
  mostFailedQuestionText: string | null;
  mostFailedQuestionCount: number;
  students: AnimateurHomeworkStudentRowDto[];
}

export interface AnimateurHomeworkStudentDetailDto {
  enfantId: number;
  enfantName: string;
  assignedCount: number;
  submittedCount: number;
  remainingCount: number;
  averageScorePercent: number | null;
  difficultyStatus: string;
  difficultyLabel: string;
  weakAxes: string[];
  quizAttempts: QuizAttemptDto[];
  failedQuestions: TutorQuizAnswerDto[];
  assignments: HomeworkDto[];
  attempts: HomeworkAttemptDto[];
}
