import { useNavigate } from 'react-router-dom';
import { Calendar } from 'lucide-react';
import type { Meeting } from '../types';

interface Props {
  meeting: Meeting;
}

export default function RaceCard({ meeting }: Props) {
  const navigate = useNavigate();
  const startDate = new Date(meeting.date_start);
  const endDate = new Date(meeting.date_end);
  const formatDate = (d: Date) =>
    d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });

  return (
    <div
      style={{
        background: 'var(--surface)',
        border: '1px solid var(--border)',
        borderRadius: 12,
        overflow: 'hidden',
        cursor: 'pointer',
        transition: 'all 200ms',
        position: 'relative',
      }}
      onClick={() => navigate(`/calendar/${meeting.meeting_key}`)}
      onMouseEnter={(e) => {
        e.currentTarget.style.borderColor = 'var(--accent)';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.borderColor = 'var(--border)';
      }}
    >
      {meeting.circuit_image && (
        <div
          style={{
            height: 120,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: 'var(--surface-alt)',
            borderBottom: '1px solid var(--border)',
            position: 'relative',
            overflow: 'hidden',
          }}
        >
          <img
            src={meeting.circuit_image}
            alt={meeting.circuit_short_name}
            style={{
              height: '100%',
              width: '100%',
              objectFit: 'contain',
              padding: 12,
              opacity: 0.85,
            }}
          />
        </div>
      )}
      <div style={{ padding: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 10 }}>
          <span
            style={{
              fontFamily: "'Fira Code', monospace",
              fontSize: 11,
              fontWeight: 600,
              color: 'var(--accent)',
              textTransform: 'uppercase',
              letterSpacing: 1,
            }}
          >
            {meeting.circuit_short_name}
          </span>
          <img
            src={meeting.country_flag}
            alt={meeting.country_name}
            style={{ width: 24, height: 16, borderRadius: 2, objectFit: 'cover' }}
          />
        </div>
        <div
          style={{
            fontFamily: "'Fira Code', monospace",
            fontSize: 15,
            fontWeight: 600,
            marginBottom: 4,
            lineHeight: 1.3,
          }}
        >
          {meeting.meeting_name}
        </div>
        <div style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 10 }}>
          {meeting.location}, {meeting.country_name}
        </div>
        <div style={{ fontSize: 12, color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: 6 }}>
          <Calendar size={13} />
          {formatDate(startDate)} - {formatDate(endDate)}
        </div>
      </div>
    </div>
  );
}
