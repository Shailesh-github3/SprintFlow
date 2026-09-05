import { useState, useEffect } from 'react';

// Delays propagating a fast-changing value (e.g. search input) until it
// stays unchanged for `delay` ms — used to avoid firing one API request
// per keystroke.
export const useDebounce = (value, delay = 400) => {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);

  return debounced;
};
