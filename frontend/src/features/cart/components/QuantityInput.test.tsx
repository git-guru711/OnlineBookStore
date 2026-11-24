import { screen } from '@testing-library/react';
import { QuantityInput } from '@/features/cart/components/QuantityInput';
import { renderWithProviders } from '@/testing/test-utils';

function renderQuantity({ value = 2, available = 10 }: { value?: number; available?: number } = {}) {
  const onChange = vi.fn<(quantity: number) => void>();
  const result = renderWithProviders(
    <QuantityInput title="Clean Code" value={value} available={available} onChange={onChange} />,
  );
  return { ...result, onChange };
}

describe('QuantityInput', () => {
  it('shows the quantity in a field labelled with the book title', () => {
    renderQuantity({ value: 2 });

    expect(screen.getByRole('textbox', { name: 'Quantity of Clean Code' })).toHaveValue('2');
  });

  it('asks for one more with "+" and one less with "−"', async () => {
    const { user, onChange } = renderQuantity({ value: 2 });

    await user.click(screen.getByRole('button', { name: 'Increase quantity of Clean Code' }));
    await user.click(screen.getByRole('button', { name: 'Decrease quantity of Clean Code' }));

    expect(onChange.mock.calls).toEqual([[3], [1]]);
  });

  it('cannot go below 1 (removing a book is a separate action)', () => {
    renderQuantity({ value: 1 });

    expect(screen.getByRole('button', { name: 'Decrease quantity of Clean Code' })).toBeDisabled();
  });

  it('cannot go above the copies available', () => {
    renderQuantity({ value: 3, available: 3 });

    expect(screen.getByRole('button', { name: 'Increase quantity of Clean Code' })).toBeDisabled();
  });

  it('cannot go above 99, the most the backend accepts per book, even with more copies available', () => {
    renderQuantity({ value: 99, available: 150 });

    expect(screen.getByRole('button', { name: 'Increase quantity of Clean Code' })).toBeDisabled();
  });

  it('still lets the user reduce a quantity that is above the copies available', () => {
    renderQuantity({ value: 3, available: 1 }); // stock went down after the books were added

    expect(screen.getByRole('button', { name: 'Decrease quantity of Clean Code' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Increase quantity of Clean Code' })).toBeDisabled();
  });

  it('asks for a typed quantity when the field is left', async () => {
    const { user, onChange } = renderQuantity({ value: 2, available: 10 });
    const field = screen.getByRole('textbox', { name: 'Quantity of Clean Code' });

    await user.clear(field);
    await user.type(field, '5');
    await user.tab();

    expect(onChange).toHaveBeenCalledExactlyOnceWith(5);
  });

  it.each([
    ['0', 'below 1'],
    ['abc', 'not a number'],
    ['11', 'more than the 10 available'],
  ])('ignores a typed "%s" (%s) and shows the old quantity again', async (typed) => {
    const { user, onChange } = renderQuantity({ value: 2, available: 10 });
    const field = screen.getByRole('textbox', { name: 'Quantity of Clean Code' });

    await user.clear(field);
    await user.type(field, typed);
    await user.tab();

    expect(onChange).not.toHaveBeenCalled();
    expect(field).toHaveValue('2');
  });
});
