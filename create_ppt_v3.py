from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.enum.text import PP_ALIGN
from pathlib import Path

base_path = Path(r'C:\application\AI_Project\SentinelAI\SentinelAI_Atos_Presentation - base.pptx')
out_path = Path(r'C:\application\AI_Project\SentinelAI\SentinelAI_Atos_Presentation - v3.pptx')

prs = Presentation(base_path)

# Keep the original slide count and replace content on key slides with richer technical narrative.

# Helper functions

def clear_slide_text(slide):
    for shape in slide.shapes:
        if hasattr(shape, 'text_frame') and shape.text_frame:
            shape.text_frame.clear()


def replace_title_and_body(slide, title, body, subtitle=None):
    # clear existing text shapes except footer-like ones
    for shape in list(slide.shapes):
        if hasattr(shape, 'text_frame') and shape.text_frame:
            text = shape.text.strip()
            if 'Atos Confidential' in text or 'May 2026' in text or (text.startswith('SentinelAI') and '|' in text):
                continue
            shape.text_frame.clear()

    # set title in first non-footer textbox
    title_shape = None
    for shape in slide.shapes:
        if hasattr(shape, 'text_frame') and shape.text_frame:
            if not shape.text.strip():
                title_shape = shape
                break
    if title_shape is None:
        title_shape = slide.shapes.add_textbox(Inches(0.6), Inches(0.35), Inches(11.0), Inches(0.8))
    tf = title_shape.text_frame
    p = tf.paragraphs[0]
    p.text = title
    p.font.size = Pt(24)
    p.font.bold = True
    p.font.name = 'Calibri'

    body_shape = None
    for shape in slide.shapes:
        if hasattr(shape, 'text_frame') and shape.text_frame:
            if not shape.text.strip():
                if title_shape is not None and shape is not title_shape:
                    body_shape = shape
                    break
    if body_shape is None:
        body_shape = slide.shapes.add_textbox(Inches(0.7), Inches(1.35), Inches(10.5), Inches(5.8))
    tfb = body_shape.text_frame
    tfb.clear()
    tfb.word_wrap = True
    paras = body.split('\n')
    for idx, line in enumerate(paras):
        p = tfb.paragraphs[0] if idx == 0 else tfb.add_paragraph()
        p.text = line
        p.font.size = Pt(18 if idx == 0 else 16)
        p.font.name = 'Calibri'
        p.font.bold = False
        p.level = 0
        p.alignment = PP_ALIGN.LEFT
        if line.startswith('•') or line.startswith('-'):
            p.bullet = True
            p.text = line[2:]
            p.level = 0
            p.font.size = Pt(16)
            p.space_after = Pt(4)
    if subtitle:
        p2 = tf.paragraphs[1] if len(tf.paragraphs) > 1 else tf.add_paragraph()
        p2.text = subtitle
        p2.font.size = Pt(12)
        p2.font.bold = False
        p2.font.name = 'Calibri'


def add_link_box(slide, text, address, left=Inches(8.8), top=Inches(7.05), width=Inches(2.2), height=Inches(0.35)):
    box = slide.shapes.add_textbox(left, top, width, height)
    tf = box.text_frame
    tf.clear()
    p = tf.paragraphs[0]
    run = p.add_run()
    run.text = text
    run.hyperlink.address = address
    p.font.size = Pt(10)
    p.font.name = 'Calibri'

# Replace content on the main slides to match the richer v2 story while preserving the original base theme.
replacements = {
    1: ("Why an AI Layer on Top of Observability?", "AIOps theory:\n• Observability data is raw signal, not insight.\n• SentinelAI adds memory, reasoning and live investigation.\n• The platform turns logs into RCA, context and action.\n• Knowledge Service makes each incident reusable for the next one."),
    3: ("SentinelAI in One Slide", "An AI-assisted incident investigation platform built on the stack we already use.\n\n• Log RCA: generate issue, root cause, impacted service and fix.\n• ELK live investigation: query live logs with service, severity and timeframe.\n• Knowledge Service: enrich RCA with deployment, release, timeline and Jira evidence.\n• Future: move from assisted investigation to agentic operations."),
    4: ("High-Level Architecture", "Four deployable services + shared infrastructure.\n\n• sentinelai-ui (React): RCA and ELK investigation experience.\n• SentinelAI-Core (Spring Boot): orchestration, RCA, ELK integration and SKS calls.\n• SentinelAI-Engine (FastAPI): provider routing for Groq, Ollama, OpenAI and HuggingFace.\n• SentinelAI-Knowledge-Service (Spring Boot): engineering memory layer for timeline, releases, deployments and Jira.\n\nInfrastructure: PostgreSQL + pgvector, Elasticsearch, Ollama and Docker."),
    5: ("SentinelAI-Knowledge-Service", "The new capability added after the initial RCA MVP.\n\n• Sync engineering metadata from GitHub, Jira, Confluence and Jenkins.\n• Ingest webhooks and expose timeline, relationships and search APIs.\n• Enable Core to enrich RCA prompts with current deployment and incident context.\n• Turn each incident into reusable operational memory for the next investigation."),
    6: ("Existing Capability – Log RCA", "Paste logs and receive structured root-cause analysis in seconds.\n\n• UI sends the log to Core via POST /api/rca/analyze.\n• Core adds historical context and engineering evidence where available.\n• The AI Engine returns structured issue, root cause, impacted service and recommended fix.\n• This shortens mean time to triage and creates a shareable RCA output."),
    7: ("Existing Capability – ELK-Based Live Investigation", "Investigate live logs directly from observability data.\n\n• Engineer selects service, severity and timeframe.\n• Core queries Elasticsearch and retrieves recent log lines.\n• The AI Engine summarizes the findings and highlights suspicious evidence.\n• Output includes probable root cause, impacted service and recommended action."),
    8: ("Existing Capability – RAG and Incident Memory", "Every investigation becomes better for the next one.\n\n• Store incidents and embeddings for semantic retrieval.\n• Search similar incidents to ground new RCA generation.\n• Combine this with Knowledge Service context for richer operational understanding."),
    12: ("Roadmap – P1 Enhancements", "Three capabilities that turn SentinelAI into a core engineering tool.\n\n• Natural language operational search over ELK and engineering knowledge.\n• Incident correlation using ELK logs, deployments, releases and timeline events.\n• Deployment regression detection with richer context from changes and incidents."),
    13: ("Roadmap – P2 Enhancements", "From pilot to product-grade operations platform.\n\n• Real-time ELK/Kafka monitoring and AI-driven incident grouping.\n• Jira, Slack and Teams automation with RCA and runbook drafts.\n• Enterprise readiness with RBAC, audit logging and LLMOps."),
    14: ("Future Vision – Enterprise AI SRE Copilot", "The long-term operating model.\n\n• The agent reasons over ELK, Kafka, deployments, releases and engineering knowledge.\n• SentinelAI-Knowledge-Service becomes the memory layer for past incidents and changes.\n• The human approves in minutes while the system drafts RCA, runbooks and workflow actions.")
}

for idx, (title, body) in replacements.items():
    if idx < len(prs.slides):
        replace_title_and_body(prs.slides[idx], title, body)

# add architecture link to the architecture slide
add_link_box(prs.slides[4], 'Open architecture draw.io', str(Path(r'C:\application\AI_Project\SentinelAI\SentinelAI-Architecture-v2.drawio').resolve()))

# add a small technical architecture note to the last slide or architecture slide text
prs.save(out_path)
print('Created', out_path)
print('Slide count', len(prs.slides))
