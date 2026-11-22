import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import NotificationBell from './NotificationBell';
import api from '../services/api';

vi.mock('../services/api', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
  },
}));

describe('NotificationBell', () => {
  let queryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });
  });

  it('renders bell icon and displays unread badge when unreadCount > 0', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/notifications/unread-count') {
        return Promise.resolve({ data: { unreadCount: 3 } });
      }
      return Promise.resolve({ data: [] });
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <NotificationBell />
        </MemoryRouter>
      </QueryClientProvider>
    );

    expect(await screen.findByText('3')).toBeInTheDocument();
  });

  it('does not display badge when unreadCount is 0', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/notifications/unread-count') {
        return Promise.resolve({ data: { unreadCount: 0 } });
      }
      return Promise.resolve({ data: [] });
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <NotificationBell />
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.queryByText('0')).not.toBeInTheDocument();
    });
  });
});
