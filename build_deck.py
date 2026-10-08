import os
import sys
import pptx
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE
from pptx.dml.color import RGBColor

# Output setup
OUTPUT_DIR = r"D:\POC Projects\Vintage Melodies Android"
if not os.path.exists(OUTPUT_DIR):
    os.makedirs(OUTPUT_DIR)

OUTPUT_FILE = os.path.join(OUTPUT_DIR, "Vintage_Melodies_Existing_Project_and_Android_Blueprint.pptx")
IMG_DIR = r"D:\POC Projects\Vintage Melodies\public\images"

BG_IMG = os.path.join(IMG_DIR, "background.jpg")
WEB_IMG = os.path.join(IMG_DIR, "web.png")
MOBILE_IMG = os.path.join(IMG_DIR, "Mobile.png")

prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)

# Palette definition
DARK_BG = RGBColor(0x15, 0x0D, 0x06)       # Deep Vintage Coffee / Wood
CARD_BG = RGBColor(0x24, 0x18, 0x0D)       # Dark Warm Card Box
CARD_BORDER = RGBColor(0x6E, 0x48, 0x24)   # Subtle Vintage Gold Border
AMBER_ACCENT = RGBColor(0xF6, 0x82, 0x1F)  # Bright Vintage Amber
GOLD_ACCENT = RGBColor(0xE6, 0x9D, 0x45)   # Warm Gold Accent
HEADER_GOLD = RGBColor(0xFF, 0xD1, 0x80)   # Highlight Gold
TEXT_WHITE = RGBColor(0xFF, 0xF8, 0xF0)    # Soft Off-White
TEXT_MUTED = RGBColor(0xC8, 0xB2, 0x9B)    # Muted Tan Text
CODE_BG = RGBColor(0x1B, 0x11, 0x08)       # Code block background
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
BLACK = RGBColor(0x00, 0x00, 0x00)
TABLE_HEADER_BG = RGBColor(0x40, 0x25, 0x12)
TABLE_ROW_BG = RGBColor(0x28, 0x1B, 0x0E)
TABLE_ROW_ALT = RGBColor(0x1F, 0x14, 0x0A)

blank_layout = prs.slide_layouts[6]

def set_slide_background(slide):
    bg_shape = slide.shapes.add_shape(
        MSO_SHAPE.RECTANGLE, Inches(0), Inches(0), Inches(13.333), Inches(7.5)
    )
    bg_shape.fill.solid()
    bg_shape.fill.fore_color.rgb = DARK_BG
    bg_shape.line.fill.background()
    return bg_shape

def add_header(slide, title_text, category_text="VINTAGE MELODIES ARCHITECTURE & ANDROID BLUEPRINT"):
    cat_box = slide.shapes.add_textbox(Inches(0.8), Inches(0.4), Inches(11.7), Inches(0.35))
    tf_cat = cat_box.text_frame
    tf_cat.word_wrap = True
    p_cat = tf_cat.paragraphs[0]
    p_cat.text = category_text.upper()
    p_cat.font.size = Pt(10)
    p_cat.font.bold = True
    p_cat.font.color.rgb = AMBER_ACCENT
    
    title_box = slide.shapes.add_textbox(Inches(0.8), Inches(0.72), Inches(11.7), Inches(0.7))
    tf_title = title_box.text_frame
    tf_title.word_wrap = True
    p_title = tf_title.paragraphs[0]
    p_title.text = title_text
    p_title.font.size = Pt(22)
    p_title.font.bold = True
    p_title.font.color.rgb = TEXT_WHITE

def add_card(slide, left, top, width, height, title="", border_color=CARD_BORDER, bg_color=CARD_BG):
    card = slide.shapes.add_shape(
        MSO_SHAPE.ROUNDED_RECTANGLE, Inches(left), Inches(top), Inches(width), Inches(height)
    )
    card.fill.solid()
    card.fill.fore_color.rgb = bg_color
    card.line.color.rgb = border_color
    card.line.width = Pt(1.5)
    
    if title:
        tb = slide.shapes.add_textbox(Inches(left + 0.2), Inches(top + 0.15), Inches(width - 0.4), Inches(0.45))
        tf = tb.text_frame
        tf.word_wrap = True
        p = tf.paragraphs[0]
        p.text = title
        p.font.size = Pt(14)
        p.font.bold = True
        p.font.color.rgb = GOLD_ACCENT
    return card

def add_bullet_list(slide, left, top, width, height, items, font_size=12, space_after=7):
    tb = slide.shapes.add_textbox(Inches(left), Inches(top), Inches(width), Inches(height))
    tf = tb.text_frame
    tf.word_wrap = True
    for i, item in enumerate(items):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        if isinstance(item, tuple):
            header, desc = item
            run1 = p.add_run()
            run1.text = "• " + header + ": "
            run1.font.bold = True
            run1.font.size = Pt(font_size)
            run1.font.color.rgb = HEADER_GOLD
            
            run2 = p.add_run()
            run2.text = desc
            run2.font.size = Pt(font_size)
            run2.font.color.rgb = TEXT_WHITE
        else:
            run = p.add_run()
            run.text = "• " + item
            run.font.size = Pt(font_size)
            run.font.color.rgb = TEXT_WHITE
        p.space_after = Pt(space_after)

# ---------------------------------------------------------
# SLIDE 1: COVER SLIDE
# ---------------------------------------------------------
slide1 = prs.slides.add_slide(blank_layout)
set_slide_background(slide1)

# Left Side Image Banner
if os.path.exists(BG_IMG):
    slide1.shapes.add_picture(BG_IMG, Inches(0.8), Inches(1.2), width=Inches(4.8), height=Inches(5.1))

# Right Side Cover Box
add_card(slide1, 5.9, 1.2, 6.6, 5.1, title="", border_color=AMBER_ACCENT)

cov_box = slide1.shapes.add_textbox(Inches(6.2), Inches(1.5), Inches(6.0), Inches(4.5))
tf_cov = cov_box.text_frame
tf_cov.word_wrap = True

p = tf_cov.paragraphs[0]
p.text = "Vintage Melodies"
p.font.size = Pt(36)
p.font.bold = True
p.font.color.rgb = AMBER_ACCENT
p.space_after = Pt(10)

p2 = tf_cov.add_paragraph()
p2.text = "Existing Web Project & Android App Development Blueprint"
p2.font.size = Pt(18)
p2.font.bold = True
p2.font.color.rgb = TEXT_WHITE
p2.space_after = Pt(14)

