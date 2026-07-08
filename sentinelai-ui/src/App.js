import React, { useState } from 'react';
import ElkInvestigationPage from './ElkInvestigationPage';

// ── Navigation styles ──────────────────────────────────────────────────────
const navStyles = {
  wrapper: {
    fontFamily: "'Segoe UI', Arial, sans-serif",
    minHeight: '100vh',
    background: '#f5f7fa',
  },
  nav: {
    background: '#1a1a2e',
    padding: '0 32px',
    display: 'flex',
    alignItems: 'center',
    gap: 0,
    boxShadow: '0 2px 6px rgba(0,0,0,0.18)',
  },
  brand: {
    color: '#fff',
    fontWeight: 700,
    fontSize: 18,
    marginRight: 32,
    letterSpacing: 0.5,
    padding: '16px 0',
  },
  tab: (active) => ({
    padding: '16px 20px',
    cursor: 'pointer',
    color: active ? '#fff' : '#aab',
    fontWeight: active ? 700 : 400,
    fontSize: 14,
    userSelect: 'none',
    transition: 'color 0.15s',
    background: 'none',
    border: 'none',
    borderBottom: active ? '3px solid #4fc3f7' : '3px solid transparent',
  }),
  content: { maxWidth: 760, margin: '32px auto', padding: '0 16px' },
};

