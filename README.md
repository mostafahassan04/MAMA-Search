# MAMA Search

MAMA Search is a Java-based search engine project that demonstrates the core functionalities of a modern search engine — including web crawling, indexing, ranking, and user query processing. It features a modular design, multithreading, and a user-friendly web interface.

## Features

### 🌐 Web Crawler
- Multithreaded crawler with configurable thread count.
- Respects `robots.txt` and avoids duplicate or non-HTML pages.
- URL normalization and persistent crawl state.
- Crawled up to 6000 pages using a seed-based queue system.

### 🧠 Indexer
- Parses HTML content and stores indexed data with positional and tag-based metadata.
- Persistent storage using a custom schema for fast and incremental updates.
- Supports retrieval by word, stemmed forms, and tags (title, headers, body).

### 🔎 Query Processor & Phrase Search (with Bonus Features)
- Supports keyword search with stemming (e.g., "travel" matches "traveling", "traveler").
- Phrase search using quotation marks (e.g., `"machine learning"` matches exact phrase).
- Bonus: Boolean logic support:
  - `“football player” OR “tennis player”`
  - `“machine learning” AND “AI”`
  - `“deep learning” NOT “CNN”`
- Supports up to 2 Boolean operations per search.

### 📊 Ranker
- Uses TF-IDF for relevance scoring.
- Implements PageRank algorithm to measure page importance.
- Final ranking is a hybrid of relevance and popularity.

### 💻 Web Interface
- Search bar with real-time auto-suggestions based on previous queries.
- Results display includes:
  - Page title
  - URL
  - Snippets with highlighted query terms
- Shows search time and paginates results.

## Technologies Used
- **Java** (Core logic: crawler, indexer, query processor)
- **MongoDB** (For index persistence and suggestions)
- **HTML/CSS/JavaScript** (Web frontend)
- **Git** (Version control and collaboration)
- **Agile** (Used agile methodology for iterative development)

## Setup Instructions
1. Clone the repository:
   ```bash
   git clone https://github.com/galelo04/MAMA-Search.git
