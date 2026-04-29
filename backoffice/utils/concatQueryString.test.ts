import { concatQueryString } from './concatQueryString';

describe('concatQueryString', () => {
  it('should return original url when array is empty', () => {
    expect(concatQueryString([], 'https://example.com/api')).toBe('https://example.com/api');
  });

  it('should append query string with ? for the first element', () => {
    expect(concatQueryString(['a=1'], 'https://example.com/api')).toBe('https://example.com/api?a=1');
  });

  it('should append additional query parameters with &', () => {
    const url = concatQueryString(['a=1', 'b=2', 'c=3'], 'https://example.com/api');
    expect(url).toBe('https://example.com/api?a=1&b=2&c=3');
  });
});