p3 = tf_cov.add_paragraph()
p3.text = "Comprehensive Web Application Analysis, Architecture Documentation & Native Android Implementation Specification"
p3.font.size = Pt(13)
p3.font.color.rgb = TEXT_MUTED
p3.space_after = Pt(20)

meta_items = [
    ("Live Web Deployment", "https://vintage-melodies-kappa.vercel.app/"),
    ("Web Tech Stack", "HTML5 • CSS3 • JavaScript (ES6) • Node.js • Cloudflare R2 • Vercel"),
    ("Android Architecture", "100% Kotlin • Jetpack Compose • Media3 / ExoPlayer • Room DB"),
    ("Cloud Storage", "Cloudflare R2 Object Storage (Direct S3 Audio Streaming)"),
    ("Target Location", "D:\\POC Projects\\Vintage Melodies Android")
]

for label, val in meta_items:
    pm = tf_cov.add_paragraph()
    r1 = pm.add_run()
    r1.text = label + ": "
    r1.font.bold = True
    r1.font.size = Pt(11)
    r1.font.color.rgb = GOLD_ACCENT
    r2 = pm.add_run()
    r2.text = val
    r2.font.size = Pt(11)
    r2.font.color.rgb = TEXT_WHITE
    pm.space_after = Pt(6)


# ---------------------------------------------------------
# SLIDE 2: PROJECT OVERVIEW & EXECUTIVE SUMMARY
# ---------------------------------------------------------
slide2 = prs.slides.add_slide(blank_layout)
set_slide_background(slide2)
add_header(slide2, "Project Overview & Executive Summary")

add_card(slide2, 0.8, 1.6, 5.7, 5.2, title="Core Project Concept & UX")
add_bullet_list(slide2, 1.0, 2.2, 5.3, 4.4, [
    ("Nostalgic Ambient Player", "Vintage Melodies is an immersive music platform offering warm retro melodies with full-screen vintage backdrop visuals."),
    ("Target Users", "Retro music enthusiasts, ambient listeners seeking continuous background playback, and users desiring curated regional playlists."),
    ("Current Deployment", "Deployed live on Vercel with high-performance static SPA asset delivery and Node.js serverless functions."),
    ("Core Value Proposition", "Delivers continuous nostalgic audio streaming with zero intrusion, warm translucent UI, and cloud-hosted MP3 library.")
], font_size=13, space_after=12)

add_card(slide2, 6.8, 1.6, 5.7, 2.5, title="Motivation for Native Android App")
add_bullet_list(slide2, 7.0, 2.2, 5.3, 1.8, [
    ("Background Throttling Fix", "Web browsers aggressively throttle JS background timers and suspend audio on lock screen. Android Native provides robust Foreground Media Services."),
    ("Native Controls & Lockscreen", "Integrate directly into Android Quick Settings, Lockscreen controls, Bluetooth hardware buttons, and Android Auto."),
    ("Offline Track Caching", "Store played audio locally in Room DB & ExoPlayer disk cache for playback in zero-connectivity environments.")
], font_size=12, space_after=6)

add_card(slide2, 6.8, 4.3, 5.7, 2.5, title="Existing Web Data Flow")
diag_box = slide2.shapes.add_textbox(Inches(7.0), Inches(4.8), Inches(5.3), Inches(1.8))
tf_diag = diag_box.text_frame
tf_diag.word_wrap = True
diag_lines = [
    "User / Mobile Browser",
    "       ↓ (HTTPS SPA Load)",
    "Vintage Melodies Web App (HTML/CSS/JS)",
    "       ↓ (REST API Calls)",
    "Vercel Serverless / Node.js Express API",
    "       ↓ (S3 Protocol / REST)",
    "Cloudflare R2 Object Storage (Audio CDN)"
]
for dl in diag_lines:
    pd = tf_diag.add_paragraph()
    pd.text = dl
    pd.font.size = Pt(11)
    pd.font.bold = ("↓" not in dl)
    pd.font.color.rgb = AMBER_ACCENT if "Cloudflare" in dl or "Web App" in dl else TEXT_WHITE
    pd.alignment = PP_ALIGN.CENTER


# ---------------------------------------------------------
# SLIDE 3: EXISTING PROJECT TECHNOLOGY STACK
# ---------------------------------------------------------
slide3 = prs.slides.add_slide(blank_layout)
set_slide_background(slide3)
add_header(slide3, "Existing Web Project Technology Stack")

tech_cards = [
    (0.8, 1.6, 3.6, 2.5, "Frontend & Styling", [
        ("HTML5 SPA", "Single-Page Application architecture with semantic HTML markup"),
        ("Custom CSS3 System", "Glassmorphic backdrop-filter, dark gold themes, CSS variables"),
        ("Vanilla JS Modules", "ES6 modular design (`app.js`, `player.js`, `admin.js`, `store.js`)"),
        ("Responsive Layout", "CSS Grid & Flexbox optimized for 320px mobile to 4K desktop")
    ]),
    (4.8, 1.6, 3.6, 2.5, "Backend & Server API", [
        ("Node.js Runtime", "Node.js v22.x execution environment"),
        ("Express.js Framework", "REST API routing, middleware, error logging"),
        ("Express-Session", "Session cookie management with serverless fallback"),
        ("Vercel Functions", "Stateless serverless deployment compatibility")
    ]),
    (8.8, 1.6, 3.7, 2.5, "Cloud Storage & Media", [
        ("Cloudflare R2", "S3-compatible object storage for hosting audio MP3 files"),
        ("AWS SDK v3", "`@aws-sdk/client-s3` used for bucket querying"),
        ("Cloudflare CDN", "Direct CDN edge delivery (`pub-*.r2.dev`) for fast streaming"),
        ("Metadata Parser", "Auto-cleans title, artist, and maps artwork")
    ]),
    (0.8, 4.3, 5.6, 2.5, "Database & Client State", [
        ("SQLite Database", "`better-sqlite3` database engine (`./data/vintage-melodies.db`)"),
        ("Relational Schema", "Tables: `songs`, `playlists`, `playlist_songs` with foreign keys"),
        ("Local Persistence", "Browser `LocalStorage` fallback via `store.js` for offline state"),
        ("Admin Auth State", "Secure HTTP-only session cookies & local role verification")
    ]),
    (6.8, 4.3, 5.7, 2.5, "Hosting & Deployment", [
        ("Vercel Hosting", "Automated deployment pipeline via `vercel.json`"),
        ("Live URL", "https://vintage-melodies-kappa.vercel.app/"),
        ("SSL & Proxy", "Automatic HTTPS termination & reverse proxy compatibility"),
        ("Zero Dependency APIs", "Standalone Cloudflare R2 streaming (search APIs removed)")
    ])
]

