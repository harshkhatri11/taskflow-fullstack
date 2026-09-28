import { Role } from './enums';

export interface UserResponse {
  id: number;
  name: string;
  email: string;
  role: Role;
  createdAt: string;
  managedProjectCount: number | null;
  assignedTaskCount: number | null;
}

export interface UserSummaryResponse {
  id: number;
  name: string;
  role: Role;
}

export interface UserRequest {
  name: string;
  email: string;
  password: string;
  role: Role;
}

export interface UserUpdateRequest {
  name: string;
  email: string;
  role: Role;
}
