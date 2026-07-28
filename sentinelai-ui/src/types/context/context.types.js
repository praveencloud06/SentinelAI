/**
 * Type definitions for Engineering Context Explorer
 * These correspond to the Knowledge Service V2 API contracts
 */

// ============================================================================
// Source System Types
// ============================================================================

export const SourceSystem = {
  GITHUB: 'GITHUB',
  GITLAB: 'GITLAB',
  JIRA: 'JIRA',
  CONFLUENCE: 'CONFLUENCE',
  JENKINS: 'JENKINS',
  SERVICENOW: 'SERVICENOW',
  AZURE_DEVOPS: 'AZURE_DEVOPS',
  SLACK: 'SLACK',
  DATADOG: 'DATADOG',
  GRAFANA: 'GRAFANA',
};

export const EvidenceType = {
  COMMIT: 'COMMIT',
  PULL_REQUEST: 'PULL_REQUEST',
  RELEASE: 'RELEASE',
  JIRA_ISSUE: 'JIRA_ISSUE',
  CONFLUENCE_DOCUMENT: 'CONFLUENCE_DOCUMENT',
  DEPLOYMENT: 'DEPLOYMENT',
  RUNBOOK: 'RUNBOOK',
  INCIDENT: 'INCIDENT',
};

// ============================================================================
// API Request Types
// ============================================================================

/**
 * Context Retrieval Request
 * Corresponds to POST /api/context/retrieve
 */
export class ContextRetrievalRequest {
  constructor({
    logs = '',
    service = null,
    environment = null,
    repository = null,
    deployment = null,
    commit = null,
    time = null,
    maxResults = 10,
    minScore = 0.3,
    includeHistoricalIncidents = true,
    includeRunbooks = true,
    includeArchitectureDocs = true,
  } = {}) {
    this.logs = logs;
    this.service = service;
    this.environment = environment;
    this.repository = repository;
    this.deployment = deployment;
    this.commit = commit;
    this.time = time;
    this.maxResults = maxResults;
    this.minScore = minScore;
    this.includeHistoricalIncidents = includeHistoricalIncidents;
    this.includeRunbooks = includeRunbooks;
    this.includeArchitectureDocs = includeArchitectureDocs;
  }
}

/**
 * Engineering Context Explorer Request
 * Corresponds to POST /api/context/explorer
 */
export class ExplorerRequest {
  constructor({
    query = '',
    filters = {},
    searchType = 'SEMANTIC',
    maxResults = 20,
  } = {}) {
    this.query = query;
    this.filters = filters;
    this.searchType = searchType;
    this.maxResults = maxResults;
  }
}

// ============================================================================
// API Response Types
// ============================================================================

/**
 * Engineering Evidence
 * Unified model for all engineering evidence types
 */
export class EngineeringEvidence {
  constructor({
    type = '',
    title = '',
    summary = '',
    score = 0,
    reason = '',
    metadata = {},
    sourceSystem = '',
    sourceId = '',
    link = '',
    vectorSimilarityScore = null,
    relationshipScore = null,
    timelineScore = null,
    serviceMatchScore = null,
    environmentMatchScore = null,
    recencyScore = null,
    exactMatchScore = null,
  } = {}) {
    this.type = type;
    this.title = title;
    this.summary = summary;
    this.score = score;
    this.reason = reason;
    this.metadata = metadata;
    this.sourceSystem = sourceSystem;
    this.sourceId = sourceId;
    this.link = link;
    this.vectorSimilarityScore = vectorSimilarityScore;
    this.relationshipScore = relationshipScore;
    this.timelineScore = timelineScore;
    this.serviceMatchScore = serviceMatchScore;
    this.environmentMatchScore = environmentMatchScore;
    this.recencyScore = recencyScore;
    this.exactMatchScore = exactMatchScore;
  }

  /**
   * Get formatted score as percentage
   */
  getScorePercentage() {
    return Math.round(this.score * 100);
  }

