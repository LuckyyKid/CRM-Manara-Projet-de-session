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
  source: 'db' | 'mock';
}

export const HOME_ACTIVITY_MOCKS: HomeActivityCard[] = [
  {
    id: 'mock-camp-ete',
    title: 'Camp de jour ete',
    summary: "Une experience estivale avec jeux, sorties creatrices et activites sportives adaptees au rythme des plus jeunes.",
    description: "Une experience estivale avec jeux, sorties creatrices et activites sportives adaptees au rythme des plus jeunes.",
    ageMin: 6,
    ageMax: 12,
    imageUrl: 'https://gojeunesse.org/wp-content/uploads/2025/08/go-jeunesse-camp-de-jour-4.webp',
    imageAlt: 'Enfants participant a un camp de jour',
    source: 'mock',
  },
  {
    id: 'mock-robotique',
    title: 'Atelier robotique',
    summary: 'Decouverte de la robotique, logique de programmation et creation de projets concrets en petit groupe.',
    description: 'Decouverte de la robotique, logique de programmation et creation de projets concrets en petit groupe.',
    ageMin: 12,
    ageMax: 17,
    imageUrl: 'https://t4.ftcdn.net/jpg/04/72/89/75/360_F_472897567_AehnXELjs1uA2wdYyyHNhRG2kCh1Pasy.jpg',
    imageAlt: 'Atelier de robotique',
    source: 'mock',
  },
];
