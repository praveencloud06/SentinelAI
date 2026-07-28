import React, { useState } from 'react';
import EngineeringContextPanel from './components/context/EngineeringContextPanel';

// ══════════════════════════════════════════════════════════════════════════════
// CONFIGURATION - Enterprise Observability Filters
// ══════════════════════════════════════════════════════════════════════════════

const ENVIRONMENT_OPTIONS = ['Production', 'Staging', 'QA', 'Development'];

const APPLICATION_OPTIONS = [
  'payment-service',
  'order-service', 
  'vehicle-registration-service',
  'notification-service',
  'auth-service',
  'api-gateway',
];

const SEVERITY_OPTIONS = ['ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE'];

const TIMEFRAME_OPTIONS = [
  { label: 'Last 15 Minutes', value: 15 },
  { label: 'Last 30 Minutes', value: 30 },
  { label: 'Last 1 Hour',     value: 60 },
  { label: 'Last 6 Hours',    value: 360 },
  { label: 'Last 12 Hours',   value: 720 },
  { label: 'Last 24 Hours',   value: 1440 },
  { label: 'Last 7 Days',     value: 10080 },
  { label: 'Last 30 Days',    value: 43200 },
  { label: 'Custom Range',    value: 'custom' },
];

const MAX_LOGS_OPTIONS = [100, 250, 500, 1000];

const DEPLOYMENT_VERSIONS = [
  'v2.1.5',
  'v2.1.4-hotfix',
  'v2.1.3',
  'v3.2.1',
  'v1.8.2',
];

// ══════════════════════════════════════════════════════════════════════════════
// STYLES - Enterprise Observability Layout
// ══════════════════════════════════════════════════════════════════════════════

