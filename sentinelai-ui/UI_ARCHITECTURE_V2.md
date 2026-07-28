# SentinelAI UI V2 - Engineering Context Explorer

## Current Architecture Analysis

### Existing Structure
```
sentinelai-ui/
├── src/
│   ├── App.js                  # Main app with navigation
│   ├── ElkInvestigationPage.jsx # ELK Investigation page
│   ├── index.js               # Entry point
│   └── ...                    # Other React files
```

### Current Design System
- Dark navigation bar: `#1a1a2e`
- Light content background: `#f5f7fa`
- Brand color: `#4fc3f7` (light blue)
- Card-based layout with shadows
- Tab-based navigation with active state

## New Architecture V2

### Updated Folder Structure
```
sentinelai-ui/
├── src/
│   ├── App.js                          # Updated with new navigation
│   ├── ElkInvestigationPage.jsx         # UNCHANGED
│   ├── index.js                        # UNCHANGED
│   │
│   ├── pages/
│   │   └── EngineeringContextExplorer/  # New context explorer page
│   │       ├── index.jsx                # Main page component
│   │       └── styles.js               # Page-specific styles
│   │
│   ├── components/
│   │   └── context/
│   │       ├── EngineeringEvidenceCard.jsx
│   │       ├── ContextSummaryCard.jsx
│   │       ├── TimelineView.jsx
│   │       ├── RelationshipGraph.jsx
│   │       ├── SearchPanel.jsx
│   │       ├── LoadingSteps.jsx
│   │       ├── EvidenceFilter.jsx
│   │       ├── RelatedDocuments.jsx
│   │       ├── SearchHistory.jsx
│   │       └── EvidenceTable.jsx
│   │
│   ├── hooks/
│   │   └── context/
│   │       ├── useContextRetrieval.js
│   │       ├── useSearchHistory.js
│   │       └── useContextFilters.js
│   │
│   ├── services/
│   │   └── contextService.js            # API service for context retrieval
│   │
│   ├── types/
│   │   └── context/
│   │       ├── context.types.js        # TypeScript-like type definitions
│   │       └── api.types.js             # API response types
│   │
│   ├── utils/
│   │   └── context/
│   │       ├── iconUtils.js             # Icon utilities
│   │       ├── formatters.js            # Data formatters
│   │       └── validators.js            # Input validators
│   │
│   └── constants/
│       └── context/
│           ├── sourceSystems.js         # Source system constants
│           ├── searchModes.js           # Search mode constants
│           └── filterOptions.js          # Filter options
```

## Component Hierarchy

```
EngineeringContextExplorer (Main Page)
├── SearchPanel
│   ├── LogsInputMode
│   ├── NaturalLanguageMode
│   └── QuickExamples
├── LoadingSteps (when loading)
├── ContextSummaryCard (when results available)
├── EngineeringEvidenceGrid
│   └── EngineeringEvidenceCard (multiple)
├── TimelineView
├── RelationshipGraph
├── RelatedDocuments
│   ├── ConfluenceAccordion
│   ├── GitHubAccordion
│   ├── JiraAccordion
│   └── RunbooksAccordion
├── EvidenceFilter
└── SearchHistory
```

## Design System Extension

### Colors
```javascript
const colors = {
  // Existing
  primary: '#4fc3f7',
  background: '#f5f7fa',
  dark: '#1a1a2e',
  
  // New
  card: '#ffffff',
  cardBorder: '#e5e7eb',
  textPrimary: '#1f2937',
  textSecondary: '#6b7280',
  success: '#10b981',
  warning: '#f59e0b',
  error: '#ef4444',
  info: '#3b82f6',
  
  // Source system colors
  github: '#24292e',
  jira: '#0052cc',
  confluence: '#0052cc',
  jenkins: '#d33833',
  deployment: '#8b5cf6',
  commit: '#10b981',
  runbook: '#f59e0b',
  
  // Score colors
  highScore: '#10b981',
  mediumScore: '#f59e0b',
  lowScore: '#ef4444',
};
```

