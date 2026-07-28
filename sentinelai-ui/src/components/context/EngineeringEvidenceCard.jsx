/**
 * Engineering Evidence Card Component
 * Displays a single piece of engineering evidence with explainable AI features
 * Shows why the evidence was selected with detailed signal scores
 */

import React, { useState } from 'react';

const styles = {
  card: {
    background: '#ffffff',
    border: '1px solid #e5e7eb',
    borderRadius: 8,
    padding: 16,
    marginBottom: 16,
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
    transition: 'box-shadow 0.2s ease, transform 0.2s ease',
    cursor: 'pointer',
  },
  cardHover: {
    boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
    transform: 'translateY(-2px)',
  },
  header: {
    display: 'flex',
    alignItems: 'flex-start',
    gap: 12,
    marginBottom: 12,
  },
  icon: {
    width: 40,
    height: 40,
    borderRadius: 8,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: 20,
    flexShrink: 0,
  },
  iconGitHub: { background: '#24292e', color: '#ffffff' },
  iconJira: { background: '#0052cc', color: '#ffffff' },
  iconConfluence: { background: '#0052cc', color: '#ffffff' },
  iconJenkins: { background: '#d33833', color: '#ffffff' },
  iconDeployment: { background: '#8b5cf6', color: '#ffffff' },
  iconCommit: { background: '#10b981', color: '#ffffff' },
  iconRunbook: { background: '#f59e0b', color: '#ffffff' },
  iconDefault: { background: '#6b7280', color: '#ffffff' },

  headerContent: {
    flex: 1,
    minWidth: 0,
  },
  sourceBadge: {
    display: 'inline-block',
    padding: '2px 8px',
    borderRadius: 4,
    fontSize: 11,
    fontWeight: 600,
    textTransform: 'uppercase',
    marginBottom: 4,
  },
  title: {
    fontSize: 15,
    fontWeight: 600,
    color: '#1f2937',
    marginBottom: 4,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  summary: {
    fontSize: 13,
    color: '#6b7280',
    lineHeight: 1.4,
    display: '-webkit-box',
    WebkitLineClamp: 2,
    WebkitBoxOrient: 'vertical',
    overflow: 'hidden',
  },
  scoreSection: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: 12,
    paddingTop: 12,
    borderTop: '1px solid #f3f4f6',
  },
  scoreBadge: {
    display: 'flex',
    alignItems: 'center',
    gap: 6,
  },
  scoreValue: {
    fontSize: 18,
    fontWeight: 700,
  },
  scoreLabel: {
    fontSize: 12,
    color: '#6b7280',
  },
  linkButton: {
    padding: '6px 12px',
    background: '#f3f4f6',
    border: 'none',
    borderRadius: 4,
    fontSize: 12,
    fontWeight: 500,
    color: '#374151',
    cursor: 'pointer',
    textDecoration: 'none',
    display: 'inline-flex',
    alignItems: 'center',
    gap: 4,
  },
  linkButtonHover: {
    background: '#e5e7eb',
  },
  reasonSection: {
    marginTop: 12,
    padding: 12,
    background: '#f9fafb',
    borderRadius: 6,
    border: '1px solid #e5e7eb',
  },
  reasonTitle: {
    fontSize: 12,
    fontWeight: 600,
    color: '#374151',
    marginBottom: 8,
    display: 'flex',
    alignItems: 'center',
    gap: 6,
  },
  reasonList: {
    margin: 0,
    paddingLeft: 16,
    fontSize: 12,
    color: '#4b5563',
  },
  reasonItem: {
    marginBottom: 4,
  },
  signalScores: {
    marginTop: 12,
    padding: 12,
    background: '#f0f9ff',
    borderRadius: 6,
    border: '1px solid #bae6fd',
  },
  signalTitle: {
    fontSize: 12,
    fontWeight: 600,
    color: '#1e40af',
    marginBottom: 8,
  },
  signalGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))',
    gap: 8,
  },
  signalItem: {
    display: 'flex',
    flexDirection: 'column',
    gap: 2,
  },
  signalLabel: {
    fontSize: 10,
    color: '#6b7280',
    textTransform: 'uppercase',
  },
  signalValue: {
    fontSize: 14,
    fontWeight: 600,
    color: '#1e40af',
  },
  expandButton: {
    width: '100%',
    padding: '8px',
    background: 'none',
    border: 'none',
    borderTop: '1px solid #f3f4f6',
    fontSize: 12,
    color: '#6b7280',
    cursor: 'pointer',
    marginTop: 8,
  },
  expandButtonHover: {
    background: '#f9fafb',
    color: '#374151',
  },
};

