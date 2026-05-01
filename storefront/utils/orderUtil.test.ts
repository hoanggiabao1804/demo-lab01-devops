import { getOrderStatusTitle, getDeliveryMethodTitle, getDeliveryStatusTitle } from './orderUtil';

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
  DELIVERING: 'DELIVERING',
  PENDING: 'PENDING',
} as const;

describe('getOrderStatusTitle', () => {
  it('PENDING', () => {
    expect(getOrderStatusTitle(EOrderStatus.PENDING)).toBe('Pending');
  });

  it('ACCEPTED', () => {
    expect(getOrderStatusTitle(EOrderStatus.ACCEPTED)).toBe('Accepted');
  });

  it('COMPLETED', () => {
    expect(getOrderStatusTitle(EOrderStatus.COMPLETED)).toBe('Completed');
  });

  it('CANCELLED', () => {
    expect(getOrderStatusTitle(EOrderStatus.CANCELLED)).toBe('Cancelled');
  });

  it('PENDING_PAYMENT', () => {
    expect(getOrderStatusTitle(EOrderStatus.PENDING_PAYMENT)).toBe('Pending Payment');
  });

  it('PAID', () => {
    expect(getOrderStatusTitle(EOrderStatus.PAID)).toBe('Paid');
  });

  it('REFUND', () => {
    expect(getOrderStatusTitle(EOrderStatus.REFUND)).toBe('Refund');
  });

  it('SHIPPING', () => {
    expect(getOrderStatusTitle(EOrderStatus.SHIPPING)).toBe('Shipping');
  });

  it('REJECT', () => {
    expect(getOrderStatusTitle(EOrderStatus.REJECT)).toBe('Reject');
  });

  it('default (null)', () => {
    expect(getOrderStatusTitle(null)).toBe('All');
  });
});

describe('getDeliveryMethodTitle', () => {
  it('GRAB_EXPRESS', () => {
    expect(getDeliveryMethodTitle(EDeliveryMethod.GRAB_EXPRESS)).toBe('Grab Express');
  });

  it('VIETTEL_POST', () => {
    expect(getDeliveryMethodTitle(EDeliveryMethod.VIETTEL_POST)).toBe('Viettel Post');
  });

  it('SHOPEE_EXPRESS', () => {
    expect(getDeliveryMethodTitle(EDeliveryMethod.SHOPEE_EXPRESS)).toBe('Shopee Express');
  });

  it('YAS_EXPRESS', () => {
    expect(getDeliveryMethodTitle(EDeliveryMethod.YAS_EXPRESS)).toBe('Yas Express');
  });

  it('default', () => {
    expect(getDeliveryMethodTitle('UNKNOWN' as any)).toBe('Preparing');
  });
});

describe('getDeliveryStatusTitle', () => {
  it('DELIVERED', () => {
    expect(getDeliveryStatusTitle(EDeliveryStatus.DELIVERED)).toBe('Delivered');
  });

  it('CANCELLED', () => {
    expect(getDeliveryStatusTitle(EDeliveryStatus.CANCELLED)).toBe('Cancelled');
  });

  it('DELIVERING', () => {
    expect(getDeliveryStatusTitle(EDeliveryStatus.DELIVERING)).toBe('Delivering');
  });

  it('PENDING', () => {
    expect(getDeliveryStatusTitle(EDeliveryStatus.PENDING)).toBe('Pending');
  });

  it('default', () => {
    expect(getDeliveryStatusTitle('UNKNOWN' as any)).toBe('Preparing');
  });
});
