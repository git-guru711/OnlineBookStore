export interface OrderItem {
  bookId: number;
  title: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export type OrderStatus = 'PLACED';

export interface Order {
  id: number;
  status: OrderStatus;
  items: OrderItem[];
  totalQuantity: number;
  totalAmount: number;
  createdAt: string;
}

export interface OrderSummary {
  id: number;
  status: OrderStatus;
  totalQuantity: number;
  totalAmount: number;
  createdAt: string;
}
