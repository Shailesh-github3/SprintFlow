import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Trash2, Send } from 'lucide-react';
import { toast } from 'sonner';
import api from '../services/api';
import { Button } from './ui/button';
import { Textarea } from './ui/textarea';
import { Avatar, AvatarFallback } from './ui/avatar';
import { Skeleton } from './ui/skeleton';
import { useAuth } from '../contexts/AuthContext';

const CommentList = ({ issueId }) => {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [newComment, setNewComment] = useState('');

  // Fetch Comments
  const { data: comments, isLoading, isError } = useQuery({
    queryKey: ['comments', issueId],
    queryFn: async () => {
      const response = await api.get(`/api/comments/issue/${issueId}`);
      return response.data;
    },
    enabled: !!issueId,
  });

  // Create Comment Mutation
  const createMutation = useMutation({
    mutationFn: (commentText) =>
      api.post('/api/comments', {
        issueId: Number(issueId),
        content: commentText,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries(['comments', issueId]);
      setNewComment('');
      toast.success('Comment added');
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to add comment');
    }
  });

  // Delete Comment Mutation
  const deleteMutation = useMutation({
    mutationFn: (commentId) => api.delete(`/api/comments/${commentId}`),
    onSuccess: () => {
      queryClient.invalidateQueries(['comments', issueId]);
      toast.success('Comment deleted');
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to delete comment');
    }
  });

  const handleAddComment = (e) => {
    e.preventDefault();
    if (!newComment.trim()) return;
    createMutation.mutate(newComment);
  };

  const handleDelete = (commentId) => {
    if (window.confirm('Delete this comment?')) {
      deleteMutation.mutate(commentId);
    }
  };

  if (isError) {
    return <div className="text-sm text-destructive mt-4">Failed to load comments.</div>;
  }

  return (
    <div className="space-y-4 mt-6">
      <h4 className="text-sm font-semibold">Comments</h4>

      {/* Comment Form */}
      <form onSubmit={handleAddComment} className="flex flex-col gap-2">
        <Textarea 
          placeholder="Add a comment..."
          value={newComment}
          onChange={(e) => setNewComment(e.target.value)}
          className="min-h-[80px] resize-none text-sm"
          disabled={createMutation.isPending}
        />
        <div className="flex justify-end">
          <Button 
            type="submit" 
            size="sm" 
            disabled={createMutation.isPending || !newComment.trim()}
            className="gap-2"
          >
            <Send className="h-3 w-3" />
            {createMutation.isPending ? 'Sending...' : 'Send'}
          </Button>
        </div>
      </form>

      {/* Comment List */}
      <div className="space-y-4 mt-4">
        {isLoading ? (
          <div className="space-y-3">
            {[1, 2].map(i => <Skeleton key={i} className="h-16 w-full" />)}
          </div>
        ) : comments?.length === 0 ? (
          <p className="text-sm text-muted-foreground italic">No comments yet.</p>
        ) : (
          comments?.map((comment) => (
            <div key={comment.commentId} className="flex gap-3 text-sm border-b pb-4 last:border-0">
              <Avatar className="h-8 w-8">
                <AvatarFallback>{comment.createdBy?.fullName?.charAt(0) || 'U'}</AvatarFallback>
              </Avatar>
              <div className="flex-1 space-y-1">
                <div className="flex items-center justify-between">
                  <div>
                    <span className="font-medium">{comment.createdBy?.fullName}</span>
                    <span className="text-xs text-muted-foreground ml-2">
                      {new Date(comment.createdAt).toLocaleString()}
                    </span>
                  </div>
                  {comment.createdBy?.email === user?.email && (
                    <Button 
                      size="icon" 
                      className="h-6 w-6 text-muted-foreground hover:text-destructive"
                      onClick={() => handleDelete(comment.commentId)}
                    >
                      <Trash2 className="h-3 w-3" />
                    </Button>
                  )}
                </div>
                <p className="text-foreground whitespace-pre-wrap">{comment.commentText}</p>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default CommentList;