  /**
   * Get score level (high, medium, low)
   */
  getScoreLevel() {
    if (this.score >= 0.8) return 'high';
    if (this.score >= 0.5) return 'medium';
    return 'low';
  }

  /**
   * Check if evidence has detailed signal scores
   */
  hasDetailedScores() {
    return this.vectorSimilarityScore !== null ||
           this.relationshipScore !== null ||
           this.timelineScore !== null;
  }
}

/**
 * Retrieval Metadata
 * Statistics about the context retrieval process
 */
export class RetrievalMetadata {
  constructor({
    totalCandidates = 0,
    filteredResults = 0,
    retrievalTimeMs = 0,
    signalWeights = {},
    queryEmbeddingModel = '',
    resultsByType = {},
  } = {}) {
    this.totalCandidates = totalCandidates;
    this.filteredResults = filteredResults;
    this.retrievalTimeMs = retrievalTimeMs;
    this.signalWeights = signalWeights;
    this.queryEmbeddingModel = queryEmbeddingModel;
    this.resultsByType = resultsByType;
  }

  /**
   * Get average confidence score
   */
  getAverageConfidence() {
    const total = Object.values(this.resultsByType).reduce((sum, count) => sum + count, 0);
    if (total === 0) return 0;
    return Math.min(100, Math.round((this.filteredResults / this.totalCandidates) * 100));
  }
}

/**
 * Context Retrieval Response
 * Main response from context retrieval API
 */
export class ContextRetrievalResponse {
  constructor({
    summary = '',
    evidence = [],
    metadata = null,
  } = {}) {
    this.summary = summary;
    this.evidence = evidence.map(e => new EngineeringEvidence(e));
    this.metadata = metadata ? new RetrievalMetadata(metadata) : null;
  }

  /**
   * Get top recommendation
   */
  getTopRecommendation() {
    return this.evidence.length > 0 ? this.evidence[0] : null;
  }

  /**
   * Get evidence by type
   */
  getEvidenceByType(type) {
    return this.evidence.filter(e => e.type === type);
  }

  /**
   * Get evidence by source system
   */
  getEvidenceBySource(sourceSystem) {
    return this.evidence.filter(e => e.sourceSystem === sourceSystem);
  }
}

// ============================================================================
// UI State Types
// ============================================================================

/**
 * Search Mode
 */
export const SearchMode = {
  LOGS: 'logs',
  NATURAL_LANGUAGE: 'natural-language',
};

/**
 * View Mode
 */
export const ViewMode = {
  GRID: 'grid',
  TIMELINE: 'timeline',
  GRAPH: 'graph',
};

/**
 * Filter State
 */
export class FilterState {
  constructor({
    service = '',
    environment = '',
    repository = '',
    dateRange = { from: null, to: null },
    sourceSystems = [],
    minScore = 0.3,
  } = {}) {
    this.service = service;
    this.environment = environment;
    this.repository = repository;
    this.dateRange = dateRange;
    this.sourceSystems = sourceSystems;
    this.minScore = minScore;
  }

  /**
   * Check if filters are active
   */
  hasActiveFilters() {
    return this.service !== '' ||
           this.environment !== '' ||
           this.repository !== '' ||
           this.sourceSystems.length > 0 ||
           this.minScore > 0.3;
  }

  /**
   * Reset all filters
   */
  reset() {
    return new FilterState();
  }
}

/**
 * Search History Item
 */
export class SearchHistoryItem {
  constructor({
    id = '',
    query = '',
    mode = SearchMode.LOGS,
    filters = new FilterState(),
    timestamp = new Date(),
    resultCount = 0,
  } = {}) {
    this.id = id;
    this.query = query;
    this.mode = mode;
    this.filters = filters;
    this.timestamp = timestamp;
    this.resultCount = resultCount;
  }