for left, top, width, height, title, items in tech_cards:
    add_card(slide3, left, top, width, height, title=title)
    add_bullet_list(slide3, left + 0.2, top + 0.6, width - 0.4, height - 0.7, items, font_size=11, space_after=4)


# ---------------------------------------------------------
# SLIDE 4: SYSTEM ARCHITECTURE & DATA FLOW
# ---------------------------------------------------------
slide4 = prs.slides.add_slide(blank_layout)
set_slide_background(slide4)
add_header(slide4, "System Architecture & End-to-End Data Flow")

add_card(slide4, 0.8, 1.6, 11.7, 5.2, title="Detailed Web Application Architecture")

arch_steps = [
    ("1. Client SPA Initialization", "Browser requests `index.html`. Client JS initializes `App`, `Player`, `Auth`, and `Store` modules."),
    ("2. Playlist & Track Query", "Client requests `/api/playlists` and `/api/cloud-r2/songs`. Express queries SQLite DB & Cloudflare R2 S3 bucket."),
    ("3. Cloudflare R2 Bucket Parsing", "Server executes `ListObjectsV2Command` recursively across all bucket folders, cleaning titles and assigning artwork."),
    ("4. Media Stream URL Handshake", "Server returns direct Cloudflare R2 CDN audio URLs (`https://pub-*.r2.dev/folder/song.mp3`)."),
    ("5. HTML5 & Web Audio Playback", "Client loads audio URL into `<audio>` element and connects to Web Audio API `AudioContext` for 5-Band Equalizer processing."),
    ("6. Media Session OS Integration", "Pushes track metadata (Title, Artist, Artwork) to browser/OS `navigator.mediaSession` for lock-screen controls.")
]

for idx, (title, desc) in enumerate(arch_steps):
    step_top = 2.2 + idx * 0.72
    # Number badge shape
    badge = slide4.shapes.add_shape(MSO_SHAPE.OVAL, Inches(1.0), Inches(step_top), Inches(0.45), Inches(0.45))
    badge.fill.solid()
    badge.fill.fore_color.rgb = AMBER_ACCENT
    badge.line.fill.background()
    tf_b = badge.text_frame
    p_b = tf_b.paragraphs[0]
    p_b.text = str(idx + 1)
    p_b.font.bold = True
    p_b.font.size = Pt(14)
    p_b.font.color.rgb = WHITE
    p_b.alignment = PP_ALIGN.CENTER
    
    # Text box
    tb = slide4.shapes.add_textbox(Inches(1.6), Inches(step_top - 0.05), Inches(10.6), Inches(0.65))
    tf = tb.text_frame
    tf.word_wrap = True
    p1 = tf.paragraphs[0]
    p1.text = title
    p1.font.bold = True
    p1.font.size = Pt(13)
    p1.font.color.rgb = GOLD_ACCENT
    
    p2 = tf.add_paragraph()
    p2.text = desc
    p2.font.size = Pt(11)
    p2.font.color.rgb = TEXT_WHITE


# ---------------------------------------------------------
# SLIDE 5: CORE USER FEATURES & UI COMPONENTS
# ---------------------------------------------------------
slide5 = prs.slides.add_slide(blank_layout)
set_slide_background(slide5)
add_header(slide5, "Core User Features & UI Components")

feat_cards = [
    (0.8, 1.6, 5.7, 2.5, "Translucent Glass Audio Player", [
        ("Vinyl Animation Ring", "Circular album art with CSS keyframe rotating vinyl animation during active playback"),
        ("Full Playback Controls", "Play/Pause toggle, Previous/Next track, Shuffle toggle, Repeat One badge toggle"),
        ("Interactive Progress Bar", "Seekable progress slider with current time and total track duration display"),
        ("Volume & Mute Slider", "Custom styled range slider with instant mute/unmute capability")
    ]),
    (6.8, 1.6, 5.7, 2.5, "Web Audio 5-Band Equalizer", [
        ("Web Audio API Processing", "Uses `AudioContext` and `BiquadFilterNode` chain for hardware audio filtering"),
        ("Frequency Bands", "Adjustable sliders for 60Hz (Bass), 250Hz, 1kHz (Mid), 4kHz, and 12kHz (Treble)"),
        ("Built-in Audio Presets", "One-tap presets: Flat, Vintage Warm, Bass Boost, Vocal, and Treble Boost"),
        ("Live Visualizer Bars", "5 animated equalizer visualizer bars bouncing in sync with audio state")
    ]),
    (0.8, 4.3, 5.7, 2.5, "Sleep Timer / Schedule Modal", [
        ("Auto Playback Off", "Allows users to set countdown timers (15m, 30m, 60m, 90m) to stop playback"),
        ("Live Countdown Banner", "Shows active remaining time in modal and player notification bar"),
        ("Gentle Fade Out", "Seamlessly pauses audio when countdown reaches zero to save battery/data"),
        ("One-Tap Cancel", "Allows instant deactivation of sleep schedule at any time")
    ]),
    (6.8, 4.3, 5.7, 2.5, "Slide-out Playlist Drawer & Grid", [
        ("Home Mood Grid", "Visual playlist cards rendered with mobile artwork thumbnails"),
        ("Slide-out Drawer", "Smooth CSS transition drawer accessible from top bar or mobile nav"),
        ("Random Track Shuffler", "One-click 'Play Random' button to trigger instant random track selection"),
        ("Track Details Popover", "Hover popover displaying song metadata, artwork, folder path, and duration")
    ])
]

for left, top, width, height, title, items in feat_cards:
    add_card(slide5, left, top, width, height, title=title)
    add_bullet_list(slide5, left + 0.2, top + 0.6, width - 0.4, height - 0.7, items, font_size=11, space_after=4)


# ---------------------------------------------------------
# SLIDE 6: MOBILE & RESPONSIVE WEB EXPERIENCE
# ---------------------------------------------------------
slide6 = prs.slides.add_slide(blank_layout)
set_slide_background(slide6)
add_header(slide6, "Mobile & Responsive Web Experience")

