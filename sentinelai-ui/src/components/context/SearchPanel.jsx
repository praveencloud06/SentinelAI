/**
 * Search Panel Component
 * Dual-mode search: Logs input or Natural Language queries
 * Provides quick examples and file upload capability
 */

import React, { useState } from 'react';

const styles = {
  panel: {
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
    fontSize: 16,
    fontWeight: 600,
    color: '#1f2937',
  },
  modeSelector: {
    display: 'flex',
    gap: 8,
  },
  modeButton: {
    padding: '8px 16px',
    background: '#f3f4f6',
    border: '1px solid #e5e7eb',
    borderRadius: 6,
    fontSize: 13,
    fontWeight: 500,
    color: '#374151',
    cursor: 'pointer',
    transition: 'all 0.2s ease',
  },
  modeButtonActive: {
    background: '#1e40af',
    color: '#ffffff',
    borderColor: '#1e40af',
  },
  modeButtonHover: {
    background: '#e5e7eb',
  },
  searchArea: {
    marginTop: 16,
  },
  textarea: {
    width: '100%',
    minHeight: 120,
    padding: 12,
    border: '1px solid #d1d5db',
    borderRadius: 6,
    fontSize: 14,
    fontFamily: 'monospace',
    resize: 'vertical',
    boxSizing: 'border-box',
  },
  textareaFocus: {
    outline: 'none',
    borderColor: '#3b82f6',
    boxShadow: '0 0 0 3px rgba(59, 130, 246, 0.1)',
  },
  fileUpload: {
    marginTop: 12,
    padding: 16,
    border: '2px dashed #d1d5db',
    borderRadius: 6,
    textAlign: 'center',
    cursor: 'pointer',
    transition: 'border-color 0.2s ease',
  },
  fileUploadHover: {
    borderColor: '#3b82f6',
    background: '#f0f9ff',
  },
  fileUploadText: {
    fontSize: 13,
    color: '#6b7280',
  },
  fileUploadActive: {
    fontSize: 13,
    color: '#3b82f6',
    fontWeight: 500,
  },
  examplesSection: {
    marginTop: 16,
    padding: 12,
    background: '#f9fafb',
    borderRadius: 6,
  },
  examplesTitle: {
    fontSize: 12,
    fontWeight: 600,
    color: '#374151',
    marginBottom: 8,
    textTransform: 'uppercase',
  },
  examplesList: {
    display: 'flex',
    flexDirection: 'column',
    gap: 6,
  },
  exampleItem: {
    padding: '8px 12px',
    background: '#ffffff',
    border: '1px solid #e5e7eb',
    borderRadius: 4,
    fontSize: 13,
    color: '#374151',
    cursor: 'pointer',
    transition: 'all 0.2s ease',
  },
  exampleItemHover: {
    background: '#f0f9ff',
    borderColor: '#bfdbfe',
    color: '#1e40af',
  },
  searchButton: {
    width: '100%',
    padding: '12px',
    background: '#1e40af',
    color: '#ffffff',
    border: 'none',
    borderRadius: 6,
    fontSize: 14,
    fontWeight: 600,
    cursor: 'pointer',
    marginTop: 16,
    transition: 'background 0.2s ease',
  },
  searchButtonHover: {
    background: '#1e3a8a',
  },
  searchButtonDisabled: {
    background: '#9ca3af',
    cursor: 'not-allowed',
  },
  characterCount: {
    textAlign: 'right',
    fontSize: 12,
    color: '#6b7280',
    marginTop: 4,
  },
  characterCountWarning: {
    color: '#f59e0b',
  },
  characterCountError: {
    color: '#ef4444',
  },
};

const QUICK_EXAMPLES = [
  'Find similar incidents',
  'Show deployment related to payment-service',
  'Find Hikari timeout documentation',
  'Show Jira related to database connection pool',
  'Display recent commits for authentication service',
  'Find runbooks for API gateway failures',
];

