import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
// fonts are bundled: no third-party font server
import '@fontsource-variable/playfair-display';
import '@fontsource-variable/source-sans-3';
import { App } from '@/app/App';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