add_card(slide6, 0.8, 1.6, 5.7, 5.2, title="Responsive Design Architecture")
add_bullet_list(slide6, 1.0, 2.2, 5.3, 4.4, [
    ("Mobile Bottom Navigation Bar", "Fixed bottom navigation bar with Home, Playlists, Now Playing, and Schedule touch targets."),
    ("Media Session API Integration", "Pushes track title, artist, album art, and actions (`play`, `pause`, `previoustrack`, `nexttrack`) to iOS Control Center & Android Quick Settings."),
    ("Touch-Optimized Sliders", "Custom CSS range inputs optimized for finger drag on mobile screens without scroll interference."),
    ("Dynamic Artwork Swapping", "Uses desktop vs mobile specific artwork images (`_mobile.jpg` / `_web.png`) to optimize mobile bandwidth.")
], font_size=12, space_after=10)

# Embedded Screenshot Cards
add_card(slide6, 6.8, 1.6, 5.7, 5.2, title="Visual Interface Reference")

if os.path.exists(WEB_IMG):
    slide6.shapes.add_picture(WEB_IMG, Inches(7.0), Inches(2.2), width=Inches(3.3), height=Inches(2.0))
    tb_w = slide6.shapes.add_textbox(Inches(10.4), Inches(2.2), Inches(1.9), Inches(2.0))
    tf_w = tb_w.text_frame
    tf_w.word_wrap = True
    p_w = tf_w.paragraphs[0]
    p_w.text = "Desktop SPA Layout\nFull vintage backdrop, translucent glass player & header bar."
    p_w.font.size = Pt(10)
    p_w.font.color.rgb = TEXT_WHITE

if os.path.exists(MOBILE_IMG):
    slide6.shapes.add_picture(MOBILE_IMG, Inches(7.0), Inches(4.5), width=Inches(3.3), height=Inches(2.1))
    tb_m = slide6.shapes.add_textbox(Inches(10.4), Inches(4.5), Inches(1.9), Inches(2.1))
    tf_m = tb_m.text_frame
    tf_m.word_wrap = True
    p_m = tf_m.paragraphs[0]
    p_m.text = "Mobile Native View\nBottom navigation bar, responsive mood grid, touch player."
    p_m.font.size = Pt(10)
    p_m.font.color.rgb = TEXT_WHITE


# ---------------------------------------------------------
# SLIDE 7: CLOUDFLARE R2 STORAGE & AUDIO ENGINE
# ---------------------------------------------------------
slide7 = prs.slides.add_slide(blank_layout)
set_slide_background(slide7)
add_header(slide7, "Cloudflare R2 Storage & Audio Engine Blueprint")

add_card(slide7, 0.8, 1.6, 5.7, 5.2, title="Cloudflare R2 Storage Architecture")
add_bullet_list(slide7, 1.0, 2.2, 5.3, 4.4, [
    ("S3 Protocol Integration", "Configured using `@aws-sdk/client-s3` targeting `https://<account_id>.r2.cloudflarestorage.com`."),
    ("Bucket Credentials", "Bucket Name: `vintage-melodies`, Public Domain: `https://pub-e7f4f743d98b43b89671fe068ee1ee05.r2.dev`."),
    ("Recursive Object Pagination", "Uses `ListObjectsV2Command` with `ContinuationToken` loop to fetch ALL audio files across subfolders."),
    ("Supported File Formats", "Filters extensions: `.mp3`, `.m4a`, `.wav`, `.flac`, `.aac`, `.ogg`, `.opus`."),
    ("Direct CDN Streaming", "Generates direct public HTTP streaming URLs for zero-latency progressive audio playback.")
], font_size=12, space_after=8)

add_card(slide7, 6.8, 1.6, 5.7, 5.2, title="Automated Metadata Normalizer Logic")

# Code snippet frame
code_card = slide7.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(7.0), Inches(2.2), Inches(5.3), Inches(4.3))
code_card.fill.solid()
code_card.fill.fore_color.rgb = CODE_BG
code_card.line.color.rgb = CARD_BORDER

tb_code = slide7.shapes.add_textbox(Inches(7.1), Inches(2.3), Inches(5.1), Inches(4.1))
tf_code = tb_code.text_frame
tf_code.word_wrap = True

code_lines = [
    "// server/routes/cloud.js - Title & Artist Cleaning",
    "function parseCleanTitleArtist(filename, folderPath) {",
    "  let cleaned = filename.replace(/\\.[^/.]+$/, '')",
    "    .replace(/\\[.*?\\]|\\(.*?\\)/g, '')       // Strip tags",
    "    .replace(/sensongs|raagtune|320kbps/gi, '') // Remove site tags",
    "    .replace(/^(\\d{1,3})[\\s._-]+/, '')      // Strip track numbers",
    "    .replace(/_/g, ' ').trim();",
    "",
    "  let title = cleaned;",
    "  let artist = folderPath ? `📁 ${folderPath}` : 'Vintage Melodies';",
    "  if (cleaned.includes(' - ')) {",
    "    const parts = cleaned.split(' - ');",
    "    title = parts[0]; artist = parts[1];",
    "  }",
    "  return { title, artist };",
    "}"
]

for cl in code_lines:
    pc = tf_code.add_paragraph()
    pc.text = cl
    pc.font.name = "Consolas"
    pc.font.size = Pt(9.5)
    pc.font.color.rgb = AMBER_ACCENT if cl.startswith("//") or "function" in cl else TEXT_WHITE


# ---------------------------------------------------------
# SLIDE 8: ADMIN & CONTENT MANAGEMENT ARCHITECTURE
# ---------------------------------------------------------
slide8 = prs.slides.add_slide(blank_layout)
set_slide_background(slide8)
add_header(slide8, "Admin & Content Management Architecture")

add_card(slide8, 0.8, 1.6, 5.7, 5.2, title="Admin Security & Authentication")
add_bullet_list(slide8, 1.0, 2.2, 5.3, 4.4, [
    ("Session-Based Auth", "Admin authentication handled via `/api/auth/login` endpoint with Express session cookies."),
    ("Stateless Cookie Fallback", "Includes custom `sessionMiddleware` ensuring admin auth state persists in Vercel serverless environments."),
    ("Role-Based Privileges", "Non-admin users can browse/play playlists; Admin privileges unlock track management and R2 import."),
    ("Protected Endpoints", "Search, playlist creation, song addition, and server logs (`/api/logs`) strictly require admin session.")
], font_size=12, space_after=10)