// ── Existing RCA page (unchanged logic, restyled wrapper) ──────────────────
function RcaPage() {
  const [logText, setLogText] = useState('');
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const readFileAsText = (inputFile) =>
    new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result || ''));
      reader.onerror = () => reject(reader.error || new Error('Failed to read file'));
      reader.readAsText(inputFile);
    });

  const handleFileChange = (e) => {
    setFile(e.target.files[0]);
    setLogText('');
  };

  const handleTextChange = (e) => {
    setLogText(e.target.value);
    setFile(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setResult(null);
    setError(null);

    try {
      let contentToAnalyze = '';

      if (file) {
        contentToAnalyze = await readFileAsText(file);
      } else if (logText.trim()) {
        contentToAnalyze = logText;
      } else {
        setLoading(false);
        return;
      }

      const response = await fetch('/api/rca/analyze', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ log: contentToAnalyze }),
      });

      if (!response.ok) {
        const text = await response.text().catch(() => '');
        throw new Error(`API error ${response.status}: ${text || response.statusText}`);
      }

      const data = await response.json();
      setResult(data);
    } catch (err) {
      setError(err?.message || 'Unexpected error');
    }
    setLoading(false);
  };

  const evidenceCount = (items) => Array.isArray(items) ? items.length : 0;

  const renderEvidenceList = (title, items, getLabel) => {
    if (!Array.isArray(items) || items.length === 0) {
      return null;
    }

    return (
      <div style={styles.evidenceGroup}>
        <h4 style={styles.evidenceGroupTitle}>{title}</h4>
        <ul style={styles.evidenceList}>
          {items.slice(0, 5).map((item, index) => (
            <li key={`${title}-${index}`} style={styles.evidenceItem}>
              {getLabel(item)}
            </li>
          ))}
        </ul>
      </div>
    );
  };

  return (
    <div style={{ maxWidth: 760, margin: '0 auto', padding: 24, border: '1px solid #eee', borderRadius: 8, background: '#fff' }}>
      <h2>SentinelAI Log RCA</h2>
      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 16 }}>
          <label>Paste Log Text:</label>
          <textarea
            value={logText}
            onChange={handleTextChange}
            rows={6}
            style={{ width: '100%', marginTop: 8 }}
            placeholder="Paste your log here..."
          />
        </div>
        <div style={{ marginBottom: 16 }}>
          <label>Or Upload Log File:</label>
          <input type="file" accept=".txt,.log" onChange={handleFileChange} />
        </div>
        <button type="submit" disabled={loading} style={{ padding: '8px 24px' }}>
          {loading ? 'Analyzing...' : 'Analyze Log'}
        </button>
      </form>
      {error && (
        <div style={{ marginTop: 16, color: '#b00020' }}>
          <strong>Error:</strong> {error}
        </div>
      )}
      {result && (
        <div style={{ marginTop: 32, background: '#f9f9f9', padding: 16, borderRadius: 6 }}>
          <h3>RCA Result</h3>
          <div style={styles.resultGrid}>
            <div style={styles.resultCard}>
              <strong>Issue</strong>
              <p>{result.issue || 'Not provided'}</p>
            </div>
            <div style={styles.resultCard}>
              <strong>Root Cause</strong>
              <p>{result.rootCause || 'Not provided'}</p>
            </div>
            <div style={styles.resultCard}>
              <strong>Impacted Service</strong>
              <p>{result.impactedService || 'Not provided'}</p>
            </div>
            <div style={styles.resultCard}>
              <strong>Recommended Fix</strong>
              <p>{result.recommendedFix || 'Not provided'}</p>
            </div>
          </div>

          {result.engineeringContext && (
            <div style={styles.evidencePanel}>
              <div style={styles.evidenceHeader}>
                <h3 style={{ margin: 0 }}>Engineering Evidence</h3>
                <span style={{
                  ...styles.statusPill,
                  background: result.engineeringContext.available ? '#e7f7ee' : '#f5f5f5',
                  color: result.engineeringContext.available ? '#176b3a' : '#666',
                }}>
                  {result.engineeringContext.enabled
                    ? (result.engineeringContext.available ? 'SKS connected' : 'SKS unavailable')
                    : 'SKS disabled'}
                </span>
              </div>
              <p style={styles.evidenceMessage}>{result.engineeringContext.message}</p>
              {result.engineeringContext.available && (
                <>
                  <div style={styles.evidenceStats}>
                    <span>{evidenceCount(result.engineeringContext.timeline)} timeline events</span>
                    <span>{evidenceCount(result.engineeringContext.deployments)} deployments</span>
                    <span>{evidenceCount(result.engineeringContext.releases)} releases</span>
                    <span>{evidenceCount(result.engineeringContext.jiraIssues)} Jira issues</span>
                  </div>

                  {renderEvidenceList('Timeline', result.engineeringContext.timeline, (item) =>
                    `${item.occurredAt || ''} ${item.eventType || ''} ${item.title || ''}`
                  )}
                  {renderEvidenceList('Deployments', result.engineeringContext.deployments, (item) =>
                    `${item.environment || 'environment'} ${item.releaseVersion || ''} ${item.buildStatus || ''}`
                  )}
                  {renderEvidenceList('Releases', result.engineeringContext.releases, (item) =>
                    `${item.version || ''} ${item.repositoryName || ''}`
                  )}
                  {renderEvidenceList('Jira Issues', result.engineeringContext.jiraIssues, (item) =>
                    `${item.issueKey || ''} ${item.status || ''} ${item.summary || ''}`
                  )}
                </>
              )}
            </div>
          )}

          {Array.isArray(result.errors) && result.errors.length > 0 && (
            <div style={styles.errorList}>
              <strong>Warnings</strong>
              <ul>
                {result.errors.map((item, index) => <li key={index}>{item}</li>)}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

const styles = {
  resultGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
    gap: 12,
  },
  resultCard: {
    background: '#fff',
    border: '1px solid #e5e7eb',
    borderRadius: 8,
    padding: 12,
    minHeight: 92,
  },
  evidencePanel: {
    marginTop: 18,
    background: '#fff',
    border: '1px solid #d9e2ec',
    borderRadius: 8,
    padding: 14,
  },
  evidenceHeader: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
    flexWrap: 'wrap',
  },
  statusPill: {
    borderRadius: 999,
    padding: '4px 10px',
    fontSize: 12,
    fontWeight: 700,
  },
  evidenceMessage: {
    margin: '8px 0 12px',
    color: '#4b5563',
  },
  evidenceStats: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 12,
  },
  evidenceGroup: {
    borderTop: '1px solid #edf2f7',
    paddingTop: 10,
    marginTop: 10,
  },
  evidenceGroupTitle: {
    margin: '0 0 6px',
  },
  evidenceList: {
    margin: 0,
    paddingLeft: 18,
  },
  evidenceItem: {
    marginBottom: 4,
    color: '#253041',
  },
  errorList: {
    marginTop: 16,
    color: '#8a4b00',
  },
};

// ── App shell with navigation ──────────────────────────────────────────────
const PAGES = {
  rca: 'rca',
  elk: 'elk',
};

function App() {
  const [activePage, setActivePage] = useState(PAGES.rca);

  return (
    <div style={navStyles.wrapper}>
      {/* Top navigation bar */}
      <nav style={navStyles.nav}>
        <span style={navStyles.brand}>SentinelAI</span>

        <button
          style={navStyles.tab(activePage === PAGES.rca)}
          onClick={() => setActivePage(PAGES.rca)}
        >
          Log RCA
        </button>

        <button
          style={navStyles.tab(activePage === PAGES.elk)}
          onClick={() => setActivePage(PAGES.elk)}
        >
          ELK Investigation
        </button>
      </nav>

      {/* Page content */}
      <div style={navStyles.content}>
        {activePage === PAGES.rca && <RcaPage />}
        {activePage === PAGES.elk && <ElkInvestigationPage />}
      </div>
    </div>
  );
}

export default App;
