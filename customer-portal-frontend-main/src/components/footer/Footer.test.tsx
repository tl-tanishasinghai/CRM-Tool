'use client'

import React from 'react';
import { render, screen } from '@testing-library/react';
import Footer from './Footer';

// Mock image asset import
jest.mock('../../assets/images/Trilliion-Loan_Dark-p-800.png', () => 'mocked-logo.png');

describe('Footer Component', () => {
  beforeEach(() => {
    render(<Footer />);
  });

  test('renders footer container', () => {
    const footer = screen.getByRole('contentinfo');
    expect(footer).toBeInTheDocument();
  });

  test('renders logo image', () => {
    const logo = screen.getByRole('img');
    expect(logo).toHaveAttribute('src', 'mocked-logo.png');
    expect(logo).toHaveClass('footer-logo');
  });

  test('renders RBI registration text', () => {
    expect(screen.getByText(/RBI-registered NBFC/i)).toBeInTheDocument();
  });

  test('renders Lending Partners link', () => {
    const link = screen.getByText('Lending Partners');
    expect(link).toHaveAttribute('href', 'https://www.trillionloans.com/lending-partners.html');
  });

  test('renders Grievance Redressal Mechanism link', () => {
    expect(screen.getByText('Grievance Redressal Mechanism')).toBeInTheDocument();
  });

  test('renders Sachet link with correct target', () => {
    const sachetLink = screen.getByText('Sachet');
    expect(sachetLink).toHaveAttribute('href', 'https://sachet.rbi.org.in/');
    expect(sachetLink).toHaveAttribute('target', '_new');
  });

  test('renders Legal section title and links', () => {
    expect(screen.getByText('Legal')).toBeInTheDocument();
    expect(screen.getByText('Privacy Policy')).toHaveAttribute('href', expect.stringContaining('Privacy-Policy.pdf'));
    expect(screen.getByText('Disclaimer')).toBeInTheDocument();
  });

  test('renders bottom footer text', () => {
    expect(screen.getByText(/Trillionloans Fintech Private Limited/)).toBeInTheDocument();
    expect(screen.getByText(/All Rights Reserved/)).toBeInTheDocument();
  });
});
