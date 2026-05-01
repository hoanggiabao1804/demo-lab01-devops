import { formatPrice } from './formatPrice';

describe('formatPrice', () => {
  it('should format USD currency correctly', () => {
    const formatted = formatPrice(1000);
    expect(formatted).toContain('$');
    expect(formatted).toMatch(/\$1,000\.00/);
  });

  it('should format decimal prices correctly', () => {
    const formatted = formatPrice(1234.56);
    expect(formatted).toMatch(/\$1,234\.56/);
  });

  it('should format zero correctly', () => {
    const formatted = formatPrice(0);
    expect(formatted).toMatch(/\$0\.00/);
  });
});
