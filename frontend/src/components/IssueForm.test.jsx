import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import IssueForm from './IssueForm';

describe('IssueForm', () => {
  it('shows a validation error for a short title', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();

    render(
      <IssueForm open setOpen={() => {}} onSubmit={onSubmit} isLoading={false} />
    );

    await user.type(screen.getByLabelText('Title'), 'ab');
    await user.click(screen.getByRole('button', { name: 'Create Issue' }));

    expect(await screen.findByText('Title must be at least 3 characters')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('submits valid values including enum status/priority', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();

    render(
      <IssueForm open setOpen={() => {}} onSubmit={onSubmit} isLoading={false} />
    );

    await user.type(screen.getByLabelText('Title'), 'Fix login bug');
    await user.type(screen.getByLabelText('Description'), 'Users cannot log in');
    await user.type(screen.getByLabelText('Due Date'), '2026-12-31');
    await user.click(screen.getByRole('button', { name: 'Create Issue' }));

    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit.mock.calls[0][0]).toMatchObject({
      title: 'Fix login bug',
      status: 'TODO',
      priority: 'MEDIUM',
    });
  });
});
