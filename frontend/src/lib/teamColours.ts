/**
 * 车队官方涂装色映射（Jolpica 数据源不提供颜色，此处静态维护）
 * 命名规范：小写短横线
 */
const TEAM_COLOURS: Record<string, string> = {
  mercedes: '#00D7B6',
  ferrari: '#EF1A2D',
  mclaren: '#FF8000',
  'red-bull': '#3671C6',
  'racing-bulls': '#6692FF',
  'aston-martin': '#229971',
  alpine: '#0093CC',
  williams: '#64C4FF',
  haas: '#B6BABD',
  audi: '#F50537',
  cadillac: '#20B2AA',
};

/** 别名兼容（如 "RB F1 Team" → racing-bulls） */
const ALIASES: Record<string, string> = {
  'rb-f1-team': 'racing-bulls',
  'visa-cash-app-rb': 'racing-bulls',
  'scuderia-ferrari': 'ferrari',
};

function slugify(name: string): string {
  return name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');
}

/**
 * 解析车队颜色：优先使用数据源自带值，缺失时按队名匹配官方配色
 */
export function resolveTeamColour(teamName?: string, sourceColour?: string): string {
  const fromSource =
    sourceColour && sourceColour !== '' && sourceColour !== '888888'
      ? sourceColour.startsWith('#') ? sourceColour : `#${sourceColour}`
      : undefined;
  if (fromSource && fromSource !== '#888888') {
    return fromSource;
  }
  if (!teamName) return '#94A3B8';
  const slug = slugify(teamName);
  const alias = ALIASES[slug];
  if (alias) return TEAM_COLOURS[alias];
  if (TEAM_COLOURS[slug]) return TEAM_COLOURS[slug];
  for (const [key, colour] of Object.entries(TEAM_COLOURS)) {
    if (slug.includes(key)) return colour;
  }
  return '#94A3B8';
}
