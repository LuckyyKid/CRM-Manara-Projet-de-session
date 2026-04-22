export interface UserModel {
  id: number;
  email: string;
  role: string;
  enabled: boolean;
  avatarUrl: string | null;
}

export interface ParentProfileModel {
  id: number;
  nom: string;
  prenom: string;
  adresse: string | null;
}

export interface AnimateurProfileModel {
  id: number;
  nom: string;
  prenom: string;
}

export interface AdminProfileModel {
  id: number;
  nom: string;
  prenom: string;
}

export interface CurrentUserModel {
  id: number;
  accountType: string | null;
  canAccessTutoringTools: boolean;
  canAccessSportPracticeTools: boolean;
  user: UserModel;
  parent: ParentProfileModel | null;
  animateur: AnimateurProfileModel | null;
  admin: AdminProfileModel | null;
}
