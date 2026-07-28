# SentinelAI V2 - Demo Quick Start

## Demo Data Location

All demo-related files, scripts, and documentation are located in the **`demo-data/`** directory:

```
demo-data/
├── elk/                              # ELK seed data
├── rca/                              # RCA log files
├── knowledge/                        # Knowledge Service loader
├── DEMO_GUIDE.md                     # Complete demo walkthrough
├── DEMO_DATA_SUMMARY.md              # Comprehensive data overview
├── INCIDENTS.md                      # Incident definitions
├── README.md                         # Demo data instructions
├── setup-demo-environment.ps1        # Automated setup
└── setup-demo-environment.bat        # Batch wrapper
```

## Quick Start

### 1. Setup Demo Environment

```powershell
cd demo-data
.\setup-demo-environment.bat
```

### 2. Load Knowledge Service Data

After starting all services:

```powershell
cd demo-data\knowledge
.\knowledge-demo-data.bat
```

### 3. Follow Demo Guide

See **`demo-data/DEMO_GUIDE.md`** for complete step-by-step demo instructions.

## What's Included

- **5 Production Incidents** with complete, interconnected data
- **Log Files** for RCA demos (upload via UI)
- **ELK Data** for investigation demos (48+ log entries)
- **Knowledge Service Data** (GitHub, Jira, Confluence, Jenkins, incidents)
- **Complete Demo Scripts** with automated setup

## Services Required

Before running demos, ensure these services are running:

1. **Infrastructure:**
   - PostgreSQL (port 5432)
   - Elasticsearch (port 9200)
   - Ollama (optional, for local AI)

2. **SentinelAI Services:**
   - AI Engine (port 8000)
   - Core (port 8080)
   - Knowledge Service (port 8090)
   - UI (port 3000)

## Demo Documentation

- **DEMO_GUIDE.md** - Step-by-step presentation guide
- **DEMO_DATA_SUMMARY.md** - Complete data overview
- **INCIDENTS.md** - Detailed incident definitions
- **README.md** - Demo data usage instructions

All files are in the **`demo-data/`** directory.

---

For detailed documentation, see:
- **Architecture:** ARCHITECTURE_REVIEW_REPORT.md
- **BFF Implementation:** BFF_IMPLEMENTATION_SUMMARY.md
- **Main README:** README.md
