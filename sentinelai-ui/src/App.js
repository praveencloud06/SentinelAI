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
    borderBottom: active ? '3px solid #4fc3f7' : '3px solid transparent',
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

  return (
    <div style={{ maxWidth: 600, margin: '0 auto', padding: 24, border: '1px solid #eee', borderRadius: 8, background: '#fff' }}>
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
          <pre style={{ whiteSpace: 'pre-wrap' }}>{JSON.stringify(result, null, 2)}</pre>
        </div>
      )}
    </div>
  );
}

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