add_card(slide8, 6.8, 1.6, 5.7, 5.2, title="Cloud R2 Song Picker & Bulk Add")
add_bullet_list(slide8, 7.0, 2.2, 5.3, 4.4, [
    ("Cloud R2 Drawer Panel", "Dedicated UI panel (`cloud-songs-panel`) listing all audio files from Cloudflare R2."),
    ("Real-Time Filter Input", "Instant search filtering by track title, artist name, or folder path."),
    ("Multi-Select Bulk Bar", "Checkbox selection interface (`cloud-select-all-cb`) allowing selection of multiple R2 tracks."),
    ("One-Click Playlist Import", "Adds batch selected R2 tracks into any SQLite playlist in a single atomic database operation."),
    ("Playlist Management", "Create new playlists, edit names, upload web/mobile cover images, and reorder songs.")
], font_size=12, space_after=10)


# ---------------------------------------------------------
# SLIDE 9: DATABASE SCHEMA & DATA MODELS
# ---------------------------------------------------------
slide9 = prs.slides.add_slide(blank_layout)
set_slide_background(slide9)
add_header(slide9, "Database Schema & Relational Data Models")

add_card(slide9, 0.8, 1.6, 11.7, 5.2, title="SQLite Database Schema (`./data/vintage-melodies.db`)")

# Schema Table
rows, cols = 4, 4
table_shape = slide9.shapes.add_table(rows, cols, Inches(1.0), Inches(2.2), Inches(11.3), Inches(4.2))
table = table_shape.table

# Set Column Widths
table.columns[0].width = Inches(2.2)
table.columns[1].width = Inches(4.5)
table.columns[2].width = Inches(2.3)
table.columns[3].width = Inches(2.3)

headers = ["Table Name", "Columns & Data Types", "Primary / Foreign Keys", "Description"]
data = [
    ("songs", "id (INTEGER), external_id (TEXT), title (TEXT), artist (TEXT), album (TEXT), artwork_url (TEXT), duration (INTEGER), media_url (TEXT), source_url (TEXT), created_at (DATETIME)", "PRIMARY KEY (id)", "Stores audio track metadata and Cloudflare R2 streaming URLs"),
    ("playlists", "id (INTEGER), name (TEXT), web_image (TEXT), mobile_image (TEXT), is_active (INTEGER), created_at (DATETIME), updated_at (DATETIME)", "PRIMARY KEY (id)", "Stores user-created & curated mood playlists with custom artwork"),
    ("playlist_songs", "id (INTEGER), playlist_id (INTEGER), song_id (INTEGER), sort_order (INTEGER), created_at (DATETIME)", "FOREIGN KEY (playlist_id)\nFOREIGN KEY (song_id)", "Junction table mapping songs to playlists with custom sort ordering")
]

for col_idx, text in enumerate(headers):
    cell = table.cell(0, col_idx)
    cell.fill.solid()
    cell.fill.fore_color.rgb = TABLE_HEADER_BG
    p = cell.text_frame.paragraphs[0]
    p.text = text
    p.font.bold = True
    p.font.size = Pt(12)
    p.font.color.rgb = GOLD_ACCENT

for row_idx, row_data in enumerate(data):
    for col_idx, text in enumerate(row_data):
        cell = table.cell(row_idx + 1, col_idx)
        cell.fill.solid()
        cell.fill.fore_color.rgb = TABLE_ROW_BG if row_idx % 2 == 0 else TABLE_ROW_ALT
        p = cell.text_frame.paragraphs[0]
        p.text = text
        p.font.size = Pt(10.5)
        p.font.color.rgb = TEXT_WHITE


# ---------------------------------------------------------
# SLIDE 10: TRANSITION TO NATIVE ANDROID — GOALS
# ---------------------------------------------------------
slide10 = prs.slides.add_slide(blank_layout)
set_slide_background(slide10)
add_header(slide10, "Transition to Native Android — Goals & Architecture Scope")

add_card(slide10, 0.8, 1.6, 5.7, 5.2, title="Key Motivation & Android Advantages")
add_bullet_list(slide10, 1.0, 2.2, 5.3, 4.4, [
    ("Unbreakable Background Service", "Native Android `MediaSessionService` guarantees playback never stops when screen locks or app is backgrounded."),
    ("Hardware Equalizer Access", "Direct integration with Android `AudioEffect.Equalizer` API for superior audio processing performance."),
    ("Offline Media Caching", "Seamlessly cache Cloudflare R2 audio tracks to local storage using ExoPlayer `SimpleCache`."),
    ("Native Android UI", "Build 100% Jetpack Compose UI maintaining exact vintage aesthetic with smooth 60fps animations."),
    ("System Notifications", "Rich media controls in Android Quick Settings, Lock Screen, Wear OS, and Android Auto.")
], font_size=12, space_after=10)

add_card(slide10, 6.8, 1.6, 5.7, 5.2, title="Target Android Architecture Blueprint")
diag_box2 = slide10.shapes.add_textbox(Inches(7.0), Inches(2.2), Inches(5.3), Inches(4.4))
tf_d2 = diag_box2.text_frame
tf_d2.word_wrap = True

a_lines = [
    ("UI Layer (Jetpack Compose)", "Vintage Theme • Glass Cards • Equalizer View • Mood Grid"),
    ("ViewModel & State Layer", "Kotlin `StateFlow` • UI State Management • Event Dispatching"),
    ("Domain Layer", "Use Cases: `PlayTrackUseCase`, `SyncCloudR2UseCase`, `ManagePlaylists`"),
    ("Data Layer (Repositories)", "SongRepository • PlaylistRepository • EqualizerRepository"),
    ("Local & Remote Data Sources", "Room Database (Local SQLite) ↔ Cloudflare R2 S3 SDK (Remote)"),
    ("Audio Engine Core", "AndroidX Media3 ExoPlayer • `MediaSessionService` • Foreground Service")
]

for layer, detail in a_lines:
    p1 = tf_d2.add_paragraph()
    p1.text = "▪ " + layer
    p1.font.bold = True
    p1.font.size = Pt(12)
    p1.font.color.rgb = AMBER_ACCENT
    p2 = tf_d2.add_paragraph()
    p2.text = "   " + detail
    p2.font.size = Pt(10.5)
    p2.font.color.rgb = TEXT_WHITE
    p2.space_after = Pt(6)


