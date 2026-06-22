'use client'

import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import Login from './Login';
import axios from 'axios';
import { MemoryRouter } from 'react-router-dom';

// Mock assets
jest.mock('../../assets/images/Trilliion-Loan_Light.png', () => 'logo.png');
jest.mock('../../assets/images/wallet.svg', () => 'wallet.svg');
jest.mock('../../assets/images/group_mobile.svg', () => 'mobile_wallet.svg');

// Mock axios
jest.mock('axios');

// Mock window.matchMedia for responsive logic
beforeAll(() => {
  window.matchMedia = jest.fn().mockImplementation(query => ({
    matches: query.includes('min-width'),
    media: query,
    onchange: null,
    addListener: jest.fn(),
    removeListener: jest.fn(),
  }));
});

describe('Login Component', () => {
  const setShowNavbar = jest.fn();
  const setFooter = jest.fn();
  const onLogin = jest.fn();

  beforeEach(() => {
    localStorage.clear();
  });

  test('renders mobile number input field', () => {
    render(
      <MemoryRouter>
        <Login setShowNavbar={setShowNavbar} setFooter={setFooter} onLogin={onLogin} />
      </MemoryRouter>
    );
    const input = screen.getByPlaceholderText('Mobile Number');
    expect(input).toBeInTheDocument();
  });

  test('disables OTP button when phone number is invalid', () => {
    render(
      <MemoryRouter>
        <Login setShowNavbar={setShowNavbar} setFooter={setFooter} onLogin={onLogin} />
      </MemoryRouter>
    );
    const btn = screen.getByText(/Get OTP/i);
    expect(btn).toBeDisabled();
  });

  test('enables OTP button when valid phone number is entered', () => {
    render(
      <MemoryRouter>
        <Login setShowNavbar={setShowNavbar} setFooter={setFooter} onLogin={onLogin} />
      </MemoryRouter>
    );
    fireEvent.change(screen.getByPlaceholderText('Mobile Number'), {
      target: { value: '9876543210' },
    });
    expect(screen.getByText(/Get OTP/i)).not.toBeDisabled();
  });

  test('shows error message when phone is unregistered (404)', async () => {
    axios.post.mockRejectedValueOnce({ response: { status: 404, data: { message: 'Not registered' } } });

    render(
      <MemoryRouter>
        <Login setShowNavbar={setShowNavbar} setFooter={setFooter} onLogin={onLogin} />
      </MemoryRouter>
    );
    fireEvent.change(screen.getByPlaceholderText('Mobile Number'), {
      target: { value: '9876543210' },
    });
    fireEvent.click(screen.getByText(/Get OTP/i));

    await waitFor(() => {
      expect(screen.getByText('Not registered')).toBeInTheDocument();
    });
  });

  test('moves to OTP view after valid phone number', async () => {
    axios.post.mockResolvedValueOnce({ data: {} });

    render(
      <MemoryRouter>
        <Login setShowNavbar={setShowNavbar} setFooter={setFooter} onLogin={onLogin} />
      </MemoryRouter>
    );
    fireEvent.change(screen.getByPlaceholderText('Mobile Number'), {
      target: { value: '9876543210' },
    });
    fireEvent.click(screen.getByText(/Get OTP/i));

    await waitFor(() => {
      expect(screen.getByText(/Enter OTP/i)).toBeInTheDocument();
    });
  });
});
