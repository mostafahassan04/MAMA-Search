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

  return (
    <div className="relative w-full">
      <input
        type="text"
        value={query}
        onChange={handleInputChange}
        onBlur={() => setSuggestions([])}
        onFocus={handleInputChange}
        onKeyDown={handleKeyDown}
        placeholder="Search..."
        className="border border-gray-300 rounded p-2 w-full"
      />

      {suggestions.length > 0 && (
        <ul className="absolute z-10 bg-white border border-gray-300 rounded mt-1 w-full max-h-60 overflow-y-auto">
          {suggestions.map((suggestion, index) => (
            <li
              key={index}
              className="p-2 hover:bg-gray-100 cursor-pointer"
              onClick={() => {
                setQuery(suggestion);
                setSuggestions([]);
                onSearch(suggestion);
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