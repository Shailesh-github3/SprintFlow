import React, { useMemo, useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Calendar, Tag, Loader2, Edit3, X, Save, Clock, User as UserIcon } from 'lucide-react';
import { toast } from 'sonner';
import api from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from './ui/dialog';
import { Badge } from './ui/badge';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Textarea } from './ui/textarea';
import { Skeleton } from './ui/skeleton';
import CommentList from './CommentList';

const IssueDetailsDialog = ({ issueId, open, setOpen, project }) => {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [isEditing, setIsEditing] = useState(false);

  // Edit form state
  const [editTitle, setEditTitle] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editPriority, setEditPriority] = useState('MEDIUM');
  const [editDueDate, setEditDueDate] = useState('');

  // Fetch Issue details
  const { data: issue, isLoading, isError } = useQuery({
    queryKey: ['issues', 'detail', issueId],
    queryFn: async () => {
      const response = await api.get(`/api/issues/${issueId}`);
      return response.data;
    },
    enabled: !!issueId && open,
  });

  useEffect(() => {
    if (issue) {
      setEditTitle(issue.issueTitle || '');
      setEditDescription(issue.issueDescription || '');
      setEditPriority(issue.priority || 'MEDIUM');
      setEditDueDate(issue.dueDate ? issue.dueDate.substring(0, 10) : '');
      setIsEditing(false);
    }
  }, [issue]);

  const isOwner = Boolean(
    user?.email &&
    project?.projectOwner?.email &&
    user.email.toLowerCase() === project.projectOwner.email.toLowerCase()
  );

  const isCreator = Boolean(
    user?.email &&
    issue?.createdBy?.email &&
    user.email.toLowerCase() === issue.createdBy.email.toLowerCase()
  );

  const canEdit = isOwner || isCreator;

  const eligibleAssignees = useMemo(() => {
    const map = new Map();
    if (project?.projectOwner?.userId || project?.projectOwner?.id) {
      const id = project.projectOwner.userId || project.projectOwner.id;
      map.set(id, project.projectOwner);
    }
    (project?.projectMembers || []).forEach((m) => {
      const id = m?.userId || m?.id;
      if (id) {
        map.set(id, m);
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

  const updateMutation = useMutation({
    mutationFn: (updateData) => api.put(`/api/issues/${issueId}`, updateData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['issues', 'detail', issueId] });
      queryClient.invalidateQueries({ queryKey: ['issues', project?.projectId] });
      queryClient.invalidateQueries({ queryKey: ['activity', project?.projectId] });
      setIsEditing(false);
      toast.success('Issue updated successfully');
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || 'Failed to update issue');
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

  const handleSaveEdit = (e) => {
    e.preventDefault();
    if (!editTitle.trim()) {
      toast.error('Issue title is required');
      return;
    }
    if (!editDueDate) {
      toast.error('Due date is required');
      return;
    }
    updateMutation.mutate({
      title: editTitle.trim(),
      description: editDescription.trim(),
      priority: editPriority,
      dueDate: editDueDate,
      status: issue?.issueStatus || 'TODO',
    });
  };

  const getPriorityColor = (priority) => {
    switch (priority) {
      case 'HIGH': return 'destructive';
      case 'MEDIUM': return 'default';
      case 'LOW': return 'secondary';
      default: return 'outline';
    }
  };

  const assignedMemberId = issue?.assignedUser?.userId || issue?.assignedUser?.id;

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent className="sm:max-w-[650px] max-h-[85vh] overflow-y-auto">
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
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <Badge variant={getPriorityColor(issue.priority)}>{issue.priority}</Badge>
                  <Badge variant="outline">{issue.issueStatus?.replace('_', ' ') || 'TODO'}</Badge>
                </div>
                {canEdit && !isEditing && (
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-8 gap-1.5 text-xs text-muted-foreground hover:text-foreground"
                    onClick={() => setIsEditing(true)}
                  >
                    <Edit3 className="h-3.5 w-3.5" />
                    Edit
                  </Button>
                )}
              </div>

              {!isEditing ? (
                <>
                  <DialogTitle className="text-2xl leading-snug">{issue.issueTitle}</DialogTitle>
                  <DialogDescription className="flex items-center gap-2 mt-1">
                    <Calendar className="h-3.5 w-3.5" />
                    Due: {issue.dueDate ? new Date(issue.dueDate).toLocaleDateString() : 'No due date'}
                  </DialogDescription>
                </>
              ) : (
                <form onSubmit={handleSaveEdit} className="space-y-3 pt-2">
                  <div>
                    <label className="text-xs font-semibold text-muted-foreground">Title</label>
                    <Input
                      value={editTitle}
                      onChange={(e) => setEditTitle(e.target.value)}
                      placeholder="Issue title"
                      className="mt-1"
                      required
                    />
                  </div>
                  <div>
                    <label className="text-xs font-semibold text-muted-foreground">Description</label>
                    <Textarea
                      value={editDescription}
                      onChange={(e) => setEditDescription(e.target.value)}
                      placeholder="Issue description"
                      className="mt-1 min-h-[80px]"
                      required
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="text-xs font-semibold text-muted-foreground">Priority</label>
                      <select
                        className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring mt-1"
                        value={editPriority}
                        onChange={(e) => setEditPriority(e.target.value)}
                      >
                        <option value="LOW">Low</option>
                        <option value="MEDIUM">Medium</option>
                        <option value="HIGH">High</option>
                      </select>
                    </div>
                    <div>
                      <label className="text-xs font-semibold text-muted-foreground">Due Date</label>
                      <Input
                        type="date"
                        value={editDueDate}
                        onChange={(e) => setEditDueDate(e.target.value)}
                        className="mt-1"
                        required
                      />
                    </div>
                  </div>
                  <div className="flex justify-end gap-2 pt-2">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => setIsEditing(false)}
                      disabled={updateMutation.isPending}
                    >
                      <X className="h-3.5 w-3.5 mr-1" /> Cancel
                    </Button>
                    <Button
                      type="submit"
                      size="sm"
                      disabled={updateMutation.isPending}
                    >
                      {updateMutation.isPending ? (
                        <Loader2 className="h-3.5 w-3.5 animate-spin mr-1" />
                      ) : (
                        <Save className="h-3.5 w-3.5 mr-1" />
                      )}
                      Save Changes
                    </Button>
                  </div>
                </form>
              )}
            </DialogHeader>

            <div className="py-4 space-y-6">
              {/* Description (read-only view) */}
              {!isEditing && (
                <div>
                  <h4 className="text-sm font-semibold mb-2">Description</h4>
                  <p className="text-sm text-foreground whitespace-pre-wrap leading-relaxed bg-muted/20 p-3 rounded-md border">
                    {issue.issueDescription || 'No description provided.'}
                  </p>
                </div>
              )}

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
                      value={assignedMemberId ?? ''}
                      onChange={handleAssigneeChange}
                      disabled={isAssigning}
                      aria-label="Assign issue to project member"
                    >
                      <option value="">Unassigned</option>
                      {eligibleAssignees.map((member) => {
                        const mId = member.userId || member.id;
                        const ownerId = project?.projectOwner?.userId || project?.projectOwner?.id;
                        return (
                          <option key={mId} value={mId}>
                            {member.fullName} ({member.email})
                            {mId === ownerId ? ' (Owner)' : ''}
                          </option>
                        );
                      })}
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

              {/* Metadata: Created by, created at, updated at */}
              <div className="bg-muted/30 p-3 rounded-lg border text-xs text-muted-foreground space-y-1.5">
                <div className="flex items-center gap-1.5">
                  <UserIcon className="h-3.5 w-3.5 text-muted-foreground/80" />
                  <span>
                    Created by: <strong className="text-foreground">{issue.createdBy?.fullName || 'System'}</strong> {issue.createdBy?.email ? `(${issue.createdBy.email})` : ''}
                  </span>
                </div>
                <div className="flex items-center gap-1.5">
                  <Clock className="h-3.5 w-3.5 text-muted-foreground/80" />
                  <span>
                    Created: {issue.createdAt ? new Date(issue.createdAt).toLocaleString() : 'N/A'}
                  </span>
                  {issue.updatedAt && (
                    <span className="ml-3">
                      • Updated: {new Date(issue.updatedAt).toLocaleString()}
                    </span>
                  )}
                </div>
              </div>

              <hr className="my-4" />

              {/* Comments Section */}
              <CommentList issueId={issueId} isProjectOwner={isOwner} />
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
};

export default IssueDetailsDialog;
