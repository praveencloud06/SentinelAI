/**
 * Engineering Context Explorer Page
 * Main page for discovering engineering knowledge related to logs or natural language queries
 * Preserves existing Log RCA and ELK Investigation functionality
 */

import React, { useState } from 'react';
import SearchPanel from '../../components/context/SearchPanel';
import LoadingSteps from '../../components/context/LoadingSteps';
import EngineeringContextPanel from '../../components/context/EngineeringContextPanel';
import { useContextRetrieval } from '../../hooks/context/useContextRetrieval';
import { useSearchHistory } from '../../hooks/context/useSearchHistory';
import { SearchMode, ContextRetrievalRequest } from '../../types/context/context.types';

const styles = {
  page: {
    maxWidth: 1200,
    margin: '0 auto',
    padding: '24px 16px',
    fontFamily: "'Segoe UI', Arial, sans-serif",
  },
  header: {
    marginBottom: 32,
  },
  title: {
    fontSize: 28,
    fontWeight: 700,
    color: '#1f2937',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    color: '#6b7280',
    lineHeight: 1.5,
  },
  content: {
    display: 'grid',
    gridTemplateColumns: '1fr',
    gap: 24,
  },
  errorCard: {
    background: '#fef2f2',
    border: '1px solid #fecaca',
    borderRadius: 8,
    padding: 16,
    marginBottom: 24,
  },
  errorTitle: {
    fontSize: 16,
    fontWeight: 600,
    color: '#dc2626',
    marginBottom: 8,
  },
  errorMessage: {
    fontSize: 14,
    color: '#991b1b',
    marginBottom: 12,
  },
  retryButton: {
    padding: '8px 16px',
    background: '#dc2626',
    color: '#ffffff',
    border: 'none',
    borderRadius: 6,
    fontSize: 14,
    fontWeight: 500,
    cursor: 'pointer',
  },
  noResultsCard: {
    background: '#f9fafb',
    border: '1px solid #e5e7eb',
    borderRadius: 8,
    padding: 32,
    textAlign: 'center',
  },
  noResultsTitle: {
    fontSize: 18,
    fontWeight: 600,
    color: '#374151',
    marginBottom: 8,
  },
  noResultsMessage: {
    fontSize: 14,
    color: '#6b7280',
  },
  evidenceSection: {
    marginTop: 24,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: 600,
    color: '#1f2937',
    marginBottom: 16,
  },
  evidenceGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))',
    gap: 16,
  },
  emptyState: {
    textAlign: 'center',
    padding: '48px 24px',
    color: '#9ca3af',
  },
  emptyStateIcon: {
    fontSize: 48,
    marginBottom: 16,
  },
  loadingContainer: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    minHeight: '200px',
  },
};

const EngineeringContextExplorer = () => {
  const [searchMode, setSearchMode] = useState(SearchMode.LOGS);
  const [lastSearchRequest, setLastSearchRequest] = useState(null);
  
  const {
    contextData,
    loading,
    error,
    currentStep,
    retrieveContext,
    retry,
    reset,
    isRetryable,
  } = useContextRetrieval();

  const {
    history,
    addToHistory,
  } = useSearchHistory();

  const handleSearchModeChange = (mode) => {
    setSearchMode(mode);
    reset();
  };

  const handleSearch = async ({ logs, mode, query }) => {
    try {
      const request = new ContextRetrievalRequest({
        logs: logs,
        maxResults: 10,
        minScore: 0.3,
        includeHistoricalIncidents: true,
        includeRunbooks: true,
        includeArchitectureDocs: true,
      });

      setLastSearchRequest(request);
      
      const result = await retrieveContext(request);
      
      // Add to search history
      await addToHistory(
        query || logs.substring(0, 100),
        mode,
        {},
        result?.evidence?.length || 0
      );
      
    } catch (err) {
      console.error('Search failed:', err);
    }
  };

  const handleRetry = () => {
    if (lastSearchRequest) {
      retry();
    }
  };

  const handleEvidenceExpand = (evidence) => {
    console.log('Expanded evidence:', evidence);
    // Could implement detailed view or modal here
  };

  return (
    <div style={styles.page}>
      {/* Header */}
      <div style={styles.header}>
        <h1 style={styles.title}>Engineering Context Explorer</h1>
        <p style={styles.subtitle}>
          Find the most relevant engineering knowledge related to logs, deployments, commits, 
          Jira issues, runbooks and previous incidents.
        </p>
      </div>

      {/* Search Panel */}
      <SearchPanel
        searchMode={searchMode}
        onSearchModeChange={handleSearchModeChange}
        onSearch={handleSearch}
        loading={loading}
      />

      {/* Loading State */}
      {loading && (
        <div style={styles.loadingContainer}>
          <LoadingSteps currentStep={currentStep} />
        </div>
      )}

      {/* Error State */}
      {error && !loading && (
        <div style={styles.errorCard}>
          <div style={styles.errorTitle}>Search Failed</div>
          <div style={styles.errorMessage}>{error.message}</div>
          {isRetryable && (
            <button style={styles.retryButton} onClick={handleRetry}>
              Try Again
            </button>
          )}
        </div>
      )}

      {/* Results — rendered via the shared EngineeringContextPanel */}
      {contextData && !loading && !error && (
        <div style={styles.content}>
          <EngineeringContextPanel
            engineeringContext={contextData}
            title="Engineering Context Results"
            defaultExpanded={true}
          />
        </div>
      )}

      {/* Empty State */}
      {!contextData && !loading && !error && (
        <div style={styles.emptyState}>
          <div style={styles.emptyStateIcon}>🔍</div>
          <div style={styles.noResultsTitle}>Start Your Search</div>
          <div style={styles.noResultsMessage}>
            Paste logs or ask a question to discover relevant engineering context
          </div>
        </div>
      )}
    </div>
  );
};

export default EngineeringContextExplorer;