/**
 * EngineeringContextPanel — Reusable component
 *
 * Displays engineering context metadata returned by the Knowledge Service.
 * Used in three places:
 *   1. Log RCA result page   — collapsible section after RCA completes
 *   2. ELK Investigation     — collapsible section after investigation completes
 *   3. Engineering Explorer  — full-page context display
 *
 * Props
 * ─────
 * engineeringContext  object   Raw engineeringContext from RCA/ELK response
 *                              OR ContextRetrievalResponse from the Explorer.
 *                              Both shapes are handled.
 * title               string   Section heading (default: "Engineering Context Used")
 * defaultExpanded     bool     Whether the panel starts open (default: false)
 * compact             bool     Condenses the panel for inline use in result cards
 */

import React, { useState } from 'react';
import EngineeringEvidenceCard from './EngineeringEvidenceCard';

// ─────────────────────────────────────────────────────────────────────────────
// Styles — dark-mode-friendly, matching the existing app palette
// ─────────────────────────────────────────────────────────────────────────────
const s = {
  wrapper: {
    marginTop: 20,
    border: '1px solid #2d3748',
    borderRadius: 8,
    overflow: 'hidden',
    fontFamily: "'Segoe UI', Arial, sans-serif",
  },

  // ── Toggle header ──────────────────────────────────────────────────────────
  header: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '12px 16px',
    background: '#1a202c',
    cursor: 'pointer',
    userSelect: 'none',
    transition: 'background 0.15s',
  },
  headerHover: { background: '#2d3748' },
  headerLeft: { display: 'flex', alignItems: 'center', gap: 10 },
  headerIcon: { fontSize: 18 },
  headerTitle: { fontSize: 15, fontWeight: 600, color: '#e2e8f0' },
  headerBadges: { display: 'flex', alignItems: 'center', gap: 8 },

  confidencePill: (conf) => ({
    padding: '3px 10px',
    borderRadius: 999,
    fontSize: 12,
    fontWeight: 700,
    background: conf >= 70 ? '#065f46' : conf >= 40 ? '#78350f' : '#1f2937',
    color:      conf >= 70 ? '#6ee7b7' : conf >= 40 ? '#fde68a' : '#9ca3af',
  }),

  evidencePill: {
    padding: '3px 10px',
    borderRadius: 999,
    fontSize: 12,
    fontWeight: 600,
    background: '#1e3a5f',
    color: '#93c5fd',
  },

  chevron: (open) => ({
    fontSize: 11,
    color: '#6b7280',
    transform: open ? 'rotate(180deg)' : 'rotate(0deg)',
    transition: 'transform 0.2s',
    marginLeft: 4,
  }),

  // ── Collapsed summary bar (compact mode) ──────────────────────────────────
  collapsedBar: {
    padding: '8px 16px',
    background: '#111827',
    fontSize: 13,
    color: '#9ca3af',
    fontStyle: 'italic',
  },

  // ── Expanded body ─────────────────────────────────────────────────────────
  body: {
    padding: '16px',
    background: '#111827',
    borderTop: '1px solid #374151',
  },

  // ── Summary strip (confidence + evidence count + summary text) ─────────────
  summaryStrip: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
    gap: 12,
    marginBottom: 16,
  },
  statBox: {
    padding: '10px 14px',
    background: '#1a202c',
    borderRadius: 6,
    border: '1px solid #2d3748',
  },
  statLabel: {
    fontSize: 11,
    color: '#6b7280',
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
    marginBottom: 4,
  },
  statValue: {
    fontSize: 22,
    fontWeight: 700,
    color: '#e2e8f0',
  },
  statSub: {
    fontSize: 11,
    color: '#9ca3af',
    marginTop: 2,
  },

  summaryText: {
    padding: '10px 14px',
    background: '#1a202c',
    borderRadius: 6,
    border: '1px solid #2d3748',
    fontSize: 13,
    color: '#d1d5db',
    lineHeight: 1.6,
    gridColumn: '1 / -1',          // always spans full width
  },

  // ── Evidence section ───────────────────────────────────────────────────────
  sectionTitle: {
    fontSize: 14,
    fontWeight: 600,
    color: '#9ca3af',
    textTransform: 'uppercase',
    letterSpacing: '0.06em',
    marginBottom: 12,
    marginTop: 4,
  },

  evidenceGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
    gap: 12,
  },

  // Override card colours for the dark-background context
  evidenceCardOverride: {
    background: '#1a202c',
    border: '1px solid #2d3748',
  },

  // ── Legacy polling data (timeline, deployments, etc.) ─────────────────────
  legacySection: {
    marginTop: 16,
    borderTop: '1px solid #374151',
    paddingTop: 16,
  },
  legacySectionTitle: {
    fontSize: 13,
    fontWeight: 600,
    color: '#6b7280',
    marginBottom: 8,
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
  },
  legacyList: { margin: 0, paddingLeft: 16 },
  legacyItem: {
    marginBottom: 4,
    fontSize: 12,
    color: '#9ca3af',
  },

  // ── Unavailable state ──────────────────────────────────────────────────────
  unavailableNotice: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    padding: '10px 14px',
    background: '#1a202c',
    borderRadius: 6,
    border: '1px solid #374151',
    fontSize: 13,
    color: '#6b7280',
  },
};

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Normalise the two different shapes into one internal model so the render
 * logic only has to deal with a single shape.
 *
 * Shape A — from RCA / ELK response (plain object):
 *   { enabled, available, message, summary, confidence, evidenceCount,
 *     timeline[], deployments[], releases[], jiraIssues[] }
 *
 * Shape B — from ContextRetrievalResponse class (Explorer):
 *   { summary, evidence[], metadata: { totalCandidates, filteredResults, … } }
 */