# ---------------------------------------------------------
# SLIDE 11: ANDROID ARCHITECTURE & TECH STACK
# ---------------------------------------------------------
slide11 = prs.slides.add_slide(blank_layout)
set_slide_background(slide11)
add_header(slide11, "Android Technology Stack & Specifications")

android_cards = [
    (0.8, 1.6, 5.7, 2.5, "Core Framework & Architecture", [
        ("Language", "100% Kotlin with Coroutines & Asynchronous `Flow`"),
        ("Architecture", "MVVM + Clean Architecture (UI, Domain, Data layers)"),
        ("Dependency Injection", "Hilt (Google recommended DI framework for Android)"),
        ("Minimum SDK", "Android 7.0 (API level 24) • Target SDK 35 (Android 15)")
    ]),
    (6.8, 1.6, 5.7, 2.5, "UI Framework & Styling", [
        ("Jetpack Compose", "Modern declarative UI toolkit with Material 3 base"),
        ("Custom Vintage Theme", "Dark Espresso background, Amber accents, Glassmorphic cards"),
        ("Image Loading", "Coil (Coroutines-backed image loader for compose)"),
        ("Navigation", "Jetpack Compose Navigation with type-safe routes")
    ]),
    (0.8, 4.3, 5.7, 2.5, "Audio Engine & Media", [
        ("Media Framework", "AndroidX Media3 ExoPlayer (`androidx.media3:media3-exoplayer`)"),
        ("Media Session", "Media3 `MediaSessionService` for background playback service"),
        ("Audio FX", "Native Android `AudioEffect.Equalizer` 5-band frequency control"),
        ("Notification Manager", "Custom `PlayerNotificationManager` for lock-screen controls")
    ]),
    (6.8, 4.3, 5.7, 2.5, "Database, Networking & Cloud", [
        ("Local Persistence", "Room Database (`androidx.room`) mirroring web SQLite schema"),
        ("Cloudflare R2 Integration", "AWS SDK for Kotlin (`aws.sdk.kotlin:s3`) / Ktor HTTP Client"),
        ("Background Jobs", "WorkManager (`androidx.work`) for periodic cloud bucket sync"),
        ("Disk Cache", "ExoPlayer `SimpleCache` for offline audio track playback")
    ])
]

for left, top, width, height, title, items in android_cards:
    add_card(slide11, left, top, width, height, title=title)
    add_bullet_list(slide11, left + 0.2, top + 0.6, width - 0.4, height - 0.7, items, font_size=11, space_after=4)


# ---------------------------------------------------------
# SLIDE 12: ANDROID AUDIO ENGINE DESIGN (MEDIA3)
# ---------------------------------------------------------
slide12 = prs.slides.add_slide(blank_layout)
set_slide_background(slide12)
add_header(slide12, "Android Audio Engine Design (Media3 / ExoPlayer)")

add_card(slide12, 0.8, 1.6, 5.7, 5.2, title="Media3 Foreground Service Architecture")
add_bullet_list(slide12, 1.0, 2.2, 5.3, 4.4, [
    ("PlaybackService Implementation", "Extends `MediaSessionService` to run ExoPlayer inside a foreground service with active notification."),
    ("Audio Focus Management", "Automatically handles `AUDIOFOCUS_GAIN` and `AUDIOFOCUS_LOSS` (pauses playback during phone calls or navigation directions)."),
    ("Progressive MP3 Streaming", "ExoPlayer handles HTTP range requests to stream audio seamlessly from Cloudflare R2 CDN."),
    ("Lock-Screen & Notification UI", "Connects to Android System Media Controls, supporting Play, Pause, Next, Previous, and Seek actions.")
], font_size=12, space_after=8)

add_card(slide12, 6.8, 1.6, 5.7, 5.2, title="Media3 Service Code Implementation Plan")

code_card2 = slide12.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(7.0), Inches(2.2), Inches(5.3), Inches(4.3))
code_card2.fill.solid()
code_card2.fill.fore_color.rgb = CODE_BG
code_card2.line.color.rgb = CARD_BORDER

tb_code2 = slide12.shapes.add_textbox(Inches(7.1), Inches(2.3), Inches(5.1), Inches(4.1))
tf_code2 = tb_code2.text_frame
tf_code2.word_wrap = True

code_lines2 = [
    "// android/service/PlaybackService.kt",
    "class PlaybackService : MediaSessionService() {",
    "  private var mediaSession: MediaSession? = null",
    "  override fun onCreate() {",
    "    super.onCreate()",
    "    val player = ExoPlayer.Builder(this)",
    "      .setAudioAttributes(AudioAttributes.DEFAULT, true)",
    "      .setHandleAudioBecomingNoisy(true)",
    "      .build()",
    "    mediaSession = MediaSession.Builder(this, player).build()",
    "  }",
    "  override fun onGetSession(controllerInfo: ...): MediaSession? {",
    "    return mediaSession",
    "  }",
    "}"
]

for cl in code_lines2:
    pc = tf_code2.add_paragraph()
    pc.text = cl
    pc.font.name = "Consolas"
    pc.font.size = Pt(9.5)
    pc.font.color.rgb = AMBER_ACCENT if cl.startswith("//") or "class" in cl else TEXT_WHITE


# ---------------------------------------------------------
# SLIDE 13: ANDROID DATABASE & CLOUD SYNC STRATEGY
# ---------------------------------------------------------
slide13 = prs.slides.add_slide(blank_layout)
set_slide_background(slide13)
add_header(slide13, "Android Database Architecture & Cloud Sync Strategy")

add_card(slide13, 0.8, 1.6, 5.7, 5.2, title="Room Database Architecture")
add_bullet_list(slide13, 1.0, 2.2, 5.3, 4.4, [
    ("Local SQLite Mirror", "Room DB schema directly mirrors web project SQLite tables (`SongEntity`, `PlaylistEntity`, `PlaylistSongCrossRef`)."),
    ("Reactive Flow Queries", "Room DAOs expose Kotlin `Flow<List<SongEntity>>` for real-time automatic UI updates when data changes."),
    ("Offline Cache Engine", "ExoPlayer `SimpleCache` with `LeastRecentlyUsedCacheEvictor` caches played Cloudflare R2 tracks to device storage."),
    ("Zero Connectivity Playback", "Users can play previously streamed or cached tracks even when completely offline.")
], font_size=12, space_after=8)

