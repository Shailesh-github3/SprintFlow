import React, { useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Calendar, Tag, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import api from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from './ui/dialog';
import { Badge } from './ui/badge';
import { Skeleton } from './ui/skeleton';
import CommentList from './CommentList';

const IssueDetailsDialog = ({ issueId, open, setOpen, project }) => {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  // Fetch Issue details
  const { data: issue, isLoading, isError } = useQuery({
    queryKey: ['issues', 'detail', issueId],
    queryFn: async () => {
      const response = await api.get(`/api/issues/${issueId}`);
      return response.data;
    },
    enabled: !!issueId && open,
  });

  const isOwner = Boolean(
    user?.email &&
    project?.projectOwner?.email &&
    user.email.toLowerCase() === project.projectOwner.email.toLowerCase()
  );

  const eligibleAssignees = useMemo(() => {
    const map = new Map();
    if (project?.projectOwner?.id) {
      map.set(project.projectOwner.id, project.projectOwner);
    }
    (project?.projectMembers || []).forEach((m) => {
      if (m?.id) {
        map.set(m.id, m);
      }
    });
    return Array.from(map.values());
  }, [project]);

  const assignMutation = useMutation({
    mutationFn: (newUserId) => api.put(`/api/issues/${issueId}/assignee/${newUserId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['issues', 'detail', issueId] });
      queryClient.invalidateQueries({ queryKey: ['issues', project?.projectId] });
      queryClient.invalidateQueries({ queryKey: ['activity', project?.projectId] });
      toast.success('Assignee updated');
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || 'Failed to update assignee');
    },
  });

  const unassignMutation = useMutation({
    mutationFn: () => api.delete(`/api/issues/${issueId}/assignee`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['issues', 'detail', issueId] });
      queryClient.invalidateQueries({ queryKey: ['issues', project?.projectId] });
      queryClient.invalidateQueries({ queryKey: ['activity', project?.projectId] });
      toast.success('Issue unassigned');
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || 'Failed to unassign issue');
    },
  });

  const isAssigning = assignMutation.isPending || unassignMutation.isPending;

  const handleAssigneeChange = (e) => {
    const val = e.target.value;
    if (!val) {
      if (issue?.assignedUser) {
        unassignMutation.mutate();
      }
    } else {
      assignMutation.mutate(Number(val));
    }
  };

  const getPriorityColor = (priority) => {
    switch (priority) {
      case 'HIGH': return 'destructive';
      case 'MEDIUM': return 'default';
      case 'LOW': return 'secondary';
      default: return 'outline';
    }
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent className="sm:max-w-[600px] max-h-[85vh] overflow-y-auto">
        {isLoading ? (
          <div className="space-y-4 py-4">
            <Skeleton className="h-6 w-1/2" />
            <Skeleton className="h-20 w-full" />
          </div>
        ) : isError || !issue ? (
          <div className="text-center text-destructive py-8">
            Failed to load issue details.
          </div>
        ) : (
          <>
            <DialogHeader>
              <div className="flex items-center gap-2 mb-2">
                <Badge variant={getPriorityColor(issue.priority)}>{issue.priority}</Badge>
                <Badge variant="outline">{issue.issueStatus?.replace('_', ' ') || 'TODO'}</Badge>
              </div>
              <DialogTitle className="text-2xl">{issue.issueTitle}</DialogTitle>
              <DialogDescription className="flex items-center gap-2 mt-2">
                <Calendar className="h-3 w-3" />
                {issue.dueDate ? new Date(issue.dueDate).toLocaleDateString() : 'No due date'}
              </DialogDescription>
            </DialogHeader>
            
            <div className="py-4 space-y-6">
              {/* Description */}
              <div>
                <h4 className="text-sm font-semibold mb-2">Description</h4>
                <p className="text-sm text-foreground whitespace-pre-wrap leading-relaxed">
                  {issue.issueDescription || 'No description provided.'}
                </p>
              </div>

              {/* Tags if any */}
              {issue.tags && issue.tags.length > 0 && (
                <div>
                  <h4 className="text-sm font-semibold mb-2">Tags</h4>
                  <div className="flex flex-wrap gap-2">
                    {issue.tags.map((tag, i) => (
                      <Badge variant="secondary" key={i}>
                        <Tag className="h-3 w-3 mr-1" /> {tag}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}

              {/* Assignee */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <h4 className="text-sm font-semibold">Assignee</h4>
                  {isAssigning && (
                    <span className="flex items-center gap-1 text-xs text-muted-foreground">
                      <Loader2 className="h-3 w-3 animate-spin" /> Saving...
                    </span>
                  )}
                </div>

                {isOwner ? (
                  <div className="flex items-center gap-2">
                    <select
                      className="flex h-9 w-full max-w-xs rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
                      value={issue.assignedUser?.id ?? ''}
                      onChange={handleAssigneeChange}
                      disabled={isAssigning}
                      aria-label="Assign issue to project member"
                    >
                      <option value="">Unassigned</option>
                      {eligibleAssignees.map((member) => (
                        <option key={member.id} value={member.id}>
                          {member.fullName} ({member.email})
                          {member.id === project?.projectOwner?.id ? ' (Owner)' : ''}
                        </option>
                      ))}
                    </select>
                  </div>
                ) : (
                  <div className="flex items-center gap-2 text-sm">
                    {issue.assignedUser ? (
                      <span className="font-medium text-foreground">
                        {issue.assignedUser.fullName} ({issue.assignedUser.email})
                      </span>
                    ) : (
                      <span className="text-muted-foreground italic">Unassigned</span>
                    )}
                  </div>
                )}
                {isOwner && (
                  <p className="text-xs text-muted-foreground">
                    As project owner, you can assign this issue to yourself or any team member.
                  </p>
                )}
              </div>

              <hr className="my-4" />

              {/* Comments Section */}
              <CommentList issueId={issueId} />
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
};

export default IssueDetailsDialog;