const SearchPanel = ({ searchMode, onSearchModeChange, onSearch, loading }) => {
  const [logs, setLogs] = useState('');
  const [naturalLanguageQuery, setNaturalLanguageQuery] = useState('');
  const [file, setFile] = useState(null);
  const [isDragOver, setIsDragOver] = useState(false);

  const MAX_CHARS = 100000;
  const WARNING_THRESHOLD = 80000;

  const handleFileChange = (e) => {
    const selectedFile = e.target.files[0];
    if (selectedFile) {
      setFile(selectedFile);
      setLogs(''); // Clear text input when file is selected
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragOver(true);
  };

  const handleDragLeave = () => {
    setIsDragOver(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragOver(false);
    
    const droppedFile = e.dataTransfer.files[0];
    if (droppedFile) {
      setFile(droppedFile);
      setLogs('');
    }
  };

  const handleExampleClick = (example) => {
    setNaturalLanguageQuery(example);
  };

  const handleSearch = async () => {
    let searchContent = '';
    
    if (searchMode === 'logs') {
      if (file) {
        // Read file content
        const reader = new FileReader();
        reader.onload = (e) => {
          searchContent = e.target.result;
          onSearch({ logs: searchContent, mode: 'logs' });
        };
        reader.readAsText(file);
        return;
      } else {
        searchContent = logs;
      }
    } else {
      searchContent = naturalLanguageQuery;
    }

    if (searchContent.trim()) {
      await onSearch({ 
        logs: searchContent, 
        mode: searchMode,
        query: searchMode === 'natural-language' ? searchContent : undefined,
      });
    }
  };

  const getCharacterCountStyle = () => {
    const currentLength = searchMode === 'logs' ? logs.length : naturalLanguageQuery.length;
    if (currentLength >= MAX_CHARS) return styles.characterCountError;
    if (currentLength >= WARNING_THRESHOLD) return styles.characterCountWarning;
    return {};
  };

  const currentLength = searchMode === 'logs' ? logs.length : naturalLanguageQuery.length;

  return (
    <div style={styles.panel}>
      {/* Header */}
      <div style={styles.header}>
        <div style={styles.title}>Search Engineering Context</div>
        
        {/* Mode Selector */}
        <div style={styles.modeSelector}>
          <button
            style={{
              ...styles.modeButton,
              ...(searchMode === 'logs' ? styles.modeButtonActive : {}),
            }}
            onClick={() => onSearchModeChange('logs')}
          >
            Paste Logs
          </button>
          <button
            style={{
              ...styles.modeButton,
              ...(searchMode === 'natural-language' ? styles.modeButtonActive : {}),
            }}
            onClick={() => onSearchModeChange('natural-language')}
          >
            Natural Language
          </button>
        </div>
      </div>

      {/* Search Area */}
      <div style={styles.searchArea}>
        {searchMode === 'logs' ? (
          <>
            {/* Logs Mode */}
            <textarea
              style={styles.textarea}
              placeholder="Paste your logs here or upload a log file..."
              value={logs}
              onChange={(e) => {
                setLogs(e.target.value);
                setFile(null);
              }}
              disabled={!!file || loading}
            />
            
            {/* File Upload */}
            <div
              style={{
                ...styles.fileUpload,
                ...(isDragOver ? styles.fileUploadHover : {}),
              }}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
              onClick={() => document.getElementById('file-input').click()}
            >
              <input
                id="file-input"
                type="file"
                accept=".txt,.log"
                onChange={handleFileChange}
                style={{ display: 'none' }}
              />
              {file ? (
                <div style={styles.fileUploadActive}>
                  📄 {file.name}
                </div>
              ) : (
                <div style={styles.fileUploadText}>
                  {isDragOver ? 'Drop log file here' : '📁 Click to upload or drag & drop log file'}
                </div>
              )}
            </div>
          </>
        ) : (
          <>
            {/* Natural Language Mode */}
            <textarea
              style={styles.textarea}
              placeholder="Ask a question about your engineering context..."
              value={naturalLanguageQuery}
              onChange={(e) => setNaturalLanguageQuery(e.target.value)}
              disabled={loading}
            />
            
            {/* Quick Examples */}
            <div style={styles.examplesSection}>
              <div style={styles.examplesTitle}>Quick Examples</div>
              <div style={styles.examplesList}>
                {QUICK_EXAMPLES.map((example, index) => (
                  <div
                    key={index}
                    style={styles.exampleItem}
                    onClick={() => handleExampleClick(example)}
                  >
                    {example}
                  </div>
                ))}
              </div>
            </div>
          </>
        )}

        {/* Character Count */}
        <div style={{ ...styles.characterCount, ...getCharacterCountStyle() }}>
          {currentLength.toLocaleString()} / {MAX_CHARS.toLocaleString()} characters
        </div>

        {/* Search Button */}
        <button
          style={{
            ...styles.searchButton,
            ...(loading ? styles.searchButtonDisabled : {}),
          }}
          onClick={handleSearch}
          disabled={loading || currentLength === 0}
        >
          {loading ? 'Searching...' : 'Find Engineering Context'}
        </button>
      </div>
    </div>
  );
};

export default SearchPanel;