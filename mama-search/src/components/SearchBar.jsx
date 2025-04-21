import { useState } from "react";

const SearchBar = ({ onSearch }) => {
  const [query, setQuery] = useState("");

  const handleInputChange = (event) => {
    setQuery(event.target.value);
  };

  const handleKeyDown = (event) => {
    if (event.key === "Enter") {
      onSearch(query);
    }
  };

  return (
    <input
      type="text"
      value={query}
      onChange={handleInputChange}
      onKeyDown={handleKeyDown}
      placeholder="Search..."
      className="border border-gray-300 rounded p-2 w-full"
    />
  );
}

export default SearchBar;