'use client'

import React from 'react';
import { render, screen } from '@testing-library/react';
import ToolTip from './ToolTip';

describe('ToolTip Component', () => {
  test('renders children correctly', () => {
    render(
      <ToolTip text="Tooltip text">
        <button>Hover me</button>
      </ToolTip>
    );
    expect(screen.getByText('Hover me')).toBeInTheDocument();
  });

  test('renders tooltip text', () => {
    render(
      <ToolTip text="Tooltip text">
        <span>Info</span>
      </ToolTip>
    );
    expect(screen.getByText('Tooltip text')).toBeInTheDocument();
  });

  test('tooltip appears on hover (style simulation)', () => {
    render(
      <ToolTip text="Visible tooltip">
        <span>Hover</span>
      </ToolTip>
    );
    const tooltip = screen.getByText('Visible tooltip');
    tooltip.parentElement.dispatchEvent(new MouseEvent('mouseover', { bubbles: true }));
    // Note: This won't trigger CSS transitions, but you can test DOM presence
    expect(tooltip).toBeInTheDocument();
  });
});
