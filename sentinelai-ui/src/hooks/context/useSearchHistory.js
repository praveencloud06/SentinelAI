/**
 * use Search History Hook
 * Manages search history with local storage persistence
 */

import { useState, useCallback, useEffect } from 'react';
import { contextService } from '../../services/contextService';
import {
  SearchHistoryItem,
  SearchMode,
} from '../../types/context/context.types';

const STORAGE_KEY = 'sentinelai_search_history';
const MAX_HISTORY_ITEMS = 20;

export const useSearchHistory = () => {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);

  /**
   * Load history from local storage
   */
  const loadHistoryFromStorage = useCallback(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        return parsed.map(item => new SearchHistoryItem({
          ...item,
          timestamp: new Date(item.timestamp),
        }));
      }
    } catch (error) {
      console.error('Failed to load search history:', error);
    }
    return [];
  }, []);

  /**
   * Save history to local storage
   */
  const saveHistoryToStorage = useCallback((historyItems) => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(historyItems));
    } catch (error) {
      console.error('Failed to save search history:', error);
    }
  }, []);

  /**
   * Load history on mount
   */
  useEffect(() => {
    const loadedHistory = loadHistoryFromStorage();
    setHistory(loadedHistory);
  }, [loadHistoryFromStorage]);

  /**
   * Add search to history
   */
  const addToHistory = useCallback(async (query, mode, filters, resultCount) => {
    const newItem = new SearchHistoryItem({
      id: `search_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      query,
      mode,
      filters,
      timestamp: new Date(),
      resultCount,
    });

    // Add to beginning of history
    const updatedHistory = [newItem, ...history];
    
    // Limit to max items
    const trimmedHistory = updatedHistory.slice(0, MAX_HISTORY_ITEMS);
    
    setHistory(trimmedHistory);
    saveHistoryToStorage(trimmedHistory);

    // Also try to save to server
    try {
      await contextService.saveSearchToHistory(newItem);
    } catch (error) {
      console.error('Failed to save search history to server:', error);
      // Continue anyway, local storage is sufficient
    }

    return newItem;
  }, [history, saveHistoryToStorage]);

  /**
   * Remove item from history
   */
  const removeFromHistory = useCallback((id) => {
    const updatedHistory = history.filter(item => item.id !== id);
    setHistory(updatedHistory);
    saveHistoryToStorage(updatedHistory);
  }, [history, saveHistoryToStorage]);

  /**
   * Clear all history
   */
  const clearHistory = useCallback(async () => {
    setHistory([]);
    localStorage.removeItem(STORAGE_KEY);
    
    try {
      await contextService.clearSearchHistory();
    } catch (error) {
      console.error('Failed to clear search history from server:', error);
    }
  }, []);

  /**
   * Get history by mode
   */
  const getHistoryByMode = useCallback((mode) => {
    return history.filter(item => item.mode === mode);
  }, [history]);

  /**
   * Get recent searches (last 5)
   */
  const getRecentSearches = useCallback((limit = 5) => {
    return history.slice(0, limit);
  }, [history]);

  /**
   * Search within history
   */
  const searchHistory = useCallback((searchTerm) => {
    const term = searchTerm.toLowerCase();
    return history.filter(item => 
      item.query.toLowerCase().includes(term)
    );
  }, [history]);

  return {
    history,
    loading,
    addToHistory,
    removeFromHistory,
    clearHistory,
    getHistoryByMode,
    getRecentSearches,
    searchHistory,
  };
};

export default useSearchHistory;