function normalise(ctx) {
  if (!ctx) return null;

  // Shape B — has an `evidence` array (Explorer response)
  if (Array.isArray(ctx.evidence)) {
    const conf = ctx.metadata?.getAverageConfidence?.() ??
                 ctx.metadata?.filteredResults != null
                   ? Math.min(100, Math.round(
                       (ctx.metadata.filteredResults / (ctx.metadata.totalCandidates || 1)) * 100))
                   : 0;
    return {
      available:     true,
      summary:       ctx.summary || '',
      confidence:    conf,
      evidenceCount: ctx.evidence.length,
      evidence:      ctx.evidence,
      timeline:      [],
      deployments:   [],
      releases:      [],
      jiraIssues:    [],
    };
  }

  // Shape A — from RCA / ELK
  return {
    available:     ctx.available ?? false,
    message:       ctx.message   || '',
    summary:       ctx.summary   || '',
    confidence:    ctx.confidence   || 0,
    evidenceCount: ctx.evidenceCount || 0,
    evidence:      [],
    timeline:      ctx.timeline    || [],
    deployments:   ctx.deployments || [],
    releases:      ctx.releases    || [],
    jiraIssues:    ctx.jiraIssues  || [],
  };
}

function hasSomeLegacyData(m) {
  return (m.timeline.length + m.deployments.length +
          m.releases.length + m.jiraIssues.length) > 0;
}

