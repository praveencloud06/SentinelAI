# ELK Investigation - Engineering Context Enhancement

**Date:** 2026-07-23  
**Objective:** Display meaningful engineering evidence (Jira, Commits, Runbooks, Confidence, Timeline) in ELK Investigation results by extracting from Knowledge Service context and enhancing AI response.

---

## 🎯 Problem Statement

User reported missing engineering evidence in ELK Investigation results:
> "with request payment-service error 12 hours. there is no proper engineering evidence i see in here"

**What was missing:**
- ✗ Confidence percentage
- ✗ Related Jira tickets (e.g., PAY-421)
- ✗ Related Git commits
- ✗ Relevant runbooks/documentation
- ✗ Engineering timeline
- ✗ Structured next steps

---

## 🔧 Solution Implemented

### 1. Enhanced Response DTO

**File:** `SentinelAI-Core/src/main/java/com/sentinel/ai/elk/dto/ElkInvestigationResponse.java`

**Added Enterprise Fields:**
```java
// Enterprise Enhancements (New - All Optional)
private Integer confidence;              // 0-100 confidence score
private String relatedDeployment;        // e.g., "v2.1.5 deployed 2h ago"
private String relatedJira;              // e.g., "PAY-421"
private String relatedCommit;            // e.g., "abc123de"
private String relevantRunbook;          // Link or name
private List<Object> timeline;           // Chronological events
private List<String> nextSteps;          // Action items
private Boolean escalate;                // Requires escalation?
private String escalationReason;         // Why escalate
private Integer totalLogsAnalyzed;       // Metadata
private Long queryTimeMs;                // Performance metric
```

### 2. Enhanced AI Prompt

**File:** `SentinelAI-Core/src/main/java/com/sentinel/ai/elk/prompt/ElkPromptBuilder.java`

**Updated prompt instructions to AI:**
```json
{
  "summary": "<concise 1-2 sentence executive summary>",
  "probableRootCause": "<most likely root cause>",
  "impactedService": "<service name>",
  "recommendedAction": "<specific action>",
  "suspiciousLogs": ["<log entries>"],
  "confidence": 85,  // ← NEW: 0-100 score
  "relatedJira": "PAY-421",  // ← NEW: Extract from context
  "relatedCommit": "abc123de",  // ← NEW: Extract from context
  "relevantRunbook": "Payment Service Runbook",  // ← NEW
  "nextSteps": ["Step 1", "Step 2"],  // ← NEW: Structured actions
  "timeline": [  // ← NEW: Event timeline
    {"timestamp": "2026-07-23T12:30Z", "description": "Deployment"},
    {"timestamp": "2026-07-23T14:30Z", "description": "Incident began"}
  ],
  "escalate": true,  // ← NEW: Escalation flag
  "escalationReason": "Critical production incident"  // ← NEW
}
```

**AI Instructions Added:**
- confidence: 80+ = high (red badge), 50-79 = medium (orange), <50 = low (green)
- timeline: Extract 2-5 key events in chronological order
- nextSteps: Provide 2-4 concrete action items
- escalate: Set true only for critical incidents
- Extract relatedJira, relatedCommit, relevantRunbook from ENGINEERING CONTEXT

### 3. Enhanced Response Parser

**File:** `SentinelAI-Core/src/main/java/com/sentinel/ai/elk/service/ElkResponseParser.java`

**Added parsers for new fields:**
```java
Integer confidence = intOrNull(parsed, "confidence");
String relatedDeployment = stringOrNull(parsed, "relatedDeployment");
String relatedJira = stringOrNull(parsed, "relatedJira");
String relatedCommit = stringOrNull(parsed, "relatedCommit");
String relevantRunbook = stringOrNull(parsed, "relevantRunbook");
List<Object> timeline = listOrEmptyObj(parsed, "timeline");
List<String> nextSteps = listOrEmpty(parsed, "nextSteps");
Boolean escalate = boolOrNull(parsed, "escalate");
String escalationReason = stringOrNull(parsed, "escalationReason");
```

**Helper methods added:**
```java
private Integer intOrNull(Map<String, Object> map, String key);
private Boolean boolOrNull(Map<String, Object> map, String key);
private List<Object> listOrEmptyObj(Map<String, Object> map, String key);
```

### 4. Enhanced Service Implementation

**File:** `SentinelAI-Core/src/main/java/com/sentinel/ai/elk/service/ElkInvestigationServiceImpl.java`

