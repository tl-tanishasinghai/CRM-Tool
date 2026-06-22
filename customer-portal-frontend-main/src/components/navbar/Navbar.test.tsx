'use client'

import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import Navbar from './Navbar';

// Mock image imports
jest.mock('../../assets/images/Trilliion-Loan_Light.png', () => 'mocked-logo.png');
jest.mock('../../assets/images/carretDown.svg', () => 'mocked-dropdown-icon.svg');

// Mock react-router-dom
jest.mock('react-router-dom', () => ({
  useNavigate: () => jest.fn()
}));

describe('Navbar Component', () => {
  const mockLogout = jest.fn();

  beforeEach(() => {
    render(<Navbar onLogout={mockLogout} />);
  });

  test('renders logo image', () => {
    const logo = screen.getByAltText(/logo/i);
    expect(logo).toBeInTheDocument();
    expect(logo).toHaveAttribute('src', 'mocked-logo.png');
  });

  test('renders hamburger menu button', () => {
    const hamburger = screen.getByText('☰');
    expect(hamburger).toBeInTheDocument();
  });

  test('toggles hamburger menu on click', () => {
    const hamburger = screen.getByText('☰');
    fireEvent.click(hamburger);
    expect(screen.getByText('✕')).toBeInTheDocument();
  });

  test('renders top-level nav items like "Home" and "About Us"', () => {
    expect(screen.getByText('Home')).toBeInTheDocument();
    expect(screen.getByText('About Us')).toBeInTheDocument();
    expect(screen.getByText('Products')).toBeInTheDocument();
    expect(screen.getByText('Contact Us')).toBeInTheDocument();
  });

  test('shows sub-menu items on hover over "Products"', () => {
    const products = screen.getByText('Products');
    fireEvent.mouseEnter(products);

    expect(screen.getByText('SME Loan')).toBeInTheDocument();
    expect(screen.getByText('Personal Loan')).toBeInTheDocument();
    expect(screen.getByText('Vehicle Loan')).toBeInTheDocument();

    fireEvent.mouseLeave(products);
  });

  test('calls onLogout when Logout is clicked', () => {
    const logoutLink = screen.getByText('Logout');
    fireEvent.click(logoutLink);
    expect(mockLogout).toHaveBeenCalled();
  });
});
