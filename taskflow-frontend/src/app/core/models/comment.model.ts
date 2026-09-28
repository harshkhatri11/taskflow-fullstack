export interface CommentResponse {
  id: number;
  content: string;
  authorId: number | null;
  authorName: string;
  createdAt: string;
}

export interface CommentRequest {
  content: string;
}