**Context Extraction Methods:**
```java
// Extract Jira tickets (PAY-421, ORD-334, etc.)
private String extractJiraTicket(String contextText) {
    Pattern pattern = Pattern.compile("\\b([A-Z]{2,10}-\\d+)\\b");
    Matcher matcher = pattern.matcher(contextText);
    return matcher.find() ? matcher.group(1) : null;
}

// Extract Git commits (7-40 char hashes)
private String extractCommitHash(String contextText) {
    Pattern pattern = Pattern.compile("\\b([a-f0-9]{7,40})\\b");
    Matcher matcher = pattern.matcher(contextText);
    if (matcher.find()) {
        String hash = matcher.group(1);
        return hash.length() > 8 ? hash.substring(0, 8) : hash;
    }
    return null;
}

// Extract runbook links or mentions
private String extractRunbookLink(String contextText) {
    // Look for "runbook" mentions
    if (contextText.toLowerCase().contains("runbook")) {
        for (String line : contextText.split("\\n")) {
            if (line.toLowerCase().contains("runbook")) {
                return line.trim();
            }
        }
    }
    // Look for Confluence/wiki URLs
    Pattern pattern = Pattern.compile("https?://[^\\s]+(?:confluence|wiki)[^\\s]*");
    Matcher matcher = pattern.matcher(contextText);
    return matcher.find() ? matcher.group(0) : null;
}
```

**Enrichment Logic:**
```java
// Extract engineering artifacts from context
relatedJira = extractJiraTicket(contextText);
relatedCommit = extractCommitHash(contextText);
relevantRunbook = extractRunbookLink(contextText);

// Populate response fields from context if AI didn't provide them
if (result.getRelatedJira() == null && relatedJira != null) {
    result.setRelatedJira(relatedJira);
}
if (result.getRelatedCommit() == null && relatedCommit != null) {
    result.setRelatedCommit(relatedCommit);
}
if (result.getRelevantRunbook() == null && relevantRunbook != null) {
    result.setRelevantRunbook(relevantRunbook);
}

// Use Knowledge Service confidence if AI didn't provide one
if (result.getConfidence() == null && semanticContext.getConfidence() > 0) {
    result.setConfidence(semanticContext.getConfidence());
}

// Add query metadata
result.setTotalLogsAnalyzed(logLines.size());
```

---

## 🎨 Frontend Display

The enhanced UI now displays these fields in the **Engineering Context** section:

```
┌──────────────────────────────────────────────────────────┐
│ 🔗 Engineering Context                                   │
├──────────────────────────────────────────────────────────┤
│                                                          │
│ Related Deployment  v2.1.4-hotfix (deployed 2h ago)      │
│                                                          │
│ Related Jira        PAY-421                              │
│                     ↳ Link to issue                      │
│                                                          │
│ Related Commit      abc123de                             │
│                     ↳ Monospace styled                   │
│                                                          │
│ Relevant            Payment Service Runbook              │
│ Documentation       ↳ Link to wiki                       │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

**Confidence Badge Display:**
```
Confidence:  [85%]  ← Red badge (High Confidence)
             High
```

**Timeline Display:**
```
┌──────────────────────────────────────────────────────────┐
│ 📅 Engineering Timeline                                  │
├──────────────────────────────────────────────────────────┤
│                                                          │
│ ● 2026-07-23 12:30 UTC                                   │
│ │ Deployment v2.1.4-hotfix to production                 │
│ │                                                        │
│ ● 2026-07-23 14:15 UTC                                   │
│ │ First connection timeout warnings                      │
│ │                                                        │
│ ● 2026-07-23 14:30 UTC                                   │
│   Pool exhaustion - incident began                       │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

**Next Steps Display:**
```
Next Steps:
 • Review query performance in payment-db
 • Increase hikari.maximum-pool-size to 50
 • Add connection pool monitoring alerts
 • Investigate recent schema changes
```

---

## 🔄 Data Flow

```
1. User searches: payment-service, ERROR, last 12 hours
                                    ↓
2. ElkClient fetches logs from Elasticsearch
                                    ↓
3. KnowledgeServiceClient retrieves context
   → Finds: PAY-421, commit abc123de, runbook link
                                    ↓
4. ElkPromptBuilder constructs enhanced prompt
   → Includes context with Jira/commit/runbook
                                    ↓
5. AI Engine analyzes with full context
   → Returns: confidence, timeline, next steps
   → Extracts: Jira PAY-421, commit abc123de
                                    ↓
6. ElkInvestigationServiceImpl enriches response
   → Adds extracted Jira/commit/runbook if missing
   → Adds confidence from context if missing
   → Adds totalLogsAnalyzed metadata
                                    ↓
7. UI displays enterprise investigation report
   → Shows confidence badge
   → Links to Jira PAY-421
   → Shows commit hash
   → Shows runbook link
   → Displays timeline
   → Lists next steps
```

---

## ✅ Example Response

### Before (Missing Evidence)
```json
{
  "summary": "Payment processing failures due to database connection pool exhaustion",
  "probableRootCause": "HikariPool-1 exhausted; connections not available",
  "impactedService": "payment-service",
  "recommendedAction": "Increase Hikari pool size or investigate connection leaks",
  "suspiciousLogs": ["HikariCP connection pool exhausted..."]
}
```

