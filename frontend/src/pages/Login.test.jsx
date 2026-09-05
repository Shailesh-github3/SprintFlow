import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import api from '../services/api';
import { AuthProvider } from '../contexts/AuthContext';
import Login from './Login';

// Note: submission is fired via fireEvent.submit because jsdom does not
// reliably perform submit-button activation inside this provider tree.
// Validation behavior (the subject under test) is identical either way.
vi.mock('../services/api', () => ({
  default: { post: vi.fn(), get: vi.fn() },
}));

const renderLogin = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <AuthProvider>
          <Login />
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>
  );
};

describe('Login', () => {
  it('shows a validation error for a malformed email without calling the API', async () => {
    const user = userEvent.setup();
    const { container } = renderLogin();

    await user.type(screen.getByLabelText('Email'), 'not-an-email');
    await user.type(screen.getByLabelText('Password'), 'longenoughpassword');
    fireEvent.submit(container.querySelector('form'));

    // Zod blocks the submit, so no API request fires — only the field error shows.
    expect(await screen.findByText('Invalid email address')).toBeInTheDocument();
    expect(api.post).not.toHaveBeenCalled();
  });
});
