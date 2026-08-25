import styles from './TeamLogo.module.css';

/**
 * 自托管车队图标：将 SVG/PNG 放入 src/assets/teams/ 即可自动生效。
 * 文件名使用小写短横线命名，如 ferrari.svg、red-bull.svg、aston-martin.svg。
 * 找不到对应文件时回退为队名字母标（monogram）。
 */
const logoFiles = import.meta.glob<string>('../../assets/teams/*.{svg,png,webp}', {
  eager: true,
  import: 'default',
  query: '?url',
});

const assetByName = new Map<string, string>();
for (const [path, url] of Object.entries(logoFiles)) {
  const name = path.split('/').pop()?.replace(/\.(svg|png|webp)$/, '');
  if (name) assetByName.set(name, url);
}

function resolveLogo(teamName?: string): string | undefined {
  if (!teamName) return undefined;
  const slug = teamName
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '');
  const exact = assetByName.get(slug);
  if (exact) return exact;
  // 兼容全称匹配：如 "Scuderia Ferrari" 命中 ferrari.svg
  for (const [key, url] of assetByName) {
    if (slug.includes(key)) return url;
  }
  return undefined;
}

interface Props {
  teamName: string;
  colour?: string;
  size?: number;
  className?: string;
}

export default function TeamLogo({ teamName, colour = '#666666', size = 52, className }: Props) {
  const logoUrl = resolveLogo(teamName);

  if (logoUrl) {
    return (
      <img
        src={logoUrl}
        alt={`${teamName} logo`}
        width={size}
        height={size}
        className={className ?? styles.logo}
        loading="lazy"
      />
    );
  }

  return (
    <div
      className={className ?? styles.logo}
      style={{ background: colour, width: size, height: size, fontSize: size * 0.36 }}
      aria-hidden="true"
    >
      {teamName.substring(0, 2).toUpperCase()}
    </div>
  );
}