const EngineeringEvidenceCard = ({ evidence, onExpand }) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const [isHovered, setIsHovered] = useState(false);

  const getIconForSource = (sourceSystem) => {
    const icons = {
      GITHUB: '📦',
      GITLAB: '🦊',
      JIRA: '🎫',
      CONFLUENCE: '📄',
      JENKINS: '⚙️',
      DEPLOYMENT: '🚀',
      COMMIT: '💻',
      RUNBOOK: '📖',
    };
    return icons[sourceSystem] || '📋';
  };

  const getIconStyle = (sourceSystem) => {
    const styleMap = {
      GITHUB: styles.iconGitHub,
      GITLAB: styles.iconGitHub,
      JIRA: styles.iconJira,
      CONFLUENCE: styles.iconConfluence,
      JENKINS: styles.iconJenkins,
      DEPLOYMENT: styles.iconDeployment,
      COMMIT: styles.iconCommit,
      RUNBOOK: styles.iconRunbook,
    };
    return styleMap[sourceSystem] || styles.iconDefault;
  };

  const getSourceColor = (sourceSystem) => {
    const colorMap = {
      GITHUB: '#24292e',
      GITLAB: '#fc6d26',
      JIRA: '#0052cc',
      CONFLUENCE: '#0052cc',
      JENKINS: '#d33833',
      DEPLOYMENT: '#8b5cf6',
      COMMIT: '#10b981',
      RUNBOOK: '#f59e0b',
    };
    return colorMap[sourceSystem] || '#6b7280';
  };

  const getScoreColor = (score) => {
    if (score >= 0.8) return '#10b981';
    if (score >= 0.5) return '#f59e0b';
    return '#ef4444';
  };

  const formatReason = (reason) => {
    if (!reason) return [];
    // Split reason by common delimiters
    return reason.split(/[•·\n]/).filter(r => r.trim());
  };

  const handleCardClick = () => {
    if (onExpand) {
      onExpand(evidence);
    }
    setIsExpanded(!isExpanded);
  };

  const icon = getIconForSource(evidence.sourceSystem);
  const iconStyle = getIconStyle(evidence.sourceSystem);
  const sourceColor = getSourceColor(evidence.sourceSystem);
  const scoreColor = getScoreColor(evidence.score);
  const reasons = formatReason(evidence.reason);

  return (
    <div
      style={{
        ...styles.card,
        ...(isHovered ? styles.cardHover : {}),
      }}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      {/* Header */}
      <div style={styles.header}>
        <div style={{ ...styles.icon, ...iconStyle }}>
          {icon}
        </div>
        <div style={styles.headerContent}>
          <div style={{ ...styles.sourceBadge, background: `${sourceColor}15`, color: sourceColor }}>
            {evidence.sourceSystem}
          </div>
          <div style={styles.title}>{evidence.title}</div>
          <div style={styles.summary}>{evidence.summary}</div>
        </div>
      </div>

      {/* Score Section */}
      <div style={styles.scoreSection}>
        <div style={styles.scoreBadge}>
          <span style={{ ...styles.scoreValue, color: scoreColor }}>
            {evidence.getScorePercentage ? evidence.getScorePercentage() : Math.round(evidence.score * 100)}%
          </span>
          <span style={styles.scoreLabel}>Relevance</span>
        </div>
        
        {evidence.link && (
          <a
            href={evidence.link}
            target="_blank"
            rel="noopener noreferrer"
            style={styles.linkButton}
            onClick={(e) => e.stopPropagation()}
          >
            View Source →
          </a>
        )}
      </div>

      {/* Expandable Reason Section */}
      {isExpanded && (
        <>
          {/* Why was this selected */}
          {reasons.length > 0 && (
            <div style={styles.reasonSection}>
              <div style={styles.reasonTitle}>
                <span>✔</span>
                <span>Why was this selected?</span>
              </div>
              <ul style={styles.reasonList}>
                {reasons.map((reason, index) => (
                  <li key={index} style={styles.reasonItem}>
                    {reason.trim()}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* Detailed Signal Scores */}
          {evidence.hasDetailedScores && evidence.hasDetailedScores() && (
            <div style={styles.signalScores}>
              <div style={styles.signalTitle}>Signal Breakdown</div>
              <div style={styles.signalGrid}>
                {evidence.vectorSimilarityScore !== null && (
                  <div style={styles.signalItem}>
                    <span style={styles.signalLabel}>Vector Similarity</span>
                    <span style={styles.signalValue}>
                      {Math.round(evidence.vectorSimilarityScore * 100)}%
                    </span>
                  </div>
                )}
                {evidence.relationshipScore !== null && (
                  <div style={styles.signalItem}>
                    <span style={styles.signalLabel}>Relationship</span>
                    <span style={styles.signalValue}>
                      {Math.round(evidence.relationshipScore * 100)}%
                    </span>
                  </div>
                )}
                {evidence.timelineScore !== null && (
                  <div style={styles.signalItem}>
                    <span style={styles.signalLabel}>Timeline</span>
                    <span style={styles.signalValue}>
                      {Math.round(evidence.timelineScore * 100)}%
                    </span>
                  </div>
                )}
                {evidence.serviceMatchScore !== null && (
                  <div style={styles.signalItem}>
                    <span style={styles.signalLabel}>Service Match</span>
                    <span style={styles.signalValue}>
                      {Math.round(evidence.serviceMatchScore * 100)}%
                    </span>
                  </div>
                )}
                {evidence.environmentMatchScore !== null && (
                  <div style={styles.signalItem}>
                    <span style={styles.signalLabel}>Environment</span>
                    <span style={styles.signalValue}>
                      {Math.round(evidence.environmentMatchScore * 100)}%
                    </span>
                  </div>
                )}
                {evidence.recencyScore !== null && (
                  <div style={styles.signalItem}>
                    <span style={styles.signalLabel}>Recency</span>
                    <span style={styles.signalValue}>
                      {Math.round(evidence.recencyScore * 100)}%
                    </span>
                  </div>
                )}
              </div>
            </div>
          )}
        </>
      )}

      {/* Expand Button */}
      {(reasons.length > 0 || (evidence.hasDetailedScores && evidence.hasDetailedScores())) && (
        <button
          style={{
            ...styles.expandButton,
            ...(isHovered ? styles.expandButtonHover : {}),
          }}
          onClick={handleCardClick}
        >
          {isExpanded ? 'Show Less' : 'Show Details'}
        </button>
      )}
    </div>
  );
};

export default EngineeringEvidenceCard;