import { ProjectStatus } from './enums';
import { UserSummaryResponse } from './user.model';

export interface ProjectResponse {
  id: number;
  title: string;
  description: string;
  status: ProjectStatus;
  manager: UserSummaryResponse;
  createdAt: string;
}

export interface ProjectRequest {
  title: string;
  description: string;
  status: ProjectStatus;
  managerId: number | null;
}