function LegacyList({ title, items, getLabel }) {
  if (!items || items.length === 0) return null;
  return (
    <div style={{ marginBottom: 10 }}>
      <div style={s.legacySectionTitle}>{title}</div>
      <ul style={s.legacyList}>
        {items.slice(0, 5).map((item, i) => (
          <li key={i} style={s.legacyItem}>{getLabel(item)}</li>
        ))}
      </ul>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Main component
// ─────────────────────────────────────────────────────────────────────────────
const EngineeringContextPanel = ({
  engineeringContext,
  title            = 'Engineering Context Used',
  defaultExpanded  = false,
  compact          = false,
}) => {
  const [expanded,     setExpanded]     = useState(defaultExpanded);
  const [headerHovered, setHeaderHovered] = useState(false);

  const model = normalise(engineeringContext);

  // Nothing to show
  if (!model) return null;

  const { available, message, summary, confidence, evidenceCount,
          evidence, timeline, deployments, releases, jiraIssues } = model;

  // If explicitly unavailable and nothing to show, render a minimal notice
  if (!available && !summary && evidenceCount === 0 && evidence.length === 0) {
    return (
      <div style={s.wrapper}>
        <div style={s.unavailableNotice}>
          <span>ℹ️</span>
          <span>{message || 'Engineering context not available for this investigation.'}</span>
        </div>
      </div>
    );
  }

  const totalEvidence = evidence.length || evidenceCount;

  return (
    <div style={s.wrapper}>
      {/* ── Toggle header ───────────────────────────────────────────────── */}
      <div
        style={{ ...s.header, ...(headerHovered ? s.headerHover : {}) }}
        onClick={() => setExpanded(e => !e)}
        onMouseEnter={() => setHeaderHovered(true)}
        onMouseLeave={() => setHeaderHovered(false)}
        role="button"
        aria-expanded={expanded}
      >
        <div style={s.headerLeft}>
          <span style={s.headerIcon}>🔍</span>
          <span style={s.headerTitle}>{title}</span>
        </div>

        <div style={s.headerBadges}>
          {confidence > 0 && (
            <span style={s.confidencePill(confidence)}>
              {confidence}% confidence
            </span>
          )}
          {totalEvidence > 0 && (
            <span style={s.evidencePill}>
              {totalEvidence} artifact{totalEvidence !== 1 ? 's' : ''}
            </span>
          )}
          <span style={s.chevron(expanded)}>▼</span>
        </div>
      </div>

      {/* ── Collapsed one-liner (visible when header is collapsed) ────── */}
      {!expanded && summary && (
        <div style={s.collapsedBar}>{summary}</div>
      )}

      {/* ── Expanded body ────────────────────────────────────────────── */}
      {expanded && (
        <div style={s.body}>

          {/* Summary strip */}
          <div style={s.summaryStrip}>
            {confidence > 0 && (
              <div style={s.statBox}>
                <div style={s.statLabel}>Confidence</div>
                <div style={{
                  ...s.statValue,
                  color: confidence >= 70 ? '#6ee7b7'
                       : confidence >= 40 ? '#fde68a'
                       : '#f87171',
                }}>
                  {confidence}%
                </div>
                <div style={s.statSub}>
                  {confidence >= 70 ? 'High relevance'
                   : confidence >= 40 ? 'Medium relevance'
                   : 'Low relevance'}
                </div>
              </div>
            )}

            {totalEvidence > 0 && (
              <div style={s.statBox}>
                <div style={s.statLabel}>Evidence</div>
                <div style={s.statValue}>{totalEvidence}</div>
                <div style={s.statSub}>engineering artifacts</div>
              </div>
            )}

            {hasSomeLegacyData(model) && (
              <div style={s.statBox}>
                <div style={s.statLabel}>Context Data</div>
                <div style={s.statValue}>
                  {timeline.length + deployments.length +
                   releases.length + jiraIssues.length}
                </div>
                <div style={s.statSub}>timeline + deployment records</div>
              </div>
            )}

            {summary && (
              <div style={s.summaryText}>{summary}</div>
            )}
          </div>

          {/* ── Semantic evidence cards (Explorer / semantic retrieval) ── */}
          {evidence.length > 0 && (
            <>
              <div style={s.sectionTitle}>Engineering Evidence</div>
              <div style={s.evidenceGrid}>
                {evidence.map((ev, i) => (
                  <EngineeringEvidenceCard
                    key={ev.sourceId || ev.id || i}
                    evidence={ev}
                  />
                ))}
              </div>
            </>
          )}

          {/* ── Legacy polling data ─────────────────────────────────────── */}
          {hasSomeLegacyData(model) && (
            <div style={s.legacySection}>
              <div style={s.legacySectionTitle}>Detailed Context</div>

              <LegacyList
                title="Timeline"
                items={timeline}
                getLabel={item =>
                  [item.occurredAt, item.eventType, item.title]
                    .filter(Boolean).join('  ·  ')}
              />
              <LegacyList
                title="Deployments"
                items={deployments}
                getLabel={item =>
                  [item.environment, item.releaseVersion, item.buildStatus]
                    .filter(Boolean).join('  ·  ')}
              />
              <LegacyList
                title="Releases"
                items={releases}
                getLabel={item =>
                  [item.version, item.repositoryName]
                    .filter(Boolean).join('  ·  ')}
              />
              <LegacyList
                title="Jira Issues"
                items={jiraIssues}
                getLabel={item =>
                  [item.issueKey, item.status, item.summary]
                    .filter(Boolean).join('  ·  ')}
              />
            </div>
          )}

          {/* Fallback: nothing to show in body */}
          {evidence.length === 0 && !hasSomeLegacyData(model) && !summary && (
            <div style={s.unavailableNotice}>
              <span>ℹ️</span>
              <span>{message || 'No engineering context details available.'}</span>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default EngineeringContextPanel;
