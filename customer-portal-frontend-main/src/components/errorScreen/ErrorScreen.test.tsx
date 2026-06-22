'use client'

import React from 'react';
import { render, screen } from '@testing-library/react';
import ErrorScreen from './ErrorScreen'; // adjust path if needed

test('renders Try Again button', () => {
  render(<ErrorScreen logout={() => {}} />); // dummy logout function

  const button = screen.getByRole('button', { name: /try again/i });
  expect(button).toBeInTheDocument();
});