const styles = {
  page: { 
    maxWidth: 1400, 
    margin: '0 auto', 
    padding: '20px 24px',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  },

  pageTitle: {
    fontSize: 24,
    fontWeight: 700,
    color: '#1a1a1a',
    marginBottom: 8,
    letterSpacing: '-0.5px',
  },

  pageSubtitle: {
    fontSize: 14,
    color: '#666',
    marginBottom: 24,
  },

  card: {
    background: '#fff',
    border: '1px solid #e0e0e0',
    borderRadius: 8,
    padding: 24,
    marginBottom: 20,
    boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
  },

  filterGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
    gap: 20,
  },

  filterGroup: {
    marginBottom: 0,
  },

  filterSection: {
    marginBottom: 24,
    paddingBottom: 24,
    borderBottom: '1px solid #f0f0f0',
  },

  filterSectionLast: {
    marginBottom: 0,
    paddingBottom: 0,
    borderBottom: 'none',
  },

  sectionLabel: {
    fontSize: 12,
    fontWeight: 700,
    textTransform: 'uppercase',
    color: '#888',
    letterSpacing: '0.5px',
    marginBottom: 16,
  },

  label: { 
    display: 'block', 
    marginBottom: 6, 
    fontWeight: 600, 
    fontSize: 13, 
    color: '#333',
  },

  input: {
    width: '100%',
    padding: '9px 12px',
    border: '1px solid #d0d0d0',
    borderRadius: 6,
    fontSize: 14,
    boxSizing: 'border-box',
    transition: 'border-color 0.2s',
  },

  select: {
    width: '100%',
    padding: '9px 12px',
    border: '1px solid #d0d0d0',
    borderRadius: 6,
    fontSize: 14,
    background: '#fff',
    boxSizing: 'border-box',
    cursor: 'pointer',
    transition: 'border-color 0.2s',
  },

  checkboxGroup: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: 12,
    marginTop: 8,
  },

  checkboxLabel: {
    display: 'flex',
    alignItems: 'center',
    gap: 6,
    fontSize: 13,
    color: '#444',
    cursor: 'pointer',
    userSelect: 'none',
  },

  checkbox: {
    width: 16,
    height: 16,
    cursor: 'pointer',
  },

  checkboxContainer: {
    display: 'flex',
    alignItems: 'center',
    gap: 6,
    marginBottom: 8,
  },

  buttonRow: {
    display: 'flex',
    alignItems: 'center',
    gap: 16,
    marginTop: 24,
    paddingTop: 24,
    borderTop: '1px solid #f0f0f0',
  },

  button: {
    padding: '11px 32px',
    background: 'linear-gradient(135deg, #1a73e8 0%, #1557b0 100%)',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    fontSize: 14,
    fontWeight: 600,
    cursor: 'pointer',
    boxShadow: '0 2px 4px rgba(26,115,232,0.2)',
    transition: 'all 0.2s',
  },

  buttonDisabled: {
    padding: '11px 32px',
    background: '#ccc',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    fontSize: 14,
    fontWeight: 600,
    cursor: 'not-allowed',
  },

  resultCard: {
    background: '#fff',
    border: '1px solid #e0e0e0',
    borderRadius: 8,
    padding: 24,
    marginBottom: 16,
    boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
  },

  resultTitle: { 
    fontSize: 16, 
    fontWeight: 700, 
    marginBottom: 16, 
    color: '#1a1a1a',
    display: 'flex',
    alignItems: 'center',
    gap: 8,
  },

  resultIcon: {
    fontSize: 18,
  },

  kvRow: { 
    display: 'flex', 
    gap: 12, 
    marginBottom: 10,
    alignItems: 'flex-start',
  },

  kvKey: { 
    minWidth: 200, 
    fontWeight: 600, 
    color: '#555', 
    fontSize: 13, 
    flexShrink: 0,
  },

  kvValue: { 
    flex: 1, 
    fontSize: 13, 
    color: '#222', 
    wordBreak: 'break-word',
    lineHeight: 1.5,
  },

  badge: {
    display: 'inline-block',
    padding: '4px 10px',
    borderRadius: 4,
    fontSize: 12,
    fontWeight: 600,
  },

  badgeHigh: {
    background: '#fef3f2',
    color: '#d32f2f',
    border: '1px solid #fcc',
  },

  badgeMedium: {
    background: '#fff8e1',
    color: '#f57c00',
    border: '1px solid #ffe082',
  },

  badgeLow: {
    background: '#e8f5e9',
    color: '#388e3c',
    border: '1px solid #c8e6c9',
  },

  logItem: {
    background: '#fafafa',
    border: '1px solid #e8e8e8',
    borderRadius: 4,
    padding: '10px 12px',
    marginBottom: 8,
    fontSize: 12,
    fontFamily: '"Roboto Mono", "Courier New", monospace',
    wordBreak: 'break-all',
    lineHeight: 1.6,
  },

  errorLogItem: {
    background: '#fff5f5',
    border: '1px solid #ffcccc',
    borderLeft: '3px solid #d32f2f',
  },

  error: {
    background: '#fff3f3',
    border: '1px solid #f5c6c6',
    borderRadius: 6,
    padding: '12px 16px',
    color: '#b00020',
    fontSize: 14,
    display: 'flex',
    alignItems: 'center',
    gap: 8,
  },

  spinner: { 
    color: '#1a73e8', 
    fontStyle: 'italic', 
    fontSize: 13,
  },

  summaryText: {
    margin: 0,
    fontSize: 14,
    color: '#333',
    lineHeight: 1.7,
  },

  timelineItem: {
    paddingLeft: 20,
    borderLeft: '2px solid #e0e0e0',
    marginBottom: 16,
    position: 'relative',
  },

  timelineDot: {
    position: 'absolute',
    left: -5,
    top: 4,
    width: 8,
    height: 8,
    borderRadius: '50%',
    background: '#1a73e8',
  },
};

// ══════════════════════════════════════════════════════════════════════════════
// COMPONENT - Enterprise ELK Investigation
// ══════════════════════════════════════════════════════════════════════════════

