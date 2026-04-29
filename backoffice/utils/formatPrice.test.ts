import { formatPriceUSD, formatPriceVND } from './formatPrice';

describe('formatPrice', () => {
  it('should format VND currency correctly', () => {
    const formatted = formatPriceVND(1000);
    expect(formatted).toContain('₫');
    expect(formatted).toMatch(/1\.000\s*₫/);
  });

  it('should format USD currency correctly', () => {
    const formatted = formatPriceUSD(1000);
    expect(formatted).toContain('$');
    expect(formatted).toMatch(/\$1,000\.00/);
  });
});
