const AVATAR_PALETTE = [
  '#5B6EF5',
  '#E0637A',
  '#2FA894',
  '#D98A3D',
  '#8C6FE0',
  '#3D9BD9',
  '#C4548F',
  '#4FA35A',
];

export function avatarColor(name: string): string {
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  return AVATAR_PALETTE[Math.abs(hash) % AVATAR_PALETTE.length];
}
