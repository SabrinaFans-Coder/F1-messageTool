import { useState, useEffect } from 'react';

interface Props {
  targetDate: string;
}

export default function Countdown({ targetDate }: Props) {
  const [timeLeft, setTimeLeft] = useState({ days: 0, hours: 0, mins: 0, secs: 0 });

  useEffect(() => {
    const target = new Date(targetDate).getTime();

    const tick = () => {
      const now = Date.now();
      const diff = Math.max(0, target - now);
      setTimeLeft({
        days: Math.floor(diff / (1000 * 60 * 60 * 24)),
        hours: Math.floor((diff / (1000 * 60 * 60)) % 24),
        mins: Math.floor((diff / (1000 * 60)) % 60),
        secs: Math.floor((diff / 1000) % 60),
      });
    };

    tick();
    const interval = setInterval(tick, 1000);
    return () => clearInterval(interval);
  }, [targetDate]);

  const items = [
    { value: timeLeft.days, label: 'Days' },
    { value: timeLeft.hours, label: 'Hours' },
    { value: timeLeft.mins, label: 'Mins' },
    { value: timeLeft.secs, label: 'Secs' },
  ];

  return (
    <div style={{ display: 'flex', gap: 16, marginTop: 16 }}>
      {items.map((item) => (
        <div key={item.label} style={{ textAlign: 'center' }}>
          <div
            style={{
              fontFamily: "'Fira Code', monospace",
              fontSize: 32,
              fontWeight: 700,
              color: 'var(--accent)',
              lineHeight: 1,
            }}
          >
            {String(item.value).padStart(2, '0')}
          </div>
          <div style={{ fontSize: 11, color: 'var(--text-muted)', textTransform: 'uppercase', marginTop: 4 }}>
            {item.label}
          </div>
        </div>
      ))}
    </div>
  );
}
