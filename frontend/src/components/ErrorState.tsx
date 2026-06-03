import { AlertTriangle, RefreshCw } from 'lucide-react';

interface Props {
  message: string;
  onRetry?: () => void;
}

export default function ErrorState({ message, onRetry }: Props) {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '64px 24px',
        gap: 16,
      }}
    >
      <AlertTriangle size={32} style={{ color: 'var(--danger)', opacity: 0.7 }} />

      <div
        style={{
          fontFamily: "'Fira Code', monospace",
          fontSize: 14,
          fontWeight: 600,
          color: 'var(--text)',
          textAlign: 'center',
        }}
      >
        Pit Stop Required
      </div>

      <div
        style={{
          fontSize: 13,
          color: 'var(--text-muted)',
          textAlign: 'center',
          maxWidth: 360,
          lineHeight: 1.5,
        }}
      >
        {message.includes('429')
          ? 'The servers are under heavy load. Please wait a moment and try again.'
          : message.includes('timeout')
            ? 'The request took too long. The data source might be slow — please try again.'
            : message}
      </div>

      {onRetry && (
        <button
          onClick={onRetry}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: 6,
            fontFamily: "'Fira Code', monospace",
            fontSize: 12,
            fontWeight: 600,
            padding: '8px 16px',
            borderRadius: 8,
            border: '1px solid var(--accent)',
            background: 'transparent',
            color: 'var(--accent)',
            cursor: 'pointer',
            transition: 'all 150ms',
            marginTop: 8,
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.background = 'var(--accent)';
            e.currentTarget.style.color = '#000';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.background = 'transparent';
            e.currentTarget.style.color = 'var(--accent)';
          }}
        >
          <RefreshCw size={14} />
          Try Again
        </button>
      )}
    </div>
  );
}
