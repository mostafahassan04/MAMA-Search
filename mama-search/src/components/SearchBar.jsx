import { useState } from "react";

const SearchBar = ({ onSearch }) => {
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState([]);

  const handleInputChange = (event) => {
    const newQuery = event.target.value;
    setQuery(newQuery);

    fetch(`http://localhost:8080/api/suggest`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ query: newQuery }),
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error(`Network response was not ok: ${response.status} ${response.statusText}`);
        }
        return response.json();
      })
      .then((data) => {
        setSuggestions(data.suggestions || []);
        console.log("Suggestions:", data.suggestions);
      })
      .catch((error) => {
        console.error("There was a problem with the fetch operation:", error);
        setSuggestions([]);
      });
  };

  const handleKeyDown = (event) => {
    if (event.key === "Enter") {
      onSearch(query);
      setSuggestions([]);
    }
  };

  const handleSearchButtonClick = () => {
    onSearch(query);
    setSuggestions([]);
  };

  return (
    <div className="relative w-full flex">
      <input
        type="text"
        value={query}
        onChange={handleInputChange}
        onBlur={() => setTimeout(() => setSuggestions([]), 100)}
        onFocus={handleInputChange}
        onKeyDown={handleKeyDown}
        placeholder="Search..."
        className="border border-gray-300 rounded-l p-2 w-full"
      />
      <button
        onClick={handleSearchButtonClick}
        className="border border-gray-300 bg-blue-500 text-white rounded-r p-2 hover:bg-blue-600"
      >
        Search
      </button>

      {suggestions.length > 0 && (
        <ul className="absolute z-10 bg-white border border-gray-300 rounded mt-12 w-full max-h-60 overflow-y-auto pointer-events-auto">
          {suggestions.map((suggestion, index) => (
            <li
              key={index}
              className="p-2 hover:bg-gray-100 cursor-pointer pointer-events-auto"
              onClick={() => {
                setQuery(suggestion);
              }}
            >
              {suggestion}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default SearchBar;