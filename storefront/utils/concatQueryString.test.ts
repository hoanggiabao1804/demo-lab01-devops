import { concatQueryString } from './concatQueryString';

describe('concatQueryString', () => {
  it('should return original url when array is empty', () => {
    const result = concatQueryString([], 'https://example.com');
    expect(result).toBe('https://example.com');
  });

  it('should append query string with ? for the first element', () => {
    const result = concatQueryString(['param1=value1'], 'https://example.com');
    expect(result).toBe('https://example.com?param1=value1');
  });

  it('should append additional query parameters with &', () => {
    const result = concatQueryString(['param1=value1', 'param2=value2'], 'https://example.com');
    expect(result).toBe('https://example.com?param1=value1&param2=value2');
  });

  it('should handle multiple parameters correctly', () => {
    const result = concatQueryString(['a=1', 'b=2', 'c=3'], 'https://example.com/path');
    expect(result).toBe('https://example.com/path?a=1&b=2&c=3');
  });
});