export default function ElkInvestigationPage() {
  // Core filters (backward compatible)
  const [service, setService] = useState('');
  const [severity, setSeverity] = useState('ERROR');
  const [timeframeMinutes, setTimeframeMinutes] = useState(30);

  // Enterprise filters (new)
  const [environment, setEnvironment] = useState('');
  const [application, setApplication] = useState('');
  const [selectedSeverities, setSelectedSeverities] = useState(['ERROR']);
  const [searchText, setSearchText] = useState('');
  const [requestId, setRequestId] = useState('');
  const [correlationId, setCorrelationId] = useState('');
  const [host, setHost] = useState('');
  const [deploymentVersion, setDeploymentVersion] = useState('');
  const [maxLogs, setMaxLogs] = useState(100);
  const [includeContext, setIncludeContext] = useState(true);

  // Custom time range
  const [customTimeFrom, setCustomTimeFrom] = useState('');
  const [customTimeTo, setCustomTimeTo] = useState('');

  // UI state
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const isCustomTimeRange = timeframeMinutes === 'custom';

  const handleSeverityToggle = (sev) => {
    setSelectedSeverities((prev) =>
      prev.includes(sev) ? prev.filter((s) => s !== sev) : [...prev, sev]
    );
  };

  const handleInvestigate = async (e) => {
    e.preventDefault();
    
    setLoading(true);
    setResult(null);
    setError(null);

    try {
      // Build request body with enterprise filters
      const requestBody = {
        // Backward compatible fields
        service: service.trim() || undefined,
        severity: selectedSeverities.length === 1 ? selectedSeverities[0] : undefined,
        timeframeMinutes: isCustomTimeRange ? undefined : Number(timeframeMinutes),

        // Enterprise filters
        environment: environment || undefined,
        application: application || undefined,
        severityList: selectedSeverities.length > 1 ? selectedSeverities : undefined,
        searchText: searchText.trim() || undefined,
        requestId: requestId.trim() || undefined,
        correlationId: correlationId.trim() || undefined,
        host: host.trim() || undefined,
        deploymentVersion: deploymentVersion || undefined,
        maxLogs: maxLogs,
        includeContext: includeContext,

        // Custom time range
        customTimeFrom: isCustomTimeRange && customTimeFrom ? new Date(customTimeFrom).toISOString() : undefined,
        customTimeTo: isCustomTimeRange && customTimeTo ? new Date(customTimeTo).toISOString() : undefined,
      };

      // Remove undefined fields for cleaner payload
      Object.keys(requestBody).forEach((key) => {
        if (requestBody[key] === undefined) delete requestBody[key];
      });

      const response = await fetch('/api/elk-investigation/search', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody),
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
      {/* Page Header */}
      <div style={styles.pageTitle}>🔍 ELK Investigation</div>
      <div style={styles.pageSubtitle}>
        Enterprise log analysis powered by Elasticsearch and AI
      </div>

      {/* Filter Card */}
      <div style={styles.card}>
        <form onSubmit={handleInvestigate} noValidate>
          
          {/* ── Primary Filters ───────────────────────────────────── */}
          <div style={styles.filterSection}>
            <div style={styles.sectionLabel}>Primary Filters</div>
            <div style={styles.filterGrid}>
              
              <div style={styles.filterGroup}>
                <label style={styles.label} htmlFor="elk-environment">Environment</label>
                <select
                  id="elk-environment"
                  style={styles.select}
                  value={environment}
                  onChange={(e) => setEnvironment(e.target.value)}
                >
                  <option value="">All Environments</option>
                  {ENVIRONMENT_OPTIONS.map((env) => (
                    <option key={env} value={env}>{env}</option>
                  ))}
                </select>
              </div>

              <div style={styles.filterGroup}>
                <label style={styles.label} htmlFor="elk-application">Application</label>
                <select
                  id="elk-application"
                  style={styles.select}
                  value={application}
                  onChange={(e) => setApplication(e.target.value)}
                >
                  <option value="">All Applications</option>
                  {APPLICATION_OPTIONS.map((app) => (
                    <option key={app} value={app}>{app}</option>
                  ))}
                </select>
              </div>

              <div style={styles.filterGroup}>
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

            </div>
          </div>

          {/* ── Severity & Time Range ─────────────────────────────── */}
          <div style={styles.filterSection}>
            <div style={styles.sectionLabel}>Severity & Time Range</div>
            
            <div style={{ marginBottom: 20 }}>
              <label style={styles.label}>Severity Levels</label>
              <div style={styles.checkboxGroup}>
                {SEVERITY_OPTIONS.map((sev) => (
                  <label key={sev} style={styles.checkboxLabel}>
                    <input
                      type="checkbox"
                      style={styles.checkbox}
                      checked={selectedSeverities.includes(sev)}
                      onChange={() => handleSeverityToggle(sev)}
                    />
                    {sev}
                  </label>
                ))}
              </div>
            </div>

            <div style={styles.filterGrid}>
              <div style={styles.filterGroup}>
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

              {isCustomTimeRange && (
                <>
                  <div style={styles.filterGroup}>
                    <label style={styles.label} htmlFor="elk-time-from">From</label>
                    <input
                      id="elk-time-from"
                      style={styles.input}
                      type="datetime-local"
                      value={customTimeFrom}
                      onChange={(e) => setCustomTimeFrom(e.target.value)}
                    />
                  </div>

                  <div style={styles.filterGroup}>
                    <label style={styles.label} htmlFor="elk-time-to">To</label>
                    <input
                      id="elk-time-to"
                      style={styles.input}
                      type="datetime-local"
                      value={customTimeTo}
                      onChange={(e) => setCustomTimeTo(e.target.value)}
                    />
                  </div>
                </>
              )}
            </div>
          </div>

          {/* ── Advanced Filters ──────────────────────────────────── */}
          <div style={styles.filterSection}>
            <div style={styles.sectionLabel}>Advanced Filters</div>
            <div style={styles.filterGrid}>
              
              <div style={styles.filterGroup}>
                <label style={styles.label} htmlFor="elk-search">Search Text</label>
                <input
                  id="elk-search"
                  style={styles.input}
                  type="text"
                  value={searchText}
                  onChange={(e) => setSearchText(e.target.value)}
                  placeholder="timeout, NullPointerException..."
                  autoComplete="off"
                />
              </div>

              <div style={styles.filterGroup}>
                <label style={styles.label} htmlFor="elk-request-id">Request ID</label>
                <input
                  id="elk-request-id"
                  style={styles.input}
                  type="text"
                  value={requestId}
                  onChange={(e) => setRequestId(e.target.value)}
                  placeholder="req-12345..."
                  autoComplete="off"
                />
              </div>

              <div style={styles.filterGroup}>
                <label style={styles.label} htmlFor="elk-correlation-id">Correlation ID</label>
                <input
                  id="elk-correlation-id"
                  style={styles.input}
                  type="text"
                  value={correlationId}
                  onChange={(e) => setCorrelationId(e.target.value)}
                  placeholder="corr-67890..."
                  autoComplete="off"
                />
              </div>

              <div style={styles.filterGroup}>
                <label style={styles.label} htmlFor="elk-host">Host / Pod</label>
                <input
                  id="elk-host"
                  style={styles.input}
                  type="text"
                  value={host}
                  onChange={(e) => setHost(e.target.value)}
                  placeholder="payment-pod-7f8a9b..."
                  autoComplete="off"
                />
              </div>

              <div style={styles.filterGroup}>
                <label style={styles.label} htmlFor="elk-deployment">Deployment Version</label>
                <select
                  id="elk-deployment"
                  style={styles.select}
                  value={deploymentVersion}
                  onChange={(e) => setDeploymentVersion(e.target.value)}
                >
                  <option value="">All Versions</option>
                  {DEPLOYMENT_VERSIONS.map((ver) => (
                    <option key={ver} value={ver}>{ver}</option>
                  ))}
                </select>
              </div>

              <div style={styles.filterGroup}>
                <label style={styles.label} htmlFor="elk-max-logs">Maximum Logs</label>
                <select
                  id="elk-max-logs"
                  style={styles.select}
                  value={maxLogs}
                  onChange={(e) => setMaxLogs(Number(e.target.value))}
                >
                  {MAX_LOGS_OPTIONS.map((num) => (
                    <option key={num} value={num}>{num}</option>
                  ))}
                </select>
              </div>

            </div>
          </div>

          {/* ── Options ───────────────────────────────────────────── */}
          <div style={styles.filterSectionLast}>
            <div style={styles.sectionLabel}>Options</div>
            <div style={styles.checkboxContainer}>
              <input
                type="checkbox"
                id="elk-include-context"
                style={styles.checkbox}
                checked={includeContext}
                onChange={(e) => setIncludeContext(e.target.checked)}
              />
              <label htmlFor="elk-include-context" style={{ fontSize: 13, color: '#444', cursor: 'pointer' }}>
                Include Engineering Context (GitHub, Jira, Confluence)
              </label>
            </div>
          </div>

          {/* ── Action Button ─────────────────────────────────────── */}
          <div style={styles.buttonRow}>
            <button
              type="submit"
              style={loading ? styles.buttonDisabled : styles.button}
              disabled={loading}
            >
              {loading ? '⏳ Investigating...' : '🔍 Run Investigation'}
            </button>

            {loading && (
              <span style={styles.spinner}>
                Querying Elasticsearch and running AI analysis...
              </span>
            )}
          </div>

        </form>
      </div>

      {/* ── Error Display ──────────────────────────────────────── */}
      {error && (
        <div style={styles.error}>
          <span>⚠️</span>
          <div>
            <strong>Error:</strong> {error}
          </div>
        </div>
      )}

      {/* ══════════════════════════════════════════════════════════ */}
      {/* RESULTS - Enterprise Investigation Report                  */}
      {/* ══════════════════════════════════════════════════════════ */}
      {result && (
        <>
          {/* ── Executive Summary ──────────────────────────────────── */}
          <div style={styles.resultCard}>
            <div style={styles.resultTitle}>
              <span style={styles.resultIcon}>📊</span>
              Executive Summary
            </div>
            <p style={styles.summaryText}>
              {result.summary || 'AI analysis is processing the log data...'}
            </p>
          </div>

          {/* ── Probable Root Cause ────────────────────────────────── */}
          <div style={styles.resultCard}>
            <div style={styles.resultTitle}>
              <span style={styles.resultIcon}>🎯</span>
              Likely Root Cause
            </div>

            <div style={styles.kvRow}>
              <span style={styles.kvKey}>Root Cause</span>
              <span style={styles.kvValue}>{result.probableRootCause || '—'}</span>
            </div>

            <div style={styles.kvRow}>
              <span style={styles.kvKey}>Affected Service</span>
              <span style={styles.kvValue}>{result.impactedService || service || '—'}</span>
            </div>

            <div style={styles.kvRow}>
              <span style={styles.kvKey}>Confidence</span>
              <span style={styles.kvValue}>
                {result.confidence ? (
                  <span style={{
                    ...styles.badge,
                    ...(result.confidence >= 80 ? styles.badgeHigh : 
                        result.confidence >= 50 ? styles.badgeMedium : styles.badgeLow)
                  }}>
                    {result.confidence}%
                  </span>
                ) : '—'}
              </span>
            </div>
          </div>

          {/* ── Engineering Context ────────────────────────────────── */}
          {includeContext && result.engineeringContext && (
            <div style={styles.resultCard}>
              <div style={styles.resultTitle}>
                <span style={styles.resultIcon}>🔗</span>
                Engineering Context
              </div>

              {result.relatedDeployment && (
                <div style={styles.kvRow}>
                  <span style={styles.kvKey}>Related Deployment</span>
                  <span style={styles.kvValue}>{result.relatedDeployment}</span>
                </div>
              )}

              {result.relatedJira && (
                <div style={styles.kvRow}>
                  <span style={styles.kvKey}>Related Jira</span>
                  <span style={styles.kvValue}>
                    <a href="#" style={{ color: '#1a73e8', textDecoration: 'none' }}>
                      {result.relatedJira}
                    </a>
                  </span>
                </div>
              )}

              {result.relatedCommit && (
                <div style={styles.kvRow}>
                  <span style={styles.kvKey}>Related Commit</span>
                  <span style={styles.kvValue}>
                    <code style={{ 
                      background: '#f5f5f5', 
                      padding: '2px 6px', 
                      borderRadius: 3,
                      fontSize: 12,
                      fontFamily: 'monospace',
                    }}>
                      {result.relatedCommit}
                    </code>
                  </span>
                </div>
              )}

              {result.relevantRunbook && (
                <div style={styles.kvRow}>
                  <span style={styles.kvKey}>Relevant Documentation</span>
                  <span style={styles.kvValue}>
                    <a href="#" style={{ color: '#1a73e8', textDecoration: 'none' }}>
                      {result.relevantRunbook}
                    </a>
                  </span>
                </div>
              )}

              {/* Full Engineering Context Panel */}
              <div style={{ marginTop: 16 }}>
                <EngineeringContextPanel
                  engineeringContext={result.engineeringContext}
                  title="Detailed Context"
                  defaultExpanded={false}
                />
              </div>
            </div>
          )}

          {/* ── Suspicious Log Events ──────────────────────────────── */}
          {Array.isArray(result.suspiciousLogs) && result.suspiciousLogs.length > 0 && (
            <div style={styles.resultCard}>
              <div style={styles.resultTitle}>
                <span style={styles.resultIcon}>⚠️</span>
                Suspicious Log Events
              </div>
              <div style={{ fontSize: 12, color: '#666', marginBottom: 12 }}>
                {result.suspiciousLogs.length} suspicious entries detected
              </div>
              {result.suspiciousLogs.slice(0, 10).map((line, i) => (
                <div 
                  key={i} 
                  style={{
                    ...styles.logItem,
                    ...(line.toLowerCase().includes('error') ? styles.errorLogItem : {})
                  }}
                >
                  {line}
                </div>
              ))}
              {result.suspiciousLogs.length > 10 && (
                <div style={{ fontSize: 12, color: '#888', marginTop: 8, fontStyle: 'italic' }}>
                  + {result.suspiciousLogs.length - 10} more entries (showing first 10)
                </div>
              )}
            </div>
          )}

          {/* ── Engineering Timeline ───────────────────────────────── */}
          {Array.isArray(result.timeline) && result.timeline.length > 0 && (
            <div style={styles.resultCard}>
              <div style={styles.resultTitle}>
                <span style={styles.resultIcon}>📅</span>
                Engineering Timeline
              </div>
              {result.timeline.map((event, i) => (
                <div key={i} style={styles.timelineItem}>
                  <div style={styles.timelineDot}></div>
                  <div style={{ fontSize: 12, color: '#888', marginBottom: 4 }}>
                    {event.timestamp || `Event ${i + 1}`}
                  </div>
                  <div style={{ fontSize: 13, color: '#333' }}>
                    {event.description || event}
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* ── AI Recommendations ─────────────────────────────────── */}
          <div style={styles.resultCard}>
            <div style={styles.resultTitle}>
              <span style={styles.resultIcon}>💡</span>
              AI Recommendations
            </div>

            <div style={styles.kvRow}>
              <span style={styles.kvKey}>Recommended Action</span>
              <span style={styles.kvValue}>
                {result.recommendedAction || 'No specific action recommended at this time.'}
              </span>
            </div>

            {result.nextSteps && Array.isArray(result.nextSteps) && result.nextSteps.length > 0 && (
              <div style={{ marginTop: 16 }}>
                <div style={{ fontSize: 13, fontWeight: 600, color: '#555', marginBottom: 8 }}>
                  Next Steps:
                </div>
                <ul style={{ margin: 0, paddingLeft: 20, fontSize: 13, color: '#333', lineHeight: 1.8 }}>
                  {result.nextSteps.map((step, i) => (
                    <li key={i}>{step}</li>
                  ))}
                </ul>
              </div>
            )}

            {result.escalate && (
              <div style={{ 
                marginTop: 16, 
                padding: 12, 
                background: '#fff3e0', 
                border: '1px solid #ffb74d',
                borderRadius: 4,
                fontSize: 13,
                color: '#e65100',
              }}>
                <strong>⚠️ Escalation Recommended:</strong> {result.escalationReason || 'This issue may require immediate attention.'}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
