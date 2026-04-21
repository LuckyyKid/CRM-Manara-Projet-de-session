export type AnimationTimeStatus = 'UPCOMING' | 'ONGOING' | 'COMPLETED' | 'UNKNOWN';

export function animationTimeStatus(
  startTime: string | null | undefined,
  endTime: string | null | undefined,
  now = new Date(),
): AnimationTimeStatus {
  const nowTime = now.getTime();
  const start = startTime ? new Date(startTime).getTime() : Number.NaN;
  const end = endTime ? new Date(endTime).getTime() : Number.NaN;

  if (!Number.isNaN(end) && nowTime > end) {
    return 'COMPLETED';
  }
  if (!Number.isNaN(start) && nowTime < start) {
    return 'UPCOMING';
  }
  if (!Number.isNaN(start) && !Number.isNaN(end) && nowTime >= start && nowTime <= end) {
    return 'ONGOING';
  }
  return 'UNKNOWN';
}

export function isAnimationCompleted(startTime: string | null | undefined, endTime: string | null | undefined): boolean {
  return animationTimeStatus(startTime, endTime) === 'COMPLETED';
}

export function isAnimationActiveOrUpcoming(startTime: string | null | undefined, endTime: string | null | undefined): boolean {
  const status = animationTimeStatus(startTime, endTime);
  return status === 'UPCOMING' || status === 'ONGOING' || status === 'UNKNOWN';
}

export function animationTimeStatusLabel(status: AnimationTimeStatus): string {
  switch (status) {
    case 'UPCOMING':
      return 'A venir';
    case 'ONGOING':
      return 'En cours';
    case 'COMPLETED':
      return 'Terminee';
    default:
      return 'Date inconnue';
  }
}
