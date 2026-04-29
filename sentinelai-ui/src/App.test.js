import { render, screen } from '@testing-library/react';
import App from './App';

test('renders SentinelAI UI', () => {
  render(<App />);
  expect(screen.getByText(/SentinelAI Log RCA/i)).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /Analyze Log/i })).toBeInTheDocument();
});
