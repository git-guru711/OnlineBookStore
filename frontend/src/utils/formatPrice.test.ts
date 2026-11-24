import { formatPrice } from '@/utils/formatPrice';

describe('formatPrice', () => {
  it.each([
    [32.99, '€32.99'],
    [0, '€0.00'],
    [65.98, '€65.98'],
    [42, '€42.00'],
    [1234.5, '€1,234.50'],
  ])('formats %s as %s', (amount, expected) => {
    expect(formatPrice(amount)).toBe(expected);
  });
});
