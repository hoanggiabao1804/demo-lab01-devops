import { getOrderStatusTitle, getDeliveryMethodTitle, getDeliveryStatusTitle } from './orderUtil';

// Mock enums since they might not be available in test environment
const EOrderStatus = {
  PENDING: 'PENDING',
  ACCEPTED: 'ACCEPTED',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED',
  PENDING_PAYMENT: 'PENDING_PAYMENT',
  PAID: 'PAID',
  REFUND: 'REFUND',
  SHIPPING: 'SHIPPING',
  REJECT: 'REJECT',
} as const;

const EDeliveryMethod = {
  GRAB_EXPRESS: 'GRAB_EXPRESS',
  VIETTEL_POST: 'VIETTEL_POST',
  SHOPEE_EXPRESS: 'SHOPEE_EXPRESS',
  YAS_EXPRESS: 'YAS_EXPRESS',
} as const;

const EDeliveryStatus = {
  CANCELLED: 'CANCELLED',
  DELIVERED: 'DELIVERED',
} as const;

describe('getOrderStatusTitle', () => {
  it('should return correct title for PENDING status', () => {
    const result = getOrderStatusTitle(EOrderStatus.PENDING);
    expect(result).toBe('Pending');
  });

  it('should return correct title for COMPLETED status', () => {
    const result = getOrderStatusTitle(EOrderStatus.COMPLETED);
    expect(result).toBe('Completed');
  });

  it('should return "All" for null status', () => {
    const result = getOrderStatusTitle(null);
    expect(result).toBe('All');
  });
});

describe('getDeliveryMethodTitle', () => {
  it('should return correct title for GRAB_EXPRESS', () => {
    const result = getDeliveryMethodTitle(EDeliveryMethod.GRAB_EXPRESS);
    expect(result).toBe('Grab Express');
  });

  it('should return "Preparing" for unknown method', () => {
    const result = getDeliveryMethodTitle('UNKNOWN' as any);
    expect(result).toBe('Preparing');
  });
});

describe('getDeliveryStatusTitle', () => {
  it('should return correct title for DELIVERED status', () => {
    const result = getDeliveryStatusTitle(EDeliveryStatus.DELIVERED);
    expect(result).toBe('Delivered');
  });

  it('should return correct title for CANCELLED status', () => {
    const result = getDeliveryStatusTitle(EDeliveryStatus.CANCELLED);
    expect(result).toBe('Cancelled');
  });
});