add_card(slide13, 6.8, 1.6, 5.7, 5.2, title="Cloudflare R2 Sync Engine (WorkManager)")
add_bullet_list(slide13, 7.0, 2.2, 5.3, 4.4, [
    ("AWS Kotlin S3 SDK Client", "Direct S3 API connection to Cloudflare R2 endpoint using `S3Client` in Kotlin."),
    ("Automated Metadata Parser", "Ported string normalization logic cleaning track numbers and website tags from filenames."),
    ("Periodic Background Sync", "Android `WorkManager` runs background jobs to query Cloudflare R2 and update local Room DB."),
    ("Delta Synchronization", "Only inserts new or updated R2 objects into Room DB to minimize network and battery usage.")
], font_size=12, space_after=8)


# ---------------------------------------------------------
# SLIDE 14: UI/UX MAPPING — WEB TO JETPACK COMPOSE
# ---------------------------------------------------------
slide14 = prs.slides.add_slide(blank_layout)
set_slide_background(slide14)
add_header(slide14, "UI/UX Mapping — Web Components to Jetpack Compose")

add_card(slide14, 0.8, 1.6, 11.7, 5.2, title="UI Element Transformation Blueprint")

rows, cols = 6, 3
table_shape2 = slide14.shapes.add_table(rows, cols, Inches(1.0), Inches(2.2), Inches(11.3), Inches(4.2))
table2 = table_shape2.table

table2.columns[0].width = Inches(3.2)
table2.columns[1].width = Inches(4.3)
table2.columns[2].width = Inches(3.8)

headers2 = ["Web UI Component", "Jetpack Compose Equivalent", "Design & Animation Notes"]
data2 = [
    ("Full-screen Vintage Background", "Box + Image(ContentScale.Crop) + Surface(color = DarkOverlay)", "Warm dark espresso overlay over retro village graphics"),
    ("Translucent Glass Player Bar", "Surface(color = CardBg, border = AmberBorder, shape = RoundedCornerShape(16.dp))", "Glassmorphic elevation, vinyl rotating infinite rotation modifier"),
    ("Web Audio 5-Band Equalizer", "ModalBottomSheet + Custom Slider Canvas + EqualizerVisualizerBars", "5 interactive sliders connected to Android AudioEffect.Equalizer"),
    ("Mobile Bottom Navigation", "NavigationBar + NavigationBarItem(selectedIcon, label)", "Custom dark gold styling matching web mobile bottom nav"),
    ("Slide-out Playlist Drawer", "ModalNavigationDrawer / AnimatedVisibility(slideInHorizontally)", "Smooth slide-in drawer showing mood playlists and song lists")
]

for col_idx, text in enumerate(headers2):
    cell = table2.cell(0, col_idx)
    cell.fill.solid()
    cell.fill.fore_color.rgb = TABLE_HEADER_BG
    p = cell.text_frame.paragraphs[0]
    p.text = text
    p.font.bold = True
    p.font.size = Pt(12)
    p.font.color.rgb = GOLD_ACCENT

for row_idx, row_data in enumerate(data2):
    for col_idx, text in enumerate(row_data):
        cell = table2.cell(row_idx + 1, col_idx)
        cell.fill.solid()
        cell.fill.fore_color.rgb = TABLE_ROW_BG if row_idx % 2 == 0 else TABLE_ROW_ALT
        p = cell.text_frame.paragraphs[0]
        p.text = text
        p.font.size = Pt(10.5)
        p.font.color.rgb = TEXT_WHITE


# ---------------------------------------------------------
# SLIDE 15: ANDROID SECURITY & STATE MANAGEMENT
# ---------------------------------------------------------
slide15 = prs.slides.add_slide(blank_layout)
set_slide_background(slide15)
add_header(slide15, "Android Security, Auth & State Management")

add_card(slide15, 0.8, 1.6, 5.7, 5.2, title="State Management Strategy")
add_bullet_list(slide15, 1.0, 2.2, 5.3, 4.4, [
    ("Unidirectional Data Flow (UDF)", "UI emits User Events → ViewModel processes logic → Emits updated immutable `StateFlow` to Compose UI."),
    ("Sealed UI State Classes", "Modeled using Kotlin sealed interfaces: `PlayerUiState.Loading`, `PlayerUiState.Success`, `PlayerUiState.Error`."),
    ("Single Source of Truth", "Room DB acts as single source of truth; UI reacts automatically to Room `Flow` updates."),
    ("Process Death Resilience", "SavedStateHandle preserves player position and active playlist during Android activity destruction.")
], font_size=12, space_after=10)

add_card(slide15, 6.8, 1.6, 5.7, 5.2, title="Security & Admin Credentials")
add_bullet_list(slide15, 7.0, 2.2, 5.3, 4.4, [
    ("Encrypted Storage", "Admin credentials & session tokens stored securely via Jetpack `EncryptedSharedPreferences` / Proto DataStore."),
    ("Cloudflare R2 Keys", "R2 Access Keys encrypted at rest or fetched via secure remote config rather than hardcoded in source."),
    ("HTTPS Certificate Pinning", "Ensures secure TLS communication with Cloudflare R2 storage endpoints."),
    ("Admin Privilege Scope", "Admin features (R2 song picker, bulk playlist edit) secured behind PIN / Password prompt dialog.")
], font_size=12, space_after=10)


# ---------------------------------------------------------
# SLIDE 16: FEATURE MAPPING MATRIX (WEB VS ANDROID)
# ---------------------------------------------------------
slide16 = prs.slides.add_slide(blank_layout)
set_slide_background(slide16)
add_header(slide16, "Web vs Native Android Feature Comparison Matrix")

add_card(slide16, 0.8, 1.6, 11.7, 5.2, title="Feature Specification & Parity Analysis")

rows, cols = 7, 4
table_shape3 = slide16.shapes.add_table(rows, cols, Inches(1.0), Inches(2.2), Inches(11.3), Inches(4.2))
table3 = table_shape3.table

table3.columns[0].width = Inches(2.5)
table3.columns[1].width = Inches(3.6)
table3.columns[2].width = Inches(3.6)
table3.columns[3].width = Inches(1.6)

