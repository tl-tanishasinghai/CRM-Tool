'use client'

import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import Accordion from './Accordion';
import { MemoryRouter } from 'react-router-dom';

// Mock the carret image
jest.mock('../../assets/images/carretDown.svg', () => 'mocked-carret.svg');

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

describe('Accordion Component', () => {
  const mockAccordionList = [
    {
      title: 'What is Trillion Loans?',
      content: 'Trillion Loans is a lending platform.',
    },
    {
      title: 'How to apply?',
      content: 'Fill out the online application form.',
    },
  ];

  test('renders FAQ header', () => {
    render(<Accordion accordionList={mockAccordionList} />, { wrapper: MemoryRouter });
    expect(screen.getByText('FAQs')).toBeInTheDocument();
  });

  test('renders all accordion items', () => {
    render(<Accordion accordionList={mockAccordionList} />, { wrapper: MemoryRouter });
    mockAccordionList.forEach(item => {
      expect(screen.getByText(item.title)).toBeInTheDocument();
    });
  });

  test('toggles accordion content on click', () => {
    render(<Accordion accordionList={mockAccordionList} />, { wrapper: MemoryRouter });
    const firstItem = screen.getByText('What is Trillion Loans?');
    fireEvent.click(firstItem);
    expect(screen.getByText(/Trillion Loans is a lending platform/i)).toBeInTheDocument();

    fireEvent.click(firstItem);
    expect(screen.queryByText(/Trillion Loans is a lending platform/i)).not.toBeInTheDocument();
  });

  test('shows "View All FAQs" CTA when showAllFaqCta is true', () => {
    render(<Accordion accordionList={mockAccordionList} showAllFaqCta={true} />, { wrapper: MemoryRouter });
    const cta = screen.getByText('View All FAQs');
    expect(cta).toBeInTheDocument();
    fireEvent.click(cta);
    expect(mockNavigate).toHaveBeenCalledWith('/faqs');
  });

  test('does not show CTA when showAllFaqCta is false', () => {
    render(<Accordion accordionList={mockAccordionList} showAllFaqCta={false} />, { wrapper: MemoryRouter });
    expect(screen.queryByText('View All FAQs')).not.toBeInTheDocument();
  });
});
