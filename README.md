# MAMA Search

MAMA Search is a Java‑based search engine that demonstrates the core functionalities of a modern search platform — including web crawling, indexing, ranking, and user query processing. It features a modular design, multithreading, and a React‑powered web interface.

## Features

### 🌐 Web Crawler

* **Multithreaded** crawler with configurable thread count
* Respects `robots.txt` and skips duplicates or non‑HTML pages
* URL normalization and persistent crawl state
* Seed‑based queue system — crawled up to **6 000 pages**

### 🧠 Indexer

* Parses HTML content and stores data with positional and tag‑based metadata
* Custom, incremental schema for lightning‑fast updates
* Supports retrieval by word, stemmed forms, and tags (title, headers, body)

### 🔎 Query Processor & Phrase Search

* **Stemming support** (e.g., “travel” ⇒ “traveling”, “traveler”)
* **Phrase search** using quotes (e.g., `"machine learning"`)
* **Boolean logic** (up to two operations per query):

  * `"football player" OR "tennis player"`
  * `"machine learning" AND "AI"`
  * `"deep learning" NOT "CNN"`

### 📊 Ranker

* **TF‑IDF** for relevance scoring
* **PageRank** for measuring page importance
* Hybrid ranking: relevance × popularity

### ⚡ Performance & Caching

* Searches our **6 000‑document** index in **< 0.2 s** per query
* **Result caching** for instant responses on repeated queries

### 💻 Web Interface

* **React**‑powered frontend with real‑time auto‑suggestions
* Displays:

  * Page title
  * URL
  * Snippets with highlighted terms
* Shows search time and paginates results

---

## Technologies Used

* **Java**: Core logic (crawler, indexer, query processor, ranker)
* **MongoDB**: Index persistence & suggestion store
* **React**: Frontend UI (create‑react‑app)
* **HTML/CSS/JS**: Styling & interaction
* **Git & GitHub**: Version control
* **Agile**: Iterative development process

---

## Setup & Run

### 1. Clone the repo

```bash
git clone https://github.com/galelo04/MAMA-Search.git
cd MAMA-Search
```

### 2. Install dependencies

* **Frontend**:

  ```bash
  cd frontend
  npm install
  ```
* **Backend**:

  ```bash
  mvn install
  ```

### 3. Run the application

#### 🚀 Frontend

```bash
cd frontend
npm run dev
```

*Your React app will spin up at **[http://localhost:3000](http://localhost:3000)***

#### ⚙️ Backend API

Locate and run the `ServerAPI.java` file in `src/main/java/ServerAPI.java`:

* **IDE**: Open the project and run `ServerAPI` as a Java application
* **Command Line** (with Maven):

  ```bash
  mvn exec:java -Dexec.mainClass="ServerAPI"
  ```

*The API will start on **[http://localhost:8080](http://localhost:8080)** by default.*

---

Enjoy blazing‑fast search — and happy coding! 🚀
