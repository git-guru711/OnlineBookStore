const euro = new Intl.NumberFormat('en-IE', { style: 'currency', currency: 'EUR' });

export function formatPrice(amount: number): string {
  return euro.format(amount);
}
