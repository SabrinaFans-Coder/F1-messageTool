import { useState, useEffect } from 'react';

const TIPS: Record<string, string[]> = {
  general: [
    'Warming up the tyres...',
    'Checking the telemetry...',
    'Preparing the pit lane...',
    'Loading race data...',
    'The marshals are getting ready...',
  ],
  calendar: [
    'Loading the 2026 race calendar...',
    'Mapping out the circuits...',
    'Checking flight schedules...',
    'Plotting the global tour...',
  ],
  results: [
    'Crunching the championship numbers...',
    'Calculating the standings...',
    'Tallying up the points...',
    'Reviewing the race results...',
  ],
  drivers: [
    'Loading the driver profiles...',
    'Assembling the grid...',
    'Preparing driver introductions...',
    'Rolling out the cars...',
  ],
  raceReport: [
    'Loading the race report...',
    'Reviewing lap times...',
    'Analyzing race strategy...',
    'Checking the data...',
  ],
  standings: [
    'Fetching championship standings...',
    'Updating the leaderboard...',
    'Counting the points...',
    'Checking the championship battle...',
  ],
};

interface Props {
  label?: string;
  context?: keyof typeof TIPS;
}

export default function LoadingState({ label, context = 'general' }: Props) {
  const tips = TIPS[context] ?? TIPS.general;
  const [tipIndex, setTipIndex] = useState(() => Math.floor(Math.random() * tips.length));

  useEffect(() => {
    const timer = setInterval(() => {
      setTipIndex((i) => (i + 1) % tips.length);
    }, 3000);
    return () => clearInterval(timer);
  }, [tips.length]);

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '64px 24px',
        gap: 20,
      }}
    >
      {/* Spinner */}
      <div
        style={{
          width: 40,
          height: 40,
          border: '3px solid var(--border)',
          borderTopColor: 'var(--accent)',
          borderRadius: '50%',
          animation: 'spin 0.8s linear infinite',
        }}
      />

      {/* Label */}
      {label && (
        <div
          style={{
            fontFamily: "'Fira Code', monospace",
            fontSize: 13,
            fontWeight: 600,
            color: 'var(--text)',
          }}
        >
          {label}
        </div>
      )}

      {/* Rotating tip */}
      <div
        style={{
          fontSize: 12,
          color: 'var(--text-muted)',
          textAlign: 'center',
          minHeight: 18,
          transition: 'opacity 300ms',
        }}
      >
        {tips[tipIndex]}
      </div>

      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
