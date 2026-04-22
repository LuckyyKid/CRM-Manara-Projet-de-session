export type HomeAgeFilter = 'all' | '6-12' | '12-17' | '17-29';

export interface HomeActivityCard {
  id: string;
  title: string;
  summary: string;
  description: string;
  ageMin: number;
  ageMax: number;
  imageUrl: string;
  imageAlt: string;
  source: 'db';
}
