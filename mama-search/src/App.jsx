import { useState } from 'react';
import SearchBar from './components/SearchBar.jsx';

function App() {
  const [searchResults, setSearchResults] = useState({
    data: [],
    time: 0,
    query: '',
    error: null,
  });
  const [currentPage, setCurrentPage] = useState(1);

  const resultsPerPage = 10;

  const onSearch = async (query) => {
    try {
      const encodedQuery = encodeURIComponent(query);
      const response = await fetch(`http://localhost:8080/api/search?query=${encodedQuery}`);

      if (!response.ok) {
        throw new Error(`Network response was not ok: ${response.status} ${response.statusText}`);
      }
      const data = await response.json();
      console.log('Data:', data);

      setSearchResults({
        data: data.data || [],
        time: data.time || 0,
        query: query,
        error: null,
      });
      setCurrentPage(1);
    } catch (error) {
      console.error('There was a problem with the fetch operation:', error);
      setSearchResults({
        data: [],
        time: 0,
        query: query,
        error: error.message,
      });
      setCurrentPage(1);
    }
  };

  const totalResults = searchResults.data.length;
  const totalPages = Math.ceil(totalResults / resultsPerPage);
  const startIndex = (currentPage - 1) * resultsPerPage;
  const endIndex = Math.min(startIndex + resultsPerPage, totalResults);
  const currentResults = searchResults.data.slice(startIndex, endIndex);

  const handlePageChange = (page) => {
    setCurrentPage(page);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // Function to render snippet with HTML tags (e.g., <b> for bold)
  const renderSnippet = (snippet) => {
    return <span dangerouslySetInnerHTML={{ __html: snippet }} />;
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="flex justify-center pt-10">
        <div className="w-full max-w-2xl">
          <SearchBar onSearch={onSearch} />
        </div>
      </div>

      <div className="max-w-3xl mx-auto mt-6 px-4">
        {searchResults.error ? (
          <p className="text-red-500">Error: {searchResults.error}</p>
        ) : searchResults.data.length > 0 ? (
          <>
            <p className="text-gray-600 text-sm mb-4">
              About {totalResults} results (
              {(searchResults.time / 1000).toFixed(2)} seconds)
            </p>

            <div className="space-y-6">
              {currentResults.map((doc, index) => (
                <div key={startIndex + index} className="result">
                  <h3>
                    <a
                      href={doc.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-blue-600 text-lg hover:underline"
                    >
                      {doc.title}
                    </a>
                  </h3>
                  <p className="text-green-700 text-sm">{doc.url}</p>
                  <p className="text-gray-700 text-sm">
                    {renderSnippet(doc.snippet)}
                  </p>
                </div>
              ))}
            </div>

            {totalPages > 1 && (
              <div className="flex justify-center mt-8">
                <div className="flex items-center space-x-2">
                  {currentPage > 1 && (
                    <button
                      onClick={() => handlePageChange(currentPage - 1)}
                      className="text-blue-600 hover:underline px-3 py-1"
                    >
                      Previous
                    </button>
                  )}

                  {[...Array(Math.min(totalPages, 10))].map((_, i) => {
                    const page = i + 1;
                    return (
                      <button
                        key={page}
                        onClick={() => handlePageChange(page)}
                        className={`px-3 py-1 rounded-full ${
                          currentPage === page
                            ? 'text-blue-600 font-bold'
                            : 'text-blue-600 hover:underline'
                        }`}
                      >
                        {page}
                      </button>
                    );
                  })}

                  {currentPage < totalPages && (
                    <button
                      onClick={() => handlePageChange(currentPage + 1)}
                      className="text-blue-600 hover:underline px-3 py-1"
                    >
                      Next
                    </button>
                  )}
                </div>
              </div>
            )}
          </>
        ) : searchResults.query ? (
          <p className="text-gray-600">No results found for "{searchResults.query}".</p>
        ) : null}
      </div>
    </div>
  );
}

export default App;