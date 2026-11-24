export type Role = 'USER';

export interface User {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  createdAt: string;
}
