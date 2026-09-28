import { Role, TaskPriority, TaskStatus } from './enums';

export interface TaskProjectSummary {
  id: number;
  title: string;
}

export interface TaskAssigneeSummary {
  id: number;
  name: string;
  role: Role;
}

export interface TaskResponse {
  id: number;
  title: string;
  description: string;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate: string;
  project: TaskProjectSummary;
  assignedTo: TaskAssigneeSummary;
  createdAt: string;
}

export interface TaskRequest {
  title: string;
  description: string;
  status: TaskStatus;
  priority: TaskPriority;
  assignedToId: number;
  dueDate: string;
}

export interface TaskStatusUpdateRequest {
  status: TaskStatus;
}

/** Response-derived pagination state held by TaskService (subset of SpringPage, flattened). */
export interface PageInfo {
  totalElements: number;
  totalPages: number;
  pageNumber: number;
  pageSize: number;
  first: boolean;
  last: boolean;
}

export interface SpringSort {
  sorted: boolean;
  unsorted: boolean;
  empty: boolean;
}

export interface SpringPageable {
  pageNumber: number;
  pageSize: number;
  sort: SpringSort;
  offset: number;
  paged: boolean;
  unpaged: boolean;
}

/** Real shape of Spring Data's Page<T> JSON */
export interface SpringPage<T> {
  content: T[];
  pageable: SpringPageable;
  last: boolean;
  totalPages: number;
  totalElements: number;
  first: boolean;
  numberOfElements: number;
  size: number;
  number: number;
  sort: SpringSort;
  empty: boolean;
}
