import React, { useState } from 'react';

const SEVERITY_OPTIONS = ['ERROR', 'WARN', 'INFO', 'DEBUG'];
const TIMEFRAME_OPTIONS = [
  { label: 'Last 15 minutes', value: 15 },
  { label: 'Last 30 minutes', value: 30 },
  { label: 'Last 1 hour',     value: 60 },
  { label: 'Last 3 hours',    value: 180 },
  { label: 'Last 6 hours',    value: 360 },
  { label: 'Last 24 hours',   value: 1440 },
];

const styles = {
  page: { maxWidth: 720, margin: '0 auto', padding: '0 8px' },

  card: {
    background: '#fff',
    border: '1px solid #e0e0e0',
    borderRadius: 8,
    padding: 24,
    marginBottom: 24,
    boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
  },

  label: { display: 'block', marginBottom: 6, fontWeight: 600, fontSize: 13, color: '#444' },

  input: {
    width: '100%',
    padding: '8px 10px',
    border: '1px solid #ccc',
    borderRadius: 6,
    fontSize: 14,
    boxSizing: 'border-box',
  },

  select: {
    width: '100%',
    padding: '8px 10px',
    border: '1px solid #ccc',
    borderRadius: 6,
    fontSize: 14,
    background: '#fff',
    boxSizing: 'border-box',
  },

  fieldGroup: { marginBottom: 16 },

  button: {
    padding: '10px 28px',
    background: '#1a73e8',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    fontSize: 14,
    fontWeight: 600,
    cursor: 'pointer',
  },

  buttonDisabled: {
    padding: '10px 28px',
    background: '#aaa',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    fontSize: 14,
    fontWeight: 600,
    cursor: 'not-allowed',
  },

  sectionTitle: { fontSize: 15, fontWeight: 700, marginBottom: 12, color: '#222' },

  kvRow: { display: 'flex', gap: 8, marginBottom: 8 },
  kvKey:   { width: 180, fontWeight: 600, color: '#555', fontSize: 13, flexShrink: 0 },
  kvValue: { flex: 1, fontSize: 13, color: '#222', wordBreak: 'break-word' },

  logItem: {
    background: '#fdf3f3',
    border: '1px solid #f5c6c6',
    borderRadius: 4,
    padding: '6px 10px',
    marginBottom: 6,
    fontSize: 12,
    fontFamily: 'monospace',
    wordBreak: 'break-all',
  },

  error: {
    background: '#fff3f3',
    border: '1px solid #f5c6c6',
    borderRadius: 6,
    padding: '10px 14px',
    color: '#b00020',
    fontSize: 13,
  },

  spinner: { color: '#1a73e8', fontStyle: 'italic', fontSize: 13 },
};

export default function ElkInvestigationPage() {
  const [service,          setService]          = useState('');
  const [severity,         setSeverity]         = useState('ERROR');
  const [timeframeMinutes, setTimeframeMinutes] = useState(30);
  const [loading,          setLoading]          = useState(false);
  const [result,           setResult]           = useState(null);
  const [error,            setError]            = useState(null);

  const handleInvestigate = async (e) => {
    e.preventDefault();
    if (!service.trim()) {
      setError('Service name is required.');
      return;
    }

    setLoading(true);
    setResult(null);
    setError(null);

    try {
      const response = await fetch('/api/elk-investigation/search', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          service:          service.trim(),
          severity,
          timeframeMinutes: Number(timeframeMinutes),
        }),
      });

      if (!response.ok) {
        const text = await response.text().catch(() => '');
        throw new Error(`API error ${response.status}: ${text || response.statusText}`);
      }

      const data = await response.json();
      setResult(data);
    } catch (err) {
      setError(err?.message || 'Unexpected error occurred.');
    }

    setLoading(false);
  };

  return (
    <div style={styles.page}>

      {/* ── Input card ──────────────────────────────────────────── */}
      <div style={styles.card}>
        <div style={{ ...styles.sectionTitle, fontSize: 17, marginBottom: 20 }}>
          ELK Investigation
        </div>
        <form onSubmit={handleInvestigate} noValidate>

          <div style={styles.fieldGroup}>
            <label style={styles.label} htmlFor="elk-service">Service Name</label>
            <input
              id="elk-service"
              style={styles.input}
              type="text"
              value={service}
              onChange={(e) => setService(e.target.value)}
              placeholder="e.g. payment-service"
              autoComplete="off"
            />
          </div>

          <div style={styles.fieldGroup}>
            <label style={styles.label} htmlFor="elk-severity">Severity</label>
            <select
              id="elk-severity"
              style={styles.select}
              value={severity}
              onChange={(e) => setSeverity(e.target.value)}
            >
              {SEVERITY_OPTIONS.map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </div>

          <div style={styles.fieldGroup}>
            <label style={styles.label} htmlFor="elk-timeframe">Time Range</label>
            <select
              id="elk-timeframe"
              style={styles.select}
              value={timeframeMinutes}
              onChange={(e) => setTimeframeMinutes(e.target.value)}
            >
              {TIMEFRAME_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </div>

          <button
            type="submit"
            style={loading ? styles.buttonDisabled : styles.button}
            disabled={loading}
          >
            {loading ? 'Investigating…' : 'Investigate'}
          </button>

          {loading && (
            <span style={{ ...styles.spinner, marginLeft: 14 }}>
              Querying Elasticsearch and running AI analysis…
            </span>
          )}
        </form>
      </div>

      {/* ── Error display ───────────────────────────────────────── */}
      {error && (
        <div style={styles.error}>
          <strong>Error:</strong> {error}
        </div>
      )}

      {/* ── Results ─────────────────────────────────────────────── */}
      {result && (
        <>
          {/* Summary */}
          <div style={styles.card}>
            <div style={styles.sectionTitle}>Investigation Summary</div>
            <p style={{ margin: 0, fontSize: 14, color: '#222', lineHeight: 1.6 }}>
              {result.summary || '—'}
            </p>
          </div>

          {/* Key findings */}
          <div style={styles.card}>
            <div style={styles.sectionTitle}>Key Findings</div>

            <div style={styles.kvRow}>
              <span style={styles.kvKey}>Probable Root Cause</span>
              <span style={styles.kvValue}>{result.probableRootCause || '—'}</span>
            </div>

            <div style={styles.kvRow}>
              <span style={styles.kvKey}>Impacted Service</span>
              <span style={styles.kvValue}>{result.impactedService || '—'}</span>
            </div>

            <div style={styles.kvRow}>
              <span style={styles.kvKey}>Recommended Action</span>
              <span style={styles.kvValue}>{result.recommendedAction || '—'}</span>
            </div>
          </div>

          {/* Suspicious logs */}
          {Array.isArray(result.suspiciousLogs) && result.suspiciousLogs.length > 0 && (
            <div style={styles.card}>
              <div style={styles.sectionTitle}>
                Suspicious Logs ({result.suspiciousLogs.length})
              </div>
              {result.suspiciousLogs.map((line, idx) => (
                <div key={idx} style={styles.logItem}>{line}</div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}