  /**
   * Get formatted timestamp
   */
  getFormattedTimestamp() {
    return new Intl.DateTimeFormat('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(this.timestamp);
  }
}

// ============================================================================
// Loading State Types
// ============================================================================

/**
 * Loading Step
 */
export const LoadingStep = {
  INITIALIZING: 'Initializing search...',
  SEARCHING_METADATA: 'Searching engineering metadata...',
  SEARCHING_RELATIONSHIPS: 'Analyzing relationships...',
  SEARCHING_SIMILAR_KNOWLEDGE: 'Finding similar knowledge...',
  RANKING_RESULTS: 'Ranking engineering evidence...',
  PREPARING_CONTEXT: 'Preparing engineering context...',
};

// ============================================================================
// Error Types
// ============================================================================

/**
 * Context Error
 */
export class ContextError extends Error {
  constructor({
    message = 'An error occurred',
    code = 'UNKNOWN_ERROR',
    details = null,
    retryable = false,
  } = {}) {
    super(message);
    this.name = 'ContextError';
    this.code = code;
    this.details = details;
    this.retryable = retryable;
  }

  /**
   * Create no context found error
   */
  static noContextFound() {
    return new ContextError({
      message: 'No relevant engineering context found',
      code: 'NO_CONTEXT_FOUND',
      retryable: false,
    });
  }

  /**
   * Create service unavailable error
   */
  static serviceUnavailable() {
    return new ContextError({
      message: 'Knowledge Service is currently unavailable',
      code: 'SERVICE_UNAVAILABLE',
      retryable: true,
    });
  }

  /**
   * Create timeout error
   */
  static timeout() {
    return new ContextError({
      message: 'Request timed out. Please try again.',
      code: 'TIMEOUT',
      retryable: true,
    });
  }

  /**
   * Create validation error
   */
  static validation(message) {
    return new ContextError({
      message: message || 'Invalid input',
      code: 'VALIDATION_ERROR',
      retryable: false,
    });
  }
}

// ============================================================================
// Timeline Types
// ============================================================================

/**
 * Timeline Event
 */
export class TimelineEvent {
  constructor({
    id = '',
    type = '',
    title = '',
    timestamp = new Date(),
    sourceSystem = '',
    metadata = {},
  } = {}) {
    this.id = id;
    this.type = type;
    this.title = title;
    this.timestamp = timestamp;
    this.sourceSystem = sourceSystem;
    this.metadata = metadata;
  }

  /**
   * Get formatted timestamp
   */
  getFormattedTimestamp() {
    return new Intl.DateTimeFormat('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(this.timestamp);
  }
}

// ============================================================================
// Relationship Graph Types
// ============================================================================

/**
 * Graph Node
 */
export class GraphNode {
  constructor({
    id = '',
    label = '',
    type = '',
    sourceSystem = '',
    metadata = {},
  } = {}) {
    this.id = id;
    this.label = label;
    this.type = type;
    this.sourceSystem = sourceSystem;
    this.metadata = metadata;
  }
}

/**
 * Graph Edge
 */
export class GraphEdge {
  constructor({
    id = '',
    source = '',
    target = '',
    relationshipType = '',
    strength = 0,
  } = {}) {
    this.id = id;
    this.source = source;
    this.target = target;
    this.relationshipType = relationshipType;
    this.strength = strength;
  }
}

/**
 * Relationship Graph
 */
export class RelationshipGraph {
  constructor({
    nodes = [],
    edges = [],
  } = {}) {
    this.nodes = nodes.map(n => new GraphNode(n));
    this.edges = edges.map(e => new GraphEdge(e));
  }

  /**
   * Get nodes by type
   */
  getNodesByType(type) {
    return this.nodes.filter(n => n.type === type);
  }

  /**
   * Get connected nodes
   */
  getConnectedNodes(nodeId) {
    const connectedIds = this.edges
      .filter(e => e.source === nodeId || e.target === nodeId)
      .map(e => e.source === nodeId ? e.target : e.source);
    
    return this.nodes.filter(n => connectedIds.includes(n.id));
  }
}