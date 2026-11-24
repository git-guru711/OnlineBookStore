import { render, screen } from '@testing-library/react';
import { App } from '@/app/App';

describe('App', () => {
  it('renders the site header with the store name', () => {
    render(<App />);

    const banner = screen.getByRole('banner');
    expect(banner).toHaveTextContent('Bookstore');
  });

  it('renders a main content region', () => {
    render(<App />);

    expect(screen.getByRole('main')).toBeInTheDocument();
  });
});
