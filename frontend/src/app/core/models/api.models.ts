// ================================================================
// DTOs API — correspondent exactement aux records Java du backend
// ================================================================

export interface ActivityDto {
  id: number;
  name: string;
  description: string;
  ageMin: number;
  ageMax: number;
  capacity: number;
  status: string | null;
  type: string | null;
}

export interface ActivityRequestDto {
  name: string;
  description: string;
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
}

export interface AnimateurNotificationDto {
  id: number;
  category: string;
  title: string;
  message: string;
  createdAt: string;
  readStatus: boolean;
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
