/**
 * use Context Retrieval Hook
 * Main hook for engineering context retrieval with loading states and error handling
 */

import { useState, useCallback, useEffect } from 'react';
import { contextService } from '../../services/contextService';
import {
  ContextRetrievalRequest,
  ContextRetrievalResponse,
  ContextError,
  LoadingStep,
} from '../../types/context/context.types';

export const useContextRetrieval = () => {
  const [contextData, setContextData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [currentStep, setCurrentStep] = useState('');
  const [retryCount, setRetryCount] = useState(0);

  /**
   * Reset state
   */
  const reset = useCallback(() => {
    setContextData(null);
    setLoading(false);
    setError(null);
    setCurrentStep('');
    setRetryCount(0);
  }, []);

  /**
   * Simulate loading steps for better UX
   */
  const simulateLoadingSteps = async () => {
    const steps = [
      LoadingStep.INITIALIZING,
      LoadingStep.SEARCHING_METADATA,
      LoadingStep.SEARCHING_RELATIONSHIPS,
      LoadingStep.SEARCHING_SIMILAR_KNOWLEDGE,
      LoadingStep.RANKING_RESULTS,
      LoadingStep.PREPARING_CONTEXT,
    ];

    for (const step of steps) {
      setCurrentStep(step);
      await new Promise(resolve => setTimeout(resolve, 500));
    }
  };

  /**
   * Retrieve engineering context
   */
  const retrieveContext = useCallback(async (request) => {
    setLoading(true);
    setError(null);
    setCurrentStep(LoadingStep.INITIALIZING);

    try {
      // Start loading steps simulation
      const loadingStepsPromise = simulateLoadingSteps();

      // Make actual API call
      const contextRequest = request instanceof ContextRetrievalRequest
        ? request
        : new ContextRetrievalRequest(request);

      const [data] = await Promise.all([
        contextService.retrieveEngineeringContext(contextRequest),
        loadingStepsPromise,
      ]);

      setContextData(data);
      setRetryCount(0);
      
      return data;
    } catch (err) {
      console.error('Context retrieval failed:', err);
      
      if (err instanceof ContextError) {
        setError(err);
      } else {
        setError(new ContextError({
          message: err.message || 'An unexpected error occurred',
          code: 'UNKNOWN_ERROR',
          retryable: true,
        }));
      }

      throw err;
    } finally {
      setLoading(false);
      setCurrentStep('');
    }
  }, []);

  /**
   * Retry the last request
   */
  const retry = useCallback(async () => {
    if (!error || !error.retryable) {
      return;
    }

    setRetryCount(prev => prev + 1);
    
    if (retryCount >= 3) {
      setError(new ContextError({
        message: 'Maximum retry attempts reached',
        code: 'MAX_RETRIES',
        retryable: false,
      }));
      return;
    }

    // Retry with exponential backoff
    const backoffMs = Math.min(1000 * Math.pow(2, retryCount), 10000);
    await new Promise(resolve => setTimeout(resolve, backoffMs));
    
    // Note: In a real implementation, you'd need to store the last request
    // For now, this is a placeholder
    console.log('Retrying context retrieval...');
  }, [error, retryCount]);

  /**
   * Check if service is available
   */
  const checkServiceHealth = useCallback(async () => {
    try {
      return await contextService.healthCheck();
    } catch (err) {
      console.error('Health check failed:', err);
      return false;
    }
  }, []);

  return {
    contextData,
    loading,
    error,
    currentStep,
    retryCount,
    retrieveContext,
    retry,
    reset,
    checkServiceHealth,
    isRetryable: error?.retryable || false,
  };
};

export default useContextRetrieval;