### After (With Engineering Evidence)
```json
{
  "summary": "Payment processing failures due to database connection pool exhaustion",
  "probableRootCause": "HikariPool-1 exhausted; connections not available",
  "impactedService": "payment-service",
  "recommendedAction": "Increase Hikari pool size or investigate connection leaks",
  "suspiciousLogs": ["HikariCP connection pool exhausted..."],
  
  "confidence": 85,
  "relatedJira": "PAY-421",
  "relatedCommit": "abc123de",
  "relevantRunbook": "Payment Service Connection Pool Runbook",
  "nextSteps": [
    "Review query performance in payment-db",
    "Increase hikari.maximum-pool-size from 20 to 50",
    "Add connection pool monitoring alerts",
    "Investigate recent schema changes from v2.1.4 deployment"
  ],
  "timeline": [
    {"timestamp": "2026-07-23T12:30:00Z", "description": "Deployment v2.1.4-hotfix to production"},
    {"timestamp": "2026-07-23T14:15:00Z", "description": "First connection timeout warnings"},
    {"timestamp": "2026-07-23T14:30:00Z", "description": "Pool exhaustion - incident began"},
    {"timestamp": "2026-07-23T14:35:00Z", "description": "P1 alert triggered"}
  ],
  "escalate": true,
  "escalationReason": "Critical production incident affecting revenue-generating transactions",
  "totalLogsAnalyzed": 39,
  
  "engineeringContext": {
    "enabled": true,
    "available": true,
    "message": "Semantic context retrieved from SentinelAI Knowledge Service",
    "summary": "PAY-421: Investigate HikariCP pool exhaustion after v2.1.4 deployment",
    "confidence": 85,
    "evidenceCount": 4
  }
}
```

---

## 🧪 Testing Scenarios

### Scenario 1: Full Context Available
- Knowledge Service returns context with PAY-421, commit hash, runbook
- AI extracts timeline and adds confidence score
- **Expected:** All fields populated, high confidence badge

### Scenario 2: Partial Context
- Knowledge Service returns context but no explicit Jira/commit
- AI infers from log patterns
- **Expected:** AI-generated fields shown, context in collapsible panel

### Scenario 3: No Context Available
- Knowledge Service disabled or no matching context
- AI analyzes logs only
- **Expected:** AI provides best-effort analysis, no Jira/commit links

### Scenario 4: Context Extraction
- Context contains: "See Jira PAY-421 and commit abc123def456"
- **Expected:** Jira=PAY-421, Commit=abc123de (shortened)

---

## 📋 Files Modified

1. `SentinelAI-Core/src/main/java/com/sentinel/ai/elk/dto/ElkInvestigationResponse.java` - Added enterprise fields
2. `SentinelAI-Core/src/main/java/com/sentinel/ai/elk/prompt/ElkPromptBuilder.java` - Enhanced AI prompt
3. `SentinelAI-Core/src/main/java/com/sentinel/ai/elk/service/ElkResponseParser.java` - Added parsers for new fields
4. `SentinelAI-Core/src/main/java/com/sentinel/ai/elk/service/ElkInvestigationServiceImpl.java` - Added extraction logic
5. `sentinelai-ui/src/ElkInvestigationPage.jsx` - Enhanced display sections

---

## 🚀 Next Steps

1. **Compile and test** backend changes
2. **Restart services** to load new code
3. **Test with demo data:**
   ```bash
   # Load Knowledge Service data (includes PAY-421, ORD-334)
   cd demo-data\knowledge
   .\knowledge-demo-data-simple.ps1
   
   # Load ELK logs (payment-service errors from 30 min ago)
   cd ..\elk
   .\elk-seed-demo-incidents.bat
   
   # Search in UI:
   # Service: payment-service
   # Severity: ERROR
   # Time: Last 24 hours
   ```
4. **Verify display:**
   - ✅ Confidence badge shows percentage
   - ✅ Related Jira shows "PAY-421"
   - ✅ Timeline has events
   - ✅ Next steps listed
   - ✅ Engineering context panel expands

---

## 💡 Benefits

**For Engineers:**
- ✅ Immediate confidence in AI analysis
- ✅ Direct links to related Jira tickets
- ✅ Quick access to relevant commits
- ✅ Runbook links for remediation
- ✅ Clear timeline of incident progression
- ✅ Actionable next steps

**For Operations:**
- ✅ Faster incident resolution (less context switching)
- ✅ Better decision-making (confidence scores)
- ✅ Improved documentation trail (Jira links)
- ✅ Escalation guidance (escalate flag)

**Technical:**
- ✅ Backward compatible (all new fields optional)
- ✅ Graceful degradation (works without context)
- ✅ Automatic extraction (regex-based, no manual tagging)
- ✅ Rich AI responses (enhanced prompt)

---

**Status:** ✅ Implementation Complete - Ready for Testing  
**Backward Compatible:** ✅ Yes  
**Breaking Changes:** ❌ None
