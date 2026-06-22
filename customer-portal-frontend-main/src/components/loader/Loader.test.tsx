'use client'

import React from 'react';
import { render } from '@testing-library/react';
import Loader from './Loader';

describe('Loader Component', () => {
  test('renders loader div with correct class', () => {
    const { container } = render(<Loader />);
    const loaderDiv = container.querySelector('.loader');
    expect(loaderDiv).toBeInTheDocument();
  });

  test('only contains one root loader element', () => {
    const { container } = render(<Loader />);
    const loaderElements = container.getElementsByClassName('loader');
    expect(loaderElements.length).toBe(1);
  });
});
