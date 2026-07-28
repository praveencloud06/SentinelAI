/**
 * Loading Steps Component
 * Shows progressive loading steps instead of a generic spinner
 * Provides better UX during context retrieval
 */

import React from 'react';

const styles = {
  container: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '48px 24px',
    background: '#ffffff',
    borderRadius: 8,
    border: '1px solid #e5e7eb',
    margin: '24px 0',
  },
  stepsContainer: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
    width: '100%',
    maxWidth: '400px',
  },
  step: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '12px',
    borderRadius: 6,
    fontSize: 14,
    transition: 'all 0.3s ease',
  },
  stepActive: {
    background: '#f0f9ff',
    border: '1px solid #bae6fd',
  },
  stepCompleted: {
    background: '#f0fdf4',
    border: '1px solid #bbf7d0',
  },
  stepPending: {
    background: '#f9fafb',
    border: '1px solid #e5e7eb',
    opacity: 0.5,
  },
  stepIcon: {
    width: 20,
    height: 20,
    borderRadius: '50%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: 12,
    fontWeight: 600,
  },
  stepIconActive: {
    background: '#3b82f6',
    color: '#ffffff',
  },
  stepIconCompleted: {
    background: '#10b981',
    color: '#ffffff',
  },
  stepIconPending: {
    background: '#d1d5db',
    color: '#6b7280',
  },
  stepText: {
    flex: 1,
    color: '#374151',
  },
  stepTextActive: {
    fontWeight: 500,
    color: '#1e40af',
  },
  stepTextCompleted: {
    color: '#065f46',
  },
  stepTextPending: {
    color: '#9ca3af',
  },
  spinner: {
    width: 16,
    height: 16,
    border: '2px solid #e5e7eb',
    borderTop: '2px solid #3b82f6',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  },
  title: {
    fontSize: 18,
    fontWeight: 600,
    color: '#1f2937',
    marginBottom: 24,
    textAlign: 'center',
  },
  subtitle: {
    fontSize: 14,
    color: '#6b7280',
    marginBottom: 32,
    textAlign: 'center',
  },
};

const LoadingSteps = ({ currentStep, steps }) => {
  const defaultSteps = [
    'Initializing search...',
    'Searching engineering metadata...',
    'Analyzing relationships...',
    'Finding similar knowledge...',
    'Ranking engineering evidence...',
    'Preparing engineering context...',
  ];

  const displaySteps = steps || defaultSteps;
  const currentIndex = displaySteps.indexOf(currentStep);

  return (
    <div style={styles.container}>
      <div style={styles.title}>Searching Engineering Knowledge</div>
      <div style={styles.subtitle}>Analyzing your logs to find the most relevant engineering context</div>
      
      <div style={styles.stepsContainer}>
        {displaySteps.map((step, index) => {
          let stepStyle = styles.stepPending;
          let iconStyle = styles.stepIconPending;
          let textStyle = styles.stepTextPending;
          let icon = index + 1;

          if (index < currentIndex) {
            stepStyle = styles.stepCompleted;
            iconStyle = styles.stepIconCompleted;
            textStyle = styles.stepTextCompleted;
            icon = '✓';
          } else if (index === currentIndex) {
            stepStyle = styles.stepActive;
            iconStyle = styles.stepIconActive;
            textStyle = styles.stepTextActive;
            icon = <div style={styles.spinner} />;
          }

          return (
            <div
              key={index}
              style={{ ...styles.step, ...stepStyle }}
            >
              <div style={{ ...styles.stepIcon, ...iconStyle }}>
                {icon}
              </div>
              <div style={{ ...styles.stepText, ...textStyle }}>
                {step}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default LoadingSteps;