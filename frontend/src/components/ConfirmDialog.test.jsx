import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ConfirmDialog from './ConfirmDialog';

describe('ConfirmDialog', () => {
  it('shows the title and calls onConfirm when confirmed', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    const setOpen = vi.fn();

    render(
      <ConfirmDialog
        open
        setOpen={setOpen}
        title="Delete project?"
        description="This cannot be undone."
        confirmLabel="Delete project"
        onConfirm={onConfirm}
      />
    );

    expect(screen.getByText('Delete project?')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Delete project' }));
    expect(onConfirm).toHaveBeenCalledTimes(1);
    expect(setOpen).toHaveBeenCalledWith(false);
  });

  it('closes without confirming on cancel', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    const setOpen = vi.fn();

    render(
      <ConfirmDialog open setOpen={setOpen} title="Delete?" onConfirm={onConfirm} />
    );

    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onConfirm).not.toHaveBeenCalled();
    expect(setOpen).toHaveBeenCalledWith(false);
  });
});
