/**
 * Context Summary Card Component
 * Displays overall summary of retrieved engineering context
 * Shows confidence score and top recommendation
 */

import React from 'react';

const styles = {
  card: {
    background: '#ffffff',
    border: '1px solid #e5e7eb',
    borderRadius: 8,
    padding: 20,
    marginBottom: 24,
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 16,
  },
  title: {
    fontSize: 18,
    fontWeight: 600,
    color: '#1f2937',
  },
  summary: {
    fontSize: 14,
    color: '#4b5563',
    lineHeight: 1.6,
    marginBottom: 16,
  },
  confidenceSection: {
    display: 'flex',
    alignItems: 'center',
    gap: 16,
    padding: 12,
    background: '#f9fafb',
    borderRadius: 6,
    marginBottom: 16,
  },
  confidenceLabel: {
    fontSize: 13,
    fontWeight: 600,
    color: '#374151',
  },
  confidenceValue: {
    fontSize: 24,
    fontWeight: 700,
    color: '#10b981',
  },
  progressBar: {
    flex: 1,
    height: 8,
    background: '#e5e7eb',
    borderRadius: 4,
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    background: '#10b981',
    borderRadius: 4,
    transition: 'width 0.5s ease',
  },
  recommendationSection: {
    padding: 12,
    background: '#eff6ff',
    borderRadius: 6,
    border: '1px solid #bfdbfe',
  },
  recommendationLabel: {
    fontSize: 12,
    fontWeight: 600,
    color: '#1e40af',
    marginBottom: 4,
    textTransform: 'uppercase',
  },
  recommendationText: {
    fontSize: 14,
    color: '#1e3a8a',
    fontWeight: 500,
  },
  statsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(100px, 1fr))',
    gap: 12,
    marginTop: 16,
  },
  statItem: {
    textAlign: 'center',
    padding: 8,
    background: '#f9fafb',
    borderRadius: 4,
  },
  statValue: {
    fontSize: 18,
    fontWeight: 700,
    color: '#1f2937',
  },
  statLabel: {
    fontSize: 11,
    color: '#6b7280',
    textTransform: 'uppercase',
    marginTop: 2,
  },
};

const ContextSummaryCard = ({ contextData }) => {
  if (!contextData) {
    return null;
  }

  const topRecommendation = contextData.getTopRecommendation?.() || contextData.evidence?.[0];
  const confidence = contextData.metadata?.getAverageConfidence?.() || 0;
  const totalCandidates = contextData.metadata?.totalCandidates || 0;
  const filteredResults = contextData.metadata?.filteredResults || contextData.evidence?.length || 0;
  const retrievalTime = contextData.metadata?.retrievalTimeMs || 0;

  const getConfidenceColor = (score) => {
    if (score >= 80) return '#10b981';
    if (score >= 60) return '#f59e0b';
    return '#ef4444';
  };

  const formatTime = (ms) => {
    if (ms < 1000) return `${ms}ms`;
    return `${(ms / 1000).toFixed(1)}s`;
  };

  return (
    <div style={styles.card}>
      {/* Header */}
      <div style={styles.header}>
        <div style={styles.title}>Context Summary</div>
        <div style={{ fontSize: 12, color: '#6b7280' }}>
          {formatTime(retrievalTime)} search time
        </div>
      </div>

      {/* Summary Text */}
      <div style={styles.summary}>
        {contextData.summary || `Found ${filteredResults} highly relevant engineering artifacts.`}
      </div>

      {/* Confidence Score */}
      <div style={styles.confidenceSection}>
        <div style={styles.confidenceLabel}>Confidence</div>
        <div style={{ ...styles.confidenceValue, color: getConfidenceColor(confidence) }}>
          {confidence}%
        </div>
        <div style={styles.progressBar}>
          <div
            style={{
              ...styles.progressFill,
              width: `${confidence}%`,
              background: getConfidenceColor(confidence),
            }}
          />
        </div>
      </div>

      {/* Top Recommendation */}
      {topRecommendation && (
        <div style={styles.recommendationSection}>
          <div style={styles.recommendationLabel}>Top Recommendation</div>
          <div style={styles.recommendationText}>
            {topRecommendation.title}: {topRecommendation.summary}
          </div>
        </div>
      )}

      {/* Statistics */}
      <div style={styles.statsGrid}>
        <div style={styles.statItem}>
          <div style={styles.statValue}>{totalCandidates}</div>
          <div style={styles.statLabel}>Candidates</div>
        </div>
        <div style={styles.statItem}>
          <div style={styles.statValue}>{filteredResults}</div>
          <div style={styles.statLabel}>Results</div>
        </div>
        <div style={styles.statItem}>
          <div style={styles.statValue}>{contextData.evidence?.length || 0}</div>
          <div style={styles.statLabel}>Evidence</div>
        </div>
        <div style={styles.statItem}>
          <div style={styles.statValue}>{formatTime(retrievalTime)}</div>
          <div style={styles.statLabel}>Time</div>
        </div>
      </div>
    </div>
  );
};

export default ContextSummaryCard;