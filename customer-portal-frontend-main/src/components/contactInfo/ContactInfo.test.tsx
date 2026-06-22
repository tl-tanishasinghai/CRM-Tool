'use client'

import React from 'react';
import { render, screen } from '@testing-library/react';
import ContactInfo from './ContactInfo';

// Mock image imports
jest.mock('../../assets/images/phone-icon.svg', () => 'mocked-phone.svg');
jest.mock('../../assets/images/email-icon.svg', () => 'mocked-email.svg');

describe('ContactInfo Component', () => {
  beforeEach(() => {
    render(<ContactInfo />);
  });

  test('renders "Contact us" heading', () => {
    expect(screen.getByText(/Contact us/i)).toBeInTheDocument();
  });

  test('renders phone number label and value', () => {
    expect(screen.getByText(/Phone Number/i)).toBeInTheDocument();
    const phoneLink = screen.getByRole('link', { name: '022-477-90096' });
    expect(phoneLink).toHaveAttribute('href', 'tel:02247790096');
  });

  test('renders email label and value', () => {
    expect(screen.getByText(/Mail/i)).toBeInTheDocument();
    const emailLink = screen.getByRole('link', { name: 'customercare@trillionloans.com' });
    expect(emailLink).toHaveAttribute('href', 'mailto:customercare@trillionloans.com');
  });

  test('renders both contact icons', () => {
    const images = screen.getAllByRole('img');
    expect(images.length).toBeGreaterThanOrEqual(2);
    expect(images[0]).toHaveAttribute('src', 'mocked-phone.svg');
    expect(images[1]).toHaveAttribute('src', 'mocked-email.svg');
  });
});
