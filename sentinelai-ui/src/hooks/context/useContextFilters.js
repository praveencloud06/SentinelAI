/**
 * use Context Filters Hook
 * Manages filter state for engineering context retrieval
 */

import { useState, useCallback, useEffect } from 'react';
import {
  FilterState,
  SourceSystem,
} from '../../types/context/context.types';

const FILTERS_STORAGE_KEY = 'sentinelai_context_filters';

export const useContextFilters = () => {
  const [filters, setFilters] = useState(new FilterState());

  /**
   * Load filters from local storage
   */
  const loadFiltersFromStorage = useCallback(() => {
    try {
      const stored = localStorage.getItem(FILTERS_STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        return new FilterState({
          ...parsed,
          dateRange: parsed.dateRange || { from: null, to: null },
        });
      }
    } catch (error) {
      console.error('Failed to load filters:', error);
    }
    return new FilterState();
  }, []);

  /**
   * Save filters to local storage
   */
  const saveFiltersToStorage = useCallback((filterState) => {
    try {
      localStorage.setItem(FILTERS_STORAGE_KEY, JSON.stringify(filterState));
    } catch (error) {
      console.error('Failed to save filters:', error);
    }
  }, []);

  /**
   * Load filters on mount
   */
  useEffect(() => {
    const loadedFilters = loadFiltersFromStorage();
    setFilters(loadedFilters);
  }, [loadFiltersFromStorage]);

  /**
   * Update a single filter
   */
  const updateFilter = useCallback((key, value) => {
    const updatedFilters = new FilterState({
      ...filters,
      [key]: value,
    });
    setFilters(updatedFilters);
    saveFiltersToStorage(updatedFilters);
  }, [filters, saveFiltersToStorage]);

  /**
   * Update multiple filters at once
   */
  const updateFilters = useCallback((updates) => {
    const updatedFilters = new FilterState({
      ...filters,
      ...updates,
    });
    setFilters(updatedFilters);
    saveFiltersToStorage(updatedFilters);
  }, [filters, saveFiltersToStorage]);

  /**
   * Update date range
   */
  const updateDateRange = useCallback((from, to) => {
    const updatedFilters = new FilterState({
      ...filters,
      dateRange: { from, to },
    });
    setFilters(updatedFilters);
    saveFiltersToStorage(updatedFilters);
  }, [filters, saveFiltersToStorage]);

  /**
   * Toggle source system in filter
   */
  const toggleSourceSystem = useCallback((sourceSystem) => {
    const currentSystems = filters.sourceSystems || [];
    const updatedSystems = currentSystems.includes(sourceSystem)
      ? currentSystems.filter(s => s !== sourceSystem)
      : [...currentSystems, sourceSystem];

    const updatedFilters = new FilterState({
      ...filters,
      sourceSystems: updatedSystems,
    });
    setFilters(updatedFilters);
    saveFiltersToStorage(updatedFilters);
  }, [filters, saveFiltersToStorage]);

  /**
   * Reset all filters
   */
  const resetFilters = useCallback(() => {
    const resetState = new FilterState();
    setFilters(resetState);
    saveFiltersToStorage(resetState);
  }, [saveFiltersToStorage]);

  /**
   * Check if filters have any active values
   */
  const hasActiveFilters = useCallback(() => {
    return filters.hasActiveFilters();
  }, [filters]);

  /**
   * Get filter count (number of active filters)
   */
  const getActiveFilterCount = useCallback(() => {
    let count = 0;
    if (filters.service) count++;
    if (filters.environment) count++;
    if (filters.repository) count++;
    if (filters.dateRange.from || filters.dateRange.to) count++;
    if (filters.sourceSystems.length > 0) count++;
    if (filters.minScore > 0.3) count++;
    return count;
  }, [filters]);

  /**
   * Get filters as API request format
   */
  const getFiltersForRequest = useCallback(() => {
    return {
      service: filters.service || undefined,
      environment: filters.environment || undefined,
      repository: filters.repository || undefined,
      deployment: undefined, // Can be added if needed
      commit: undefined, // Can be added if needed
      time: filters.dateRange.from || undefined,
      maxResults: 10, // Default
      minScore: filters.minScore,
    };
  }, [filters]);

  return {
    filters,
    updateFilter,
    updateFilters,
    updateDateRange,
    toggleSourceSystem,
    resetFilters,
    hasActiveFilters,
    getActiveFilterCount,
    getFiltersForRequest,
  };
};

export default useContextFilters;