import { useState, type SubmitEvent } from 'react';
import IconButton from '@mui/material/IconButton';
import InputAdornment from '@mui/material/InputAdornment';
import TextField from '@mui/material/TextField';
import SearchIcon from '@mui/icons-material/Search';

interface BookSearchProps {
  value: string;
  /** Called with the trimmed text on submit; '' means show all. */
  onSearch: (text: string) => void;
}

/** Searches on submit, not on every key press. The parent remounts it (key) when the search changes from outside. */
export function BookSearch({ value, onSearch }: BookSearchProps) {
  const [text, setText] = useState(value);

  function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    onSearch(text.trim());
  }

  return (
    <form role="search" onSubmit={handleSubmit}>
      <TextField
        type="search"
        fullWidth
        placeholder="Search by title or author"
        value={text}
        onChange={(event) => setText(event.target.value)}
        slotProps={{
          htmlInput: { 'aria-label': 'Search books', maxLength: 100 }, // the backend accepts up to 100 characters
          input: {
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon aria-hidden />
              </InputAdornment>
            ),
            endAdornment: (
              <InputAdornment position="end">
                <IconButton type="submit" aria-label="Search" edge="end">
                  <SearchIcon />
                </IconButton>
              </InputAdornment>
            ),
          },
        }}
        sx={{ bgcolor: 'background.paper' }}
      />
    </form>
  );
}