headers3 = ["Feature Capability", "Existing Web Implementation", "Native Android Implementation", "Status"]
data3 = [
    ("Background Playback", "HTML5 Audio + MediaSession JS (Throttled by OS)", "Media3 ExoPlayer + Foreground Service (Guaranteed)", "SUPERIOR"),
    ("Audio Equalizer", "Web Audio API BiquadFilterNode (5-Band)", "Android Native AudioEffect.Equalizer (5-Band)", "PARITY / ENHANCED"),
    ("Cloud Storage Stream", "Cloudflare R2 S3 SDK via Node.js Express Server", "AWS Kotlin S3 SDK / REST Client to Cloudflare R2", "PARITY"),
    ("Offline Track Caching", "Not Supported (Requires Active Internet Connection)", "ExoPlayer SimpleCache + Room Local DB Storage", "NEW FEATURE"),
    ("Sleep Timer", "JS setTimeout Countdown Timer", "Android AlarmManager / Foreground Service Timer", "ENHANCED"),
    ("Lock-Screen Controls", "Browser MediaSession API", "Native Quick Settings & Lockscreen Notification Media", "SUPERIOR")
]

for col_idx, text in enumerate(headers3):
    cell = table3.cell(0, col_idx)
    cell.fill.solid()
    cell.fill.fore_color.rgb = TABLE_HEADER_BG
    p = cell.text_frame.paragraphs[0]
    p.text = text
    p.font.bold = True
    p.font.size = Pt(11)
    p.font.color.rgb = GOLD_ACCENT

for row_idx, row_data in enumerate(data3):
    for col_idx, text in enumerate(row_data):
        cell = table3.cell(row_idx + 1, col_idx)
        cell.fill.solid()
        cell.fill.fore_color.rgb = TABLE_ROW_BG if row_idx % 2 == 0 else TABLE_ROW_ALT
        p = cell.text_frame.paragraphs[0]
        p.text = text
        p.font.size = Pt(10)
        p.font.color.rgb = AMBER_ACCENT if col_idx == 3 else TEXT_WHITE
        p.font.bold = (col_idx == 3)


# ---------------------------------------------------------
# SLIDE 17: IMPLEMENTATION ROADMAP & PHASING PLAN
# ---------------------------------------------------------
slide17 = prs.slides.add_slide(blank_layout)
set_slide_background(slide17)
add_header(slide17, "Android Development Implementation Roadmap")

phases = [
    ("Phase 1: Foundation & Audio Core", "Weeks 1 - 2", [
        "Initialize Android Studio project with Jetpack Compose",
        "Implement Media3 ExoPlayer & MediaSessionService",
        "Configure Cloudflare R2 S3 audio streaming client",
        "Build basic notification & background playback service"
    ]),
    ("Phase 2: Database & R2 Sync Engine", "Weeks 3 - 4", [
        "Setup Room Database schema (Songs, Playlists, Junction)",
        "Build Cloudflare R2 recursive object parser",
        "Implement WorkManager for background cloud sync",
        "Setup ExoPlayer SimpleCache for offline audio caching"
    ]),
    ("Phase 3: Vintage UI & Equalizer", "Weeks 5 - 6", [
        "Build custom Jetpack Compose Vintage Glass Theme",
        "Implement 5-Band Equalizer UI & AudioEffect integration",
        "Develop Sleep Timer modal & countdown service",
        "Implement Slide-out Playlist Drawer & Mood Grid"
    ]),
    ("Phase 4: Admin Features & Release", "Weeks 7 - 8", [
        "Build Admin Authentication & Security Layer",
        "Implement Cloud R2 Song Picker & Bulk Add dialog",
        "Conduct performance tuning, battery & memory QA",
        "Build signed Release APK & Google Play AAB bundle"
    ])
]

for idx, (title, duration, items) in enumerate(phases):
    left = 0.8 + (idx % 2) * 5.9
    top = 1.6 + (idx // 2) * 2.7
    add_card(slide17, left, top, 5.7, 2.5, title=title)
    
    # Duration badge
    tb_d = slide17.shapes.add_textbox(Inches(left + 3.8), Inches(top + 0.15), Inches(1.7), Inches(0.4))
    p_d = tb_d.text_frame.paragraphs[0]
    p_d.text = duration
    p_d.font.size = Pt(11)
    p_d.font.bold = True
    p_d.font.color.rgb = AMBER_ACCENT
    p_d.alignment = PP_ALIGN.RIGHT
    
    add_bullet_list(slide17, left + 0.2, top + 0.65, 5.3, 1.7, items, font_size=10.5, space_after=3)


# ---------------------------------------------------------
# SLIDE 18: TECHNICAL REFERENCE & APPENDIX
# ---------------------------------------------------------
slide18 = prs.slides.add_slide(blank_layout)
set_slide_background(slide18)
add_header(slide18, "Technical Reference & Appendix")

add_card(slide18, 0.8, 1.6, 5.7, 5.2, title="Cloudflare R2 Bucket Reference")
add_bullet_list(slide18, 1.0, 2.2, 5.3, 4.4, [
    ("Account ID", "e3fc7139f24a8746c04bca71c7b0b400"),
    ("Bucket Name", "vintage-melodies"),
    ("S3 Endpoint", "https://e3fc7139f24a8746c04bca71c7b0b400.r2.cloudflarestorage.com"),
    ("Public CDN Domain", "https://pub-e7f4f743d98b43b89671fe068ee1ee05.r2.dev"),
    ("Audio File Formats", ".mp3, .m4a, .wav, .flac, .aac, .ogg, .opus")
], font_size=11, space_after=8)

add_card(slide18, 6.8, 1.6, 5.7, 5.2, title="Web API Endpoints Blueprint")
add_bullet_list(slide18, 7.0, 2.2, 5.3, 4.4, [
    ("POST /api/auth/login", "Authenticate Admin user session"),
    ("POST /api/auth/logout", "Terminate Admin user session"),
    ("GET /api/auth/status", "Verify current session admin privileges"),
    ("GET /api/cloud-r2/songs", "Fetch all audio tracks from Cloudflare R2 bucket"),
    ("GET /api/playlists", "Fetch all playlists & track counts"),
    ("POST /api/playlists", "Create new playlist (Admin only)"),
    ("GET /api/playlists/:id", "Fetch playlist details and contained songs"),
    ("POST /api/playlists/:id/songs", "Add song(s) to playlist (Admin only)"),
    ("DELETE /api/playlists/:id/songs/:songId", "Remove song from playlist (Admin only)")
], font_size=10.5, space_after=5)

# Save presentation
prs.save(OUTPUT_FILE)
print(f"SUCCESS: PowerPoint presentation saved to {OUTPUT_FILE}")
