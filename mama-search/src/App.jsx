import { useState } from 'react'
import SearchBar from './components/SearchBar.jsx'
function App() {
  const onSearch = async (query) => {
    await fetch(`http://localhost:8080/api/search?q=${query}`).then((response) => {
      if (response.ok) {
        return response.json()
      }
      throw new Error('Network response was not ok')
    }
    ).then((data) => {
      console.log(data)
    }).catch((error) => {
      console.error('There was a problem with the fetch operation:', error)
    })
    console.log("query: ", query)
  }
  return (
    <>
      <p className="text-red-500">text</p>
      <SearchBar onSearch={onSearch} />
    </>
  )
}

export default App