### Typography
```javascript
const typography = {
  fontFamily: "'Segoe UI', Arial, sans-serif",
  h1: { fontSize: 28, fontWeight: 700, color: '#1f2937' },
  h2: { fontSize: 22, fontWeight: 600, color: '#1f2937' },
  h3: { fontSize: 18, fontWeight: 600, color: '#1f2937' },
  body: { fontSize: 14, color: '#4b5563' },
  small: { fontSize: 12, color: '#6b7280' },
};
```

### Spacing
```javascript
const spacing = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  xxl: 32,
};
```

## API Integration

### Context Service
```javascript
class ContextService {
  static async retrieveEngineeringContext(request) {
    // POST /api/context/retrieve
  }
  
  static async searchByQuestion(question, filters) {
    // POST /api/context/explorer
  }
  
  static async getSearchHistory() {
    // GET /api/context/history
  }
}
```

## State Management

### Component State
```javascript
{
  // Search mode
  searchMode: 'logs' | 'natural-language',
  
  // Input
  logs: string,
  naturalLanguageQuery: string,
  
  // Filters
  filters: {
    service: string,
    environment: string,
    repository: string,
    dateRange: { from, to },
    sourceSystem: string[],
    minScore: number,
  },
  
  // Results
  contextData: ContextRetrievalResponse | null,
  loading: boolean,
  error: string | null,
  
  // UI state
  selectedEvidence: string | null,
  expandedDocument: string | null,
  viewMode: 'grid' | 'timeline' | 'graph',
}
```

## Key Features

### 1. Dual Search Modes
- **Logs Mode**: Paste logs or upload file
- **Natural Language Mode**: Ask questions in plain English

### 2. Explainable AI
- Every evidence card explains "Why was this selected?"
- Shows individual signal scores
- Transparent ranking process

### 3. Interactive Visualizations
- Timeline view of engineering events
- Relationship graph with zoom/pan
- Filterable evidence grid

### 4. Enterprise Features
- Search history with replay
- Advanced filtering
- Export capabilities
- Responsive design

## Backward Compatibility

### Preservation Rules
1. **DO NOT modify** `ElkInvestigationPage.jsx`
2. **DO NOT modify** existing `RcaPage` in `App.js`
3. **DO NOT change** existing navigation styles
4. **DO NOT alter** existing color scheme
5. **DO NOT break** existing API calls

### Extension Only
- Add new navigation tab
- Add new page component
- Add new service layer
- Keep existing components untouched

## Responsive Design

### Breakpoints
```javascript
const breakpoints = {
  mobile: '640px',
  tablet: '768px',
  desktop: '1024px',
  wide: '1280px',
};
```

### Mobile Adaptations
- Stack evidence cards vertically
- Simplified timeline view
- Collapsible filters
- Touch-friendly interactions

## Accessibility

### ARIA Labels
- Proper button labels
- Screen reader support
- Keyboard navigation
- Focus management

### Color Contrast
- WCAG AA compliant
- High contrast mode support
- Color-blind friendly alternatives

## Performance Optimizations

### Code Splitting
- Lazy load components
- Route-based splitting
- Dynamic imports

### Caching
- API response caching
- Search history persistence
- Local storage for preferences

### Loading States
- Progressive loading
- Skeleton screens
- Optimistic updates

## Future Extensibility

### Planned Integrations
- Previous RCA Search
- Slack Integration
- ServiceNow
- Azure DevOps
- Datadog
- Grafana

### Extension Points
- Plugin architecture for new sources
- Custom visualizations
- Export formats
- Notification system

## Security Considerations

### Input Validation
- Log file size limits
- Query length restrictions
- XSS prevention
- CSRF protection

### Data Privacy
- No sensitive data logging
- Secure API communication
- Session management
- Permission checks

## Testing Strategy

### Unit Tests
- Component testing
- Hook testing
- Service testing
- Utility testing

### Integration Tests
- API integration
- Component integration
- Navigation testing
- Filter testing

### E2E Tests
- User flow testing
- Cross-browser testing
- Mobile testing
- Performance testing