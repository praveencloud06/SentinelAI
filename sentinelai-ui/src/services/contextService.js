/**
 * Context Service
 * Handles API communication with SentinelAI Core (Backend-for-Frontend)
 * 
 * Architecture:
 *   UI → Core (localhost:8080) → Knowledge Service (localhost:8090)
 * 
 * All requests go through the Core API which acts as a unified backend
 * entry point. This provides:
 *   - Single backend endpoint for UI (no direct microservice calls)
 *   - Centralized authentication and authorization
 *   - No CORS issues (uses package.json proxy)
 *   - Consistent logging and request validation
 *   - Future caching can be implemented in Core
 *   - UI remains decoupled from internal microservice architecture
 */

import {
  ContextRetrievalRequest,
  ContextRetrievalResponse,
  ExplorerRequest,
  ContextError,
  SearchHistoryItem,
} from '../types/context/context.types';

// ============================================================================
// Configuration
// ============================================================================

// Use relative URL to leverage the proxy configured in package.json.
// package.json has "proxy": "http://localhost:8080" which routes all
// /api/* requests to SentinelAI Core.
//
// IMPORTANT: Do NOT use direct URLs like 'http://localhost:8090'.
// All requests must go through Core following the BFF pattern.
const CORE_API_BASE_URL = process.env.REACT_APP_API_BASE_URL || '';

const API_TIMEOUT = 30000; // 30 seconds

// ============================================================================
// Service Class
// ============================================================================

class ContextService {
  constructor() {
    this.baseUrl = CORE_API_BASE_URL;
    this.timeout = API_TIMEOUT;
  }

  /**
   * Generic fetch with timeout and error handling
   */
  async fetchWithTimeout(url, options = {}) {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), this.timeout);

    try {
      const response = await fetch(url, {
        ...options,
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      if (!response.ok) {
        const errorData = await this.parseErrorResponse(response);
        throw this.createErrorFromResponse(response, errorData);
      }

      return response;
    } catch (error) {
      clearTimeout(timeoutId);

      if (error.name === 'AbortError') {
        throw ContextError.timeout();
      }

      if (error instanceof ContextError) {
        throw error;
      }

      throw new ContextError({
        message: error.message || 'Network error occurred',
        code: 'NETWORK_ERROR',
        retryable: true,
      });
    }
  }

  /**
   * Parse error response
   */
  async parseErrorResponse(response) {
    try {
      const text = await response.text();
      try {
        return JSON.parse(text);
      } catch {
        return { message: text };
      }
    } catch {
      return { message: response.statusText };
    }
  }

  /**
   * Create ContextError from HTTP response
   */
  createErrorFromResponse(response, errorData) {
    const status = response.status;
    const message = errorData.message || errorData.error || response.statusText;

    if (status === 404) {
      return ContextError.noContextFound();
    }

    if (status === 503 || status === 502 || status === 504) {
      return ContextError.serviceUnavailable();
    }

    if (status === 400 || status === 422) {
      return ContextError.validation(message);
    }

    return new ContextError({
      message: message || 'Request failed',
      code: `HTTP_${status}`,
      retryable: status >= 500,
    });
  }

  // ============================================================================
  // Context Retrieval API
  // ============================================================================

  /**
   * Retrieve engineering context from logs
   * POST /api/context/retrieve
   * 
   * This calls Core's ContextController which proxies to Knowledge Service.
   * 
   * Flow: UI → Core /api/context/retrieve → Knowledge Service /api/context/retrieve
   */
  async retrieveEngineeringContext(request) {
    try {
      const validatedRequest = this.validateContextRequest(request);
      
      const response = await this.fetchWithTimeout(
        `${this.baseUrl}/api/context/retrieve`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(validatedRequest),
        }
      );

      const data = await response.json();
      return new ContextRetrievalResponse(data);
    } catch (error) {
      console.error('Context retrieval failed:', error);
      throw error;
    }
  }

  /**
   * Search by natural language question
   * POST /api/context/explorer
   * 
   * Note: This endpoint is not yet implemented in Core.
   * For now, it falls back to the same retrieve endpoint.
   * Future: Core should expose a separate /api/context/explorer endpoint.
   */
  async searchByQuestion(request) {
    try {
      const validatedRequest = this.validateExplorerRequest(request);
      
      // Convert explorer request to context retrieval request
      // since Core doesn't have a separate explorer endpoint yet
      const contextRequest = {
        logs: validatedRequest.query,
        maxResults: validatedRequest.maxResults || 10,
        minScore: 0.3,
        includeHistoricalIncidents: true,
        includeRunbooks: true,
        includeArchitectureDocs: true,
      };
      
      const response = await this.fetchWithTimeout(
        `${this.baseUrl}/api/context/retrieve`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(contextRequest),
        }
      );

      const data = await response.json();
      return new ContextRetrievalResponse(data);
    } catch (error) {
      console.error('Explorer search failed:', error);
      throw error;
    }
  }

  // ============================================================================
  // Search History API
  // ============================================================================

  /**
   * Get search history
   * GET /api/context/history
   * 
   * Note: This endpoint is not yet implemented.
   * Returns empty array as graceful fallback.
   */
  async getSearchHistory() {
    try {
      const response = await this.fetchWithTimeout(
        `${this.baseUrl}/api/context/history`,
        {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      const data = await response.json();
      return data.map(item => new SearchHistoryItem(item));
    } catch (error) {
      console.error('Failed to fetch search history:', error);
      // Return empty array on error (non-critical feature)
      return [];
    }
  }

  /**
   * Save search to history
   * POST /api/context/history
   * 
   * Note: This endpoint is not yet implemented.
   * Silently fails as this is a non-critical feature.
   */
  async saveSearchToHistory(searchItem) {
    try {
      const response = await this.fetchWithTimeout(
        `${this.baseUrl}/api/context/history`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(searchItem),
        }
      );

      return await response.json();
    } catch (error) {
      console.error('Failed to save search history:', error);
      // Silently fail (non-critical feature)
      return null;
    }
  }

  /**
   * Clear search history
   * DELETE /api/context/history
   * 
   * Note: This endpoint is not yet implemented.
   * Silently fails as this is a non-critical feature.
   */
  async clearSearchHistory() {
    try {
      const response = await this.fetchWithTimeout(
        `${this.baseUrl}/api/context/history`,
        {
          method: 'DELETE',
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      return await response.json();
    } catch (error) {
      console.error('Failed to clear search history:', error);
      // Silently fail (non-critical feature)
      return null;
    }
  }

  // ============================================================================
  // Validation
  // ============================================================================

  /**
   * Validate context retrieval request
   */
  validateContextRequest(request) {
    const req = request instanceof ContextRetrievalRequest 
      ? request 
      : new ContextRetrievalRequest(request);

    if (!req.logs || req.logs.trim().length === 0) {
      throw ContextError.validation('Logs are required');
    }

    if (req.logs.length > 100000) {
      throw ContextError.validation('Logs exceed maximum length (100,000 characters)');
    }

    if (req.maxResults < 1 || req.maxResults > 50) {
      throw ContextError.validation('maxResults must be between 1 and 50');
    }

    if (req.minScore < 0 || req.minScore > 1) {
      throw ContextError.validation('minScore must be between 0 and 1');
    }

    return req;
  }

  /**
   * Validate explorer request
   */
  validateExplorerRequest(request) {
    const req = request instanceof ExplorerRequest 
      ? request 
      : new ExplorerRequest(request);

    if (!req.query || req.query.trim().length === 0) {
      throw ContextError.validation('Query is required');
    }

    if (req.query.length > 500) {
      throw ContextError.validation('Query exceeds maximum length (500 characters)');
    }

    if (req.maxResults < 1 || req.maxResults > 100) {
      throw ContextError.validation('maxResults must be between 1 and 100');
    }

    return req;
  }

  // ============================================================================
  // Health Check
  // ============================================================================

  /**
   * Check if Core API is available
   * This checks Core's health, not Knowledge Service directly.
   */
  async healthCheck() {
    try {
      const response = await this.fetchWithTimeout(
        `${this.baseUrl}/actuator/health`,
        {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      const data = await response.json();
      return data.status === 'UP';
    } catch (error) {
      console.error('Health check failed:', error);
      return false;
    }
  }

  /**
   * Get Core API service info
   */
  async getServiceInfo() {
    try {
      const response = await this.fetchWithTimeout(
        `${this.baseUrl}/actuator/info`,
        {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      return await response.json();
    } catch (error) {
      console.error('Failed to get service info:', error);
      return null;
    }
  }
}

// ============================================================================
// Export singleton instance
// ============================================================================

export const contextService = new ContextService();
export